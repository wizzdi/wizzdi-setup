package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";

    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException {

        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine mainCmd = parser.parse(options, args, true); //will not fail if fed with plugins options.

        Logger logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));
        File pluginRoot = new File(mainCmd.getOptionValue(INSTALLATION_TASKS_FOLDER, "tasks"));
        logger.info("Will load tasks from " + pluginRoot.getAbsolutePath());
        PluginManager pluginManager = new DefaultPluginManager(pluginRoot.toPath());

        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        Map<String, IInstallationTask> installationTasks = pluginManager.getExtensions(IInstallationTask.class).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        Map<String, TaskWrapper> tasks = new HashMap<>();
        for (IInstallationTask task : installationTasks.values()) {
            options.addOptionGroup(getOptionsGroup(task));
//            TaskWrapper wrapper = new TaskWrapper(task, getOptions(task));
//            tasks.put(task.getId(), wrapper);
        }
        if (mainCmd.hasOption(HELP) ){
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("", options);
            return;


        }
        Parameters parameters = new Parameters();


        InstallationContext installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters());
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);
        int successes = 0;
        int failures = 0;
        while (topologicalOrderIterator.hasNext()) {
            String installationTaskUniqueId = topologicalOrderIterator.next();
            IInstallationTask installationTask = installationTasks.get(installationTaskUniqueId);
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
            logger.info("Have completed successfully  " + successes + " installation tasks , )" + failures + " installation tasks have failed");
        }

        // stop and unload all plugins
        pluginManager.stopPlugins();
    }

    private static OptionGroup getOptionsGroup(IInstallationTask task) {
        OptionGroup options = new OptionGroup();
        Parameters parameters = task.getParameters();
        for (Parameter parameter : parameters.getValues()) {
            Option option=new Option(parameter.getName(), parameter.isHasValue(), parameter.getDescription());
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
            fh = new FileHandler(folder.isEmpty() ? name + "%u.log" : folder + "/" + name + "%u.log");
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
}
