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
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.flexicore.installer.utilities.LoggerUtilities.initLogger;
import static java.lang.System.*;


public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";
    private static Logger logger;
    private static PluginManager pluginManager;
    private static List<IUIComponent> uiComponents;
    private static InstallationContext installationContext = null;
    static boolean uiFoundandRun = false;
    static String startupStatus = "";
    static Map<String, Set<String>> missingDependencies = new HashMap<>();


    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException, InterruptedException {

        System.out.println(System.getProperty("user.dir"));

        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        String[] trueArgs = getTrueArgs(args, options);

        CommandLine mainCmd = parser.parse(options, trueArgs, false); //will not fail if fed with plugins options.

        logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));
        installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters()).
                        setUiQuit(Start::uiComponentQuit).
                        setUiPause(Start::uiComponentPause).
                        setUiResume(Start::uiComponentResume).
                        setUiInstall(Start::uiComponentInstall).
                        setUiShowLogs(Start::uiComponentShowLogs).
                        setUiAbout(Start::UIAccessAbout).
                        setUiStopInstall(Start::UIAccessInterfaceStop).
                        setInstallerProgress(Start::InstallerProgress).
                        setUpdateService(Start::uiUpdateService);


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
        Object o = pluginManager.getExtensions(IInstallationTask.class);
        Map<String, IInstallationTask> installationTasks = pluginManager.getExtensions(IInstallationTask.class).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));


        // handle parameters and command line options here. do it at the dependency order.
        int order = 1;
        for (IInstallationTask task:installationTasks.values()) {
            if (task.isSnooper()) {
                task.setOrder(order++);
                installationContext.addTask(task);
            }
        }
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);
        readProperties(installationContext);
        installationContext.getiInstallationTasks().clear();

        while (topologicalOrderIterator.hasNext()) {
            String installationTaskUniqueId = topologicalOrderIterator.next();
            IInstallationTask task = installationTasks.get(installationTaskUniqueId);
            boolean found = false;
            OperatingSystem cos = task.getCurrentOperatingSystem();
            for (OperatingSystem os : task.getOperatingSystems()) {

                if (os.equals(cos)) {
                    found = true;
                    break;
                }
            }
            if (!found) continue;
            task.setOrder(order++);
            installationContext.addTask(task);

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
        if (installationContext.getParamaters() != null) installationContext.getParamaters().sort();
        boolean stopped = false;
        loadUiComponents(); //in blocking operation, it is never returned.
        if (!uiFoundandRun) {
            if (installationContext.getParamaters().getBooleanValue("install")) {
                install(installationContext);
            } else {
                info(" there is no 'install' parameter set to true that will start installation");
            }
        } else {

            while (!stopped) {
                Thread.sleep(1000);
                if (!startupStatus.equals("")) {
                    informUI(startupStatus);
                    startupStatus = "";

                }
            }
        }

        // stop and unload all plugins
        pluginManager.stopPlugins();
        cleanSnoopers();
        exit(0);

    }

    private static void cleanSnoopers() {
        for (Thread thread : snoopers) {
            info("stopping thread: " + thread.getName());
            if (thread.isAlive()) {
                thread.interrupt();
                while (thread.isAlive()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        logger.severe("Error while waiting for thread to stop");
                    }
                }
            }
        }
    }

    private static void loadUiComponents() {
        uiComponents = pluginManager.getExtensions(IUIComponent.class);
        for (IUIComponent component : uiComponents) {
            component.setContext(installationContext);
            installationContext.addUIComponent(component);
            if (component.isAutoStart()) {
                info("Have started  a UI plugin");
                if (component.startAsynch(installationContext)) {
                    uiFoundandRun = true; //will not run the installation from command line.
                }
            }
        }
    }

    /**
     * will not update ui componenets without isShowing=true
     *
     * @param task
     * @param installationContext
     */
    private static void doUpdateUI(IInstallationTask task, InstallationContext installationContext) {
        List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());
        for (IUIComponent component : filtered) {
            component.updateProgress(installationContext, task);
        }

    }

    /**
     * Update service status on UI
     * @param installationContext
     * @param service
     * @param task
     */
    private static boolean doUpdateService(InstallationContext installationContext,Service service,IInstallationTask task) {
        List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());
        for (IUIComponent component : filtered) {
            component.updateService(installationContext,service, task);
        }
        return filtered.size()!=0;
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

    /**
     * Adjust parameters to reflect values in this order of priority (from highest to lowest)
     * 1 external UI if exists (not in this function)
     * 2 command line parameters
     * 3 properties file (should be in the current folder
     * 4 code based default values.
     *
     * @param task
     * @param installationContext
     * @param taskOptions
     * @param args
     * @param parser
     * @return
     */
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
                    parameter.setSource(cmd.hasOption(name) ? ParameterSource.COMMANDLINE : parameter.getSource());
                } else {
                    // if a parameter has no value it means that it's existence changes the parameter value to true, unless the properties file changes it or
                    // overridden from the command line

                    if (!cmd.hasOption(name)) {
                        if (!getNewParameterFromProperties(parameter, installationContext)) {
                            parameter.setValue(String.valueOf(cmd.hasOption(name))); // will set the value of the parameter requiring no value to false as properties file hasn't changed it
                        } else parameter.setSource(ParameterSource.PROPERTIES_FILE);
                    } else {
                        parameter.setSource(ParameterSource.COMMANDLINE);
                        parameter.setValue(String.valueOf(cmd.hasOption(name))); // this is for non value switches indicated in command line
                    }

                }
                installationContext.getParamaters().addParameter(parameter, task);
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
    private static boolean getNewParameterFromProperties(Parameter parameter, InstallationContext installationContext) {
        String result = installationContext.getProperties().getProperty(parameter.getName());
        if (result != null) {
            parameter.setValue(result);
            return true; //was changed.
        }
        return false;
    }

    /**
     * get default value for parameter, if it has one it will be overridden by a command line option if provided.
     *
     * @param parameter
     * @param installationContext
     * @return
     */
    private static String getCalculatedDefaultValue(Parameter parameter, InstallationContext installationContext) {

        String result = installationContext.getProperties().getProperty(parameter.getName());
        if (result == null) {
            result = parameter.getDefaultValue();

        } else {
            if (parameter.getType().equals(ParameterType.LIST)) {
                String[] split = result.split(",");
                if (split != null) {
                    parameter.getListOptions().clear();
                    parameter.getListOptions().addAll(Arrays.asList(split));
                    parameter.getListOptions().remove(split[0]);
                    Collections.sort(parameter.getListOptions());
                    parameter.getListOptions().add(0, split[0]);

                }
            }
            parameter.setSource(ParameterSource.PROPERTIES_FILE);
            info("Parameter " + parameter.getName() + " default value will be taken from a properties file");
        }
        if (result.contains("&")) {
            parameter.setNonTranslatedValue(result);
            parameter.setSandSymbolPresent(true);
            result = Parameter.getReplaced(installationContext, result, parameter);
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
            //todo:handle missing dependencies differently when running from UI
            severe("Missing dependencies: " + s);

            StringBuilder builder = new StringBuilder();
            builder.append("missing dependencies: ");
            missingDependencies.forEach((s1, strings) -> {
                int i = 1;
                for (String k : strings) {
                    if (i++ != 1) {
                        builder.append(",");
                    }
                    builder.append(k);
                }
            });

            startupStatus = builder.toString();

        }
        return new TopologicalOrderIterator<>(g);
    }

    private static void informUI(String message) {
        for (IUIComponent component : uiComponents) {
            component.updateStatus(installationContext, missingDependencies, message);
        }
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

    private static boolean installRunning = false;
    private static ArrayList<Thread> snoopers = new ArrayList<>();

    private static boolean install(InstallationContext context) {
        if (!installRunning) {
            installRunning = true;

            Thread thread = new Thread(() -> {
                long startAll = System.currentTimeMillis();
                logger.info("Performing installation of " + context.getiInstallationTasks().size() + " tasks");
                for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {
                    info("will now install " + installationTask.getName() + " id: " + installationTask.getId());
                    info("details: " + installationTask.getDescription());

                    installationTask.setProgress(0).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now());
                    try {
                        long start = System.currentTimeMillis();
                        installationTask.setContext(context);
                        if (!installationTask.isSnooper()) {
                            InstallationResult result = installationTask.install(context);
                            if (result.getInstallationStatus().equals(InstallationStatus.COMPLETED)) {
                                info("Have successfully finished installation task: " + installationTask.getName() + " after " + getSeconds(start) + " Seconds");
                                installationTask.setProgress(100).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.COMPLETED);
                            } else {
                                installationTask.setProgress(0).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.FAILED);
                            }
                        } else {
                            Thread snooper = new Thread(() -> {
                                try {
                                    InstallationResult result = installationTask.install(context);
                                } catch (Throwable throwable) {
                                    severe("Exception while running a snooper",throwable);
                                }
                            });
                            snooper.setName(installationTask.getId());
                            snoopers.add(snooper);
                            snooper.start();


                        }
                    } catch (Throwable throwable) {
                        severe("Exception while installing: " + installationTask.getName(), throwable);
                        installationTask.setProgress(0).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.FAILED);


                    }
                    if (context.getConsumer() != null) {
                        context.getConsumer().updateProgress(installationTask, context);
                    }
                    // installTask(installationTask, context);
                }
                info("Total installation time was: " + getSeconds(startAll));
                for (IInstallationTask installationTask : context.getCleanupTasks().values()) {
                    installationTask.setProgress(70).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now());
                    //  installTask(installationTask, context);
                }

            });
            thread.start();
            installRunning = false;
        } else {
            info("Install already running");
            return true;
        }
        return false;
    }

    public static long getSeconds(long from) {
        return (System.currentTimeMillis() - from) / 1000;
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
        cleanSnoopers();
        exit(0);
        return true;
    }

    private static boolean uiComponentInstall(IUIComponent component, InstallationContext context) {
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

    private static IInstallationTask InstallerProgress(IInstallationTask task, InstallationContext context) {
        doUpdateUI(task, context);
        return task;
    }
    private static boolean uiUpdateService(InstallationContext context,Service service,IInstallationTask task) {
        return doUpdateService(context,service,task);
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

    @FunctionalInterface
    public static interface InstallerProgress {
        IInstallationTask installationProgress(IInstallationTask task, InstallationContext context);
    }
    @FunctionalInterface
    public static interface UpdateService {
        boolean serviceProgress(InstallationContext context, Service service, IInstallationTask task);
    }

}


