package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import com.flexicore.installer.tests.*;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;


public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";
    private static Logger logger;

    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException {

        System.out.println(System.getProperty("user.dir"));

        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        String[] trueArgs = getTrueArgs(args, options);

        CommandLine mainCmd = parser.parse(options, trueArgs, false); //will not fail if fed with plugins options.

        logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));


        InstallationContext installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters());
        File pluginRoot = new File(mainCmd.getOptionValue(INSTALLATION_TASKS_FOLDER, "tasks"));
        logger.info("Will load tasks from " + pluginRoot.getAbsolutePath());
        PluginManager pluginManager = new DefaultPluginManager(pluginRoot.toPath());

        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        Map<String, IInstallationTask> installationTasks = pluginManager.getExtensions(IInstallationTask.class).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));


        IInstallationTask iInstallationTask = new CommonParameters();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new WildflyInstall();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new WildflyParameters();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new EPX2000IInstall();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new FlexicoreInstall();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new FlexiCoreParameters();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new ItamarInstall();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);

        iInstallationTask = new ItamarParameters();
        installationTasks.put(iInstallationTask.getId(), iInstallationTask);




        Map<String, TaskWrapper> tasks = new HashMap<>();


        // handle parameters and command line options here. do it at the dependency order.
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);
        ArrayList<IInstallationTask> orderedTasks=new ArrayList<>();
        while (topologicalOrderIterator.hasNext()) {
            String installationTaskUniqueId = topologicalOrderIterator.next();
            IInstallationTask task = installationTasks.get(installationTaskUniqueId);
            orderedTasks.add(task);
            Options taskOptions = getOptions(task, installationContext);
            if (!updateParameters(task, installationContext, taskOptions, args, parser)) {
                severe("Error while parsing task parameters, quitting on task: " + task.getId());
                return; // quit here
            }

            if (mainCmd.hasOption(HELP)) {
                if (taskOptions.getOptions().size() != 0) {

                    System.out.println("command line options for: " + task.getId() + "  " + task.getInstallerDescription());
                    if (task.getPrerequisitesTask().size() != 0) {
                        System.out.println("Requires: " + task.getPrerequisitesTask());
                    }

                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("Start.bat ", taskOptions);

                    System.out.println("Task default parameters:");
                   for (Parameter p :task.getParameters(installationContext).getValues()) {
                       System.out.println(p);
                   }

                }
            }
        }

        if (mainCmd.hasOption(HELP)) {
            if (options.getOptions().size() != 0) {
                System.out.println("command line options for installer");

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Start.bat ", options);
            }
            return;

        }


        //reset


        int successes = 0;
        int failures = 0;

   for (IInstallationTask installationTask:orderedTasks) {

            logger.info("Starting " + installationTask.getId());
            InstallationResult installationResult = installationTask.install(installationContext);
            if (installationResult.getInstallationStatus().equals(InstallationStatus.COMPLETED)) {
                successes++;
            } else {
                failures++;
            }

            logger.info("Completed " + installationTask.getId() + " with " + installationResult);
        }
        if (failures == 0) {
            logger.info("Have completed successfully  " + successes + " installation tasks");
        } else {
            logger.info("Have completed successfully  " + successes + " installation tasks , " + failures + " installation tasks have failed");
        }

        // stop and unload all plugins
        pluginManager.stopPlugins();
    }

    /**
     * adjust args to options, otherwise Apache library get confused.
     *
     * @param args
     * @param options
     * @return
     */
    private static String[] getTrueArgs(String[] args, Options options) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (options.hasOption(args[i])) {
                data.add(args[i]);
                if (i < args.length - 1) {
                    if (!args[i + 1].startsWith("-")) {
                        data.add(args[i + 1]);
                        i++;
                    }
                }
            }
        }
        String[] result = new String[data.size()];
        result = data.toArray(result);
        return result;
    }

    private static boolean updateParameters(IInstallationTask task, InstallationContext installationContext, Options taskOptions, String[] args, CommandLineParser parser) {
        try {
            String[] trueArgs = getTrueArgs(args, taskOptions);
            CommandLine cmd = parser.parse(taskOptions, trueArgs, true);
            Parameters taskParameters = task.getParameters(installationContext);
            int count = 0;
            for (String name : taskParameters.getKeys()) {
                Parameter parameter = taskParameters.getParameter(name);
                if (parameter.isHasValue()) {
                    parameter.setValue(cmd.getOptionValue(name, getCalculatedDefaultValue(parameter, installationContext))); //set correct
                } else {
                    boolean test = cmd.hasOption(name);
                    parameter.setValue(String.valueOf(cmd.hasOption(name)));
                }
                installationContext.getParamaters().addParameter(parameter);
                count++;
            }
            info("Have added " + count + " parameters to installation task: " + task.getId() + " ->>" + task.getInstallerDescription());
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "error while parsing command line", e);
            return false;
        }
        return true;
    }

    private static String getCalculatedDefaultValue(Parameter parameter, InstallationContext installationContext) {
        String result = parameter.getDefaultValue();
        if (result != null) {
            int a = result.indexOf("&");
            if (a > -1) {
                int index = a + 2;
                String temp = null;
                int i = 1;
                while (index < result.length()) {
                    if (!result.substring(a + i++, index++).matches("[a-zA-Z0-9]+")) break;

                }

                String toReplace = result.substring(a, index - 2);
                String newString = installationContext.getParamaters().getValue(toReplace.substring(1));
                if (newString != null) {
                    result = result.replace(result.substring(a, index - 2), newString);
                }
            }
        }
        return result;
    }

    private static Options getOptions(IInstallationTask task, InstallationContext installationContext) {
        Options options = new Options();
        Parameters parameters = task.getParameters(installationContext);
        for (Parameter parameter : parameters.getValues()) {

            Option option = new Option(parameter.getName(), parameter.isHasValue(), parameter.getDescription());
            options.addOption(option);
        }
        return options;
    }


    private static TopologicalOrderIterator<String, DefaultEdge> getInstallationTaskIterator(Map<String, IInstallationTask> installationTasks) throws MissingInstallationTaskDependency {
        Map<String, Set<String>> missingDependencies = new HashMap<>();
        Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (IInstallationTask installationTask : installationTasks.values()) {
            String uniqueId = installationTask.getId();
            g.addVertex(uniqueId);
            for (String req : installationTask.getPrerequisitesTask()) {
                if (installationTasks.containsKey(req)) {
                    g.addVertex(req);
                    g.addEdge(req, uniqueId);
                } else {
                    missingDependencies.computeIfAbsent(uniqueId, f -> new HashSet<>()).add(req);
                }
            }
        }
        if (!missingDependencies.isEmpty()) {
            String s = "Missing Dependencies:" + System.lineSeparator() + missingDependencies.entrySet().parallelStream().map(f -> f.getKey() + " : " + f.getValue()).collect(Collectors.joining(System.lineSeparator()));
            throw new MissingInstallationTaskDependency(s);
        }
        return new TopologicalOrderIterator<>(g);
    }

    private static Options initOptions() {

        // create Options object
        return new Options()
                .addOption(INSTALLATION_TASKS_FOLDER, true, "(optional - default currentDir/tasks) folder for installation tasks")
                .addOption(LOG_PATH_OPT, true, "log folder")

                .addOption(HELP, false, "shows this message");

    }

    public static Logger initLogger(String name, String folder) {

        Logger logger = Logger.getLogger(name);
        FileHandler
                fh;
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            handler.close();
            logger.removeHandler(handler);
        }

        try {

            // String time = LocalDateTime.now().toString().replace(":", "-");
            File file;
            if (!(file = new File(folder)).exists()) {
                Files.createDirectories(file.toPath());
            }
            fh = new FileHandler(folder.isEmpty() ? name + ".log" : folder + "/" + name + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);


        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logger;

    }

    public static void info(String message) {
        if (logger != null) logger.info(message);
    }

    public static void error(String message, Throwable e) {
        if (logger != null) logger.log(Level.SEVERE, message, e);
    }

    public static void severe(String message, Throwable e) {

        error(message, e);
    }

    public static void severe(String message) {
        if (logger != null) logger.log(Level.SEVERE, message);
    }
}
