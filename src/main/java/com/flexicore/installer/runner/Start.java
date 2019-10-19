package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.interfaces.IUIComponent;

import com.flexicore.installer.localtasksfortests.*;
import com.flexicore.installer.model.*;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import static com.flexicore.installer.utilities.LoggerUtilities.initLogger;
import static java.lang.System.exit;


public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";
    private static Logger logger;
    private static PluginManager pluginManager;


    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException, InterruptedException {

        System.out.println(System.getProperty("user.dir"));

        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        String[] trueArgs = getTrueArgs(args, options);

        CommandLine mainCmd = parser.parse(options, trueArgs, false); //will not fail if fed with plugins options.

        logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));
      InstallationContext installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters()).
                        setUiQuit(Start::uiComponentQuit).
                        setUiPause(Start::uiComponentPause).
                        setUiResume(Start::uiComponentResume).
                        setUiInstall(Start::uiComponentInstall).
                        setUiShowLogs(Start::uiComponentShowLogs).
                        setUiAbout(Start::UIAccessAbout).
                        setUiStopInstall(Start::UIAccessInterfaceStop);

        File pluginRoot = new File(mainCmd.getOptionValue(INSTALLATION_TASKS_FOLDER, "tasks"));
        String path = pluginRoot.getAbsolutePath();
        if (!pluginRoot.exists()) {
            severe("cannot fine path for tasks at" + pluginRoot.getAbsolutePath() + " !!, exiting");
            exit(0);
        }
        logger.info("Will load tasks from " + pluginRoot.getAbsolutePath() + "  exists: " + pluginRoot.exists());
        pluginManager = new DefaultPluginManager(pluginRoot.toPath());

        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        Map<String, IInstallationTask> DebuginstallationTasks = pluginManager.getExtensions(IInstallationTask.class).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));


        new FlexicoreUniquenessEnforcer(DebuginstallationTasks);
        new EPX2000Install(DebuginstallationTasks);
        new ShekelComponentsInstall(DebuginstallationTasks);
        new ShekelComponentsParameters(DebuginstallationTasks);

        //********** standard ***************
        new CommonParameters(DebuginstallationTasks);


        //*********** Flexicore installation (home)
        new FlexiCoreParameters(DebuginstallationTasks);

        new FlexicoreInstall(DebuginstallationTasks);

        new FlexicoreFixConfigFile(DebuginstallationTasks);


        //***************** wildfly installation
        new WildflyParameters(DebuginstallationTasks);

        new WildflyInstall(DebuginstallationTasks);

        //************ flexicore deployment
        new FlexicoreDeploymentInstall(DebuginstallationTasks);

        //*********************shekel installation
        new ShekelComponentsParameters(DebuginstallationTasks);
        new ShekelComponentsInstall(DebuginstallationTasks);
        //********************Itamar Installation
        new ItamarParameters(DebuginstallationTasks);
        new ItamarInstall(DebuginstallationTasks);


        // handle parameters and command line options here. do it at the dependency order.
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(DebuginstallationTasks);
        readProperties(installationContext);
        installationContext.getiInstallationTasks().clear();
        while (topologicalOrderIterator.hasNext()) {
            String installationTaskUniqueId = topologicalOrderIterator.next();
            IInstallationTask task = DebuginstallationTasks.get(installationTaskUniqueId);
            if (task.cleanup()) {
                installationContext.getCleanupTasks().add(task);
            } else {
                installationContext.getCleanupTasks().add(task);
            }

            Options taskOptions = getOptions(task, installationContext);
            if (!updateParameters(task, installationContext, taskOptions, args, parser)) {
                severe("Error while parsing task parameters, quitting on task: " + task.getId());
                return; // quit here
            }

            if (mainCmd.hasOption(HELP)) {
                if (taskOptions.getOptions().size() != 0) {

                    System.out.println("command line options for: " + task.getId() + "  " + task.getDescription());
                    if (task.getPrerequisitesTask().size() != 0) {
                        System.out.println("Requires: " + task.getPrerequisitesTask());
                    }

                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("Start.bat ", taskOptions);

                    System.out.println("Task default parameters:");
                    for (Parameter p : task.getParameters(installationContext).getValues()) {
                        System.out.println(p);
                    }

                }
            }
        }

        boolean stopped = false;
        boolean uiFoundandRun = false;
        List<IUIComponent> uiComponents = pluginManager.getExtensions(IUIComponent.class);
        for (IUIComponent component : uiComponents) {
            component.setContext(installationContext);
            installationContext.addUIComponent(component);
            if (component.isAutoStart()) {
                info("Have started  a UI plugin");
                if (component.start()) {
                    uiFoundandRun = true; //will not run the installation from command line.
                }
            }
        }
        if (uiFoundandRun) {
            while (!stopped) {
                Thread.sleep(1000);
            }


            // stop and unload all plugins
            pluginManager.stopPlugins();
            exit(0);
        }
    }

    /**
     * Look for properties file if exists override code based default properties
     *
     * @param installationContext
     */
    private static void readProperties(InstallationContext installationContext) {
        String currentFolder = System.getProperty("user.dir");
        info("Will look for  properties file in: " + currentFolder);
        try (InputStream input = new FileInputStream(currentFolder + "/" + "properties")) {
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            installationContext.setProperties(prop);
        } catch (IOException ex) {
            info("No properties file provided, one can be placed in the current folder: " + System.getProperty("user.dir"));
            installationContext.setProperties(new Properties());
        }
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
                    parameter.setValue(cmd.getOptionValue(name, getCalculatedDefaultValue(parameter, installationContext))); //set correct value for parameter
                } else {
                    // if a parameter has no value it means that it's existence changes the parameter value to true, unless the properties file changes it or
                    // overridden from the command line
                    if (!cmd.hasOption(name)) {
                        getNewParameterFromProperties(parameter, installationContext);
                    }
                    parameter.setValue(String.valueOf(cmd.hasOption(name)));
                }
                installationContext.getParamaters().addParameter(parameter, null);
                count++;
            }
            info("Have added " + count + " parameters to installation task: " + task.getId() + " ->>" + task.getDescription());
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "error while parsing command line", e);
            return false;
        }
        return true;
    }

    /**
     * @param parameter
     * @param installationContext
     * @return
     */
    private static Parameter getNewParameterFromProperties(Parameter parameter, InstallationContext installationContext) {
        return parameter;
    }

    /**
     * get default value for parameter, if it has one it will be overridden by a command line option if provided.
     *
     * @param parameter
     * @param installationContext
     * @return
     */
    private static String getCalculatedDefaultValue(Parameter parameter, InstallationContext installationContext) {
        System.out.println(parameter);
        String result = installationContext.getProperties().getProperty(parameter.getName());
        if (result == null) {
            result = parameter.getDefaultValue();
        } else {
            info("Parameter " + parameter.getName() + " default value will be taken from a properties file");
        }

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


    private static boolean doStop() {
        logger.info("Performing installation stop");
        return true;
    }

    private static boolean doPause() {
        logger.info("Performing installation pause");
        return true;
    }

    private static boolean doResume() {
        logger.info("Performing installation resume");
        return true;
    }

    private static boolean doInstallDry() {
        logger.info("Performing install dry");
        return true;
    }

    private static boolean install(InstallationContext context) {
        logger.info("Performing installation ");
//        for (IInstallationTask installationTask : context.getiInstallationTasks()) {
//            installTask(installationTask, context);
//        }
//        for (IInstallationTask installationTask : context.getCleanupTasks()) {
//
//            installTask(installationTask, context);
//        }
        return true;
    }

    private static boolean installTask(IInstallationTask installationTask, InstallationContext context) {
        logger.info("Starting: " + installationTask.getId() + ", name is: " + installationTask.getName());
        InstallationResult installationResult = null;
        try {
            installationResult = installationTask.install(context);
        } catch (Throwable throwable) {
            severe("Have failed to install: " + installationTask.getId(), throwable);
        }
        context.addResult(installationTask, installationResult);
        logger.info("Completed " + installationTask.getId() + " with " + installationResult);
        if (installationResult.getInstallationStatus().equals(InstallationStatus.COMPLETED)) {

            context.incSuccess();
            return true;
        }
        context.incFailures();
        return false;
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

    private static boolean uiComponentQuit(IUIComponent uiComponent, InstallationContext context) {
        pluginManager.stopPlugins();
        exit(0);
        return true;
    }

    private static boolean uiComponentInstall(IUIComponent uiComponent, InstallationContext context) {
        return install(context);
    }

    private static boolean uiComponentInstallDry(IUIComponent uiComponent, InstallationContext context) {
        return doInstallDry();
    }

    private static boolean uiComponentPause(IUIComponent uiComponent, InstallationContext context) {
        return doPause();
    }
    private static boolean UIAccessInterfaceStop(IUIComponent uiComponent, InstallationContext context) {
        return doStop();
    }


    private static boolean uiComponentResume(IUIComponent uiComponent, InstallationContext context) {
        return doResume();
    }

    private static String uiComponentShowLogs(IUIComponent uiComponent, InstallationContext context) {
        return doshowLogs();
    }

    private static String UIAccessAbout(IUIComponent uiComponent, InstallationContext context) {
        return doAbout();
    }

    private static String doAbout() {
        logger.info("Performing about");
        return "";
    }

    private static String doshowLogs() {
        logger.info("Performing show logs");
        return "";
    }


    @FunctionalInterface
    public static interface UIAccessInterfaceQuit {
        boolean uiComponentClosed(IUIComponent uiComponent, InstallationContext context);

    }


    @FunctionalInterface
    public static interface UIAccessInterfaceInstall {
        boolean uiComponentInstall(IUIComponent uiComponent, InstallationContext context);

    }

    @FunctionalInterface
    public static interface UIAccessInterfaceInstallDry {
        boolean uiComponentInstallDry(IUIComponent uiComponent, InstallationContext context);
    }
    @FunctionalInterface
    public static interface UIAccessInterfaceStop {
        boolean uiComponentStop(IUIComponent uiComponent, InstallationContext context);
    }
    @FunctionalInterface
    public static interface UIAccessInterfacePause {
        boolean uiComponentPause(IUIComponent uiComponent, InstallationContext context);
    }

    @FunctionalInterface
    public static interface UIAccessInterfaceResume {
        boolean uiComponentResume(IUIComponent uiComponent, InstallationContext context);
    }

    @FunctionalInterface
    public static interface UIAccessInterfaceShowLogs {
        String uiComponentShowLogs(IUIComponent uiComponent, InstallationContext context);
    }

    @FunctionalInterface
    public static interface UIAccessAbout {
        String uiComponentAbout(IUIComponent uiComponent, InstallationContext context);
    }

}


