package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.interfaces.IUIComponent;
import com.flexicore.installer.model.*;
import jpowershell.OSDetector;
import jpowershell.PowerShell;

import jpowershell.PowerShellResponse;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.flexicore.installer.utilities.LoggerUtilities.initLogger;
import static java.lang.System.*;
import static jpowershell.OSDetector.*;


public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";
    private static final long PROGRESS_DELAY = 300;
    private static final String CONFIG_FILENAME = "jpowershell.properties";
    private static Logger logger;
    private static PluginManager pluginManager;
    private static List<IUIComponent> uiComponents;
    private static InstallationContext installationContext = null;
    static boolean uiFoundandRun = false;
    static String currentStatus = "";

    static Map<String, Set<String>> missingDependencies = new HashMap<>();
    private static String oldUpdate="false";
    private static Parameter updateParameter=null;


    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException, InterruptedException {

        //testFunctionalExample();
     //   PowerShellResponse result = InstallationTask.executePowerShellCommand("get-service", 10000, 10);
        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        String[] trueArgs = getTrueArgs(args, options);

        CommandLine mainCmd = parser.parse(options, trueArgs, false); //will not fail if fed with plugins options.

        logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));
        installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters()).
                        setOperatingSystem(InstallationTask.isWindows ?
                                OperatingSystem.Windows : (InstallationTask.isLinux ? OperatingSystem.Linux : OperatingSystem.OSX)).
                        setUiQuit(Start::uiComponentQuit).
                        setUiPause(Start::uiComponentPause).
                        setUiResume(Start::uiComponentResume).
                        setUiInstall(Start::uiComponentInstall).
                        setUiUninstall(Start::uiComponentUnInstall).
                        setUiUpdate(Start::uiComponentUpdate).
                        setUiShowLogs(Start::uiComponentShowLogs).
                        setUiAbout(Start::UIAccessAbout).
                        setUiStopInstall(Start::UIAccessInterfaceStop).
                        setInstallerProgress(Start::InstallerProgress).
                        setUpdateService(Start::uiUpdateService).
                        setFilesProgress(Start::updateFilesProgress).
                        setUiToggle(Start::uiComponentToggle).setUpdateSingleComponent(Start::doUpdateComponent);


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
        //installationTasks.values().forEach(iInstallationTask -> iInstallationTask.initialize(installationContext));

        // handle parameters and command line options here. do it at the dependency order.

        for (IInstallationTask task : installationTasks.values()) {
            if (task.isSnooper()) {

                installationContext.addTask(task);
            }
        }
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);
        readProperties(installationContext);
        installationContext.getiInstallationTasks().clear();
        ArrayList<String> softRequired = new ArrayList<>();
        ArrayList<IInstallationTask> finalizers = new ArrayList<>();
        ArrayList<IInstallationTask> softNeed = new ArrayList<>();

        while (topologicalOrderIterator.hasNext()) {
            String installationTaskUniqueId = topologicalOrderIterator.next();
            IInstallationTask task = installationTasks.get(installationTaskUniqueId);
            if (task.isFinalizerOnly()) {
                finalizers.add(task);
                continue;
            }
            if (!task.getSoftPrerequisitesTask().isEmpty()) { //task has soft prerequisites.
                //check if required task has been added already.
                boolean defer = false;
                for (String req : task.getSoftPrerequisitesTask()) {
                    if (!installationContext.getiInstallationTasks().containsKey(req)) {
                        defer = true;
                        if (!softRequired.contains(req)) {
                            softRequired.add(req);
                        }
                    }
                }
                if (defer) {
                    softNeed.add(task);
                    continue; //we will not add this task now
                }
            }
            ArrayList<IInstallationTask> needThis = new ArrayList<>();
            for (IInstallationTask needingTask : softNeed) {
                if (needingTask.getSoftPrerequisitesTask().contains(installationTaskUniqueId)) {
                    needThis.add(needingTask); //these tasks will be added after this task
                }
            }

            if (!handleTask(installationContext, task, mainCmd, args, parser)) {
                exit(0);
            }

            for (IInstallationTask needingTask : needThis) {
                if (needingTask.getSoftPrerequisitesTask().size() == 1) { //task will be added only if all soft have been added so far

                    if (!handleTask(installationContext, needingTask, mainCmd, args, parser)) {
                        exit(0);
                    }
                } else {
                    needingTask.getSoftPrerequisitesTask().remove(installationTaskUniqueId);
                }
            }

        }
        for (IInstallationTask task : softNeed) {
            if (!installationContext.getiInstallationTasks().containsKey(task.getId())) {
                handleTask(installationContext, task, mainCmd, args, parser);
            }
        }
        for (IInstallationTask task : finalizers) {

            handleTask(installationContext, task, mainCmd, args, parser);
        }
        //do pass two, brute force second pass on all parameters we cannot fix references unless we have them all.
        for (IInstallationTask task : installationContext.getiInstallationTasks().values()) {
            Options taskOptions = getOptions(task, installationContext);
            updateParameters(false, task, installationContext, taskOptions, args, parser);
        }
        if (installationContext.getParamaters() != null) installationContext.getParamaters().sort();
        installationContext.getiInstallationTasks().values().forEach(iInstallationTask -> iInstallationTask.initialize(installationContext));
        int order = 1;
        for (IInstallationTask task : installationContext.getiInstallationTasks().values()) task.setOrder(order++);
        loadUiComponents();  //currently asynchronous
        if (!uiFoundandRun) {
            checkHelp(mainCmd);
            if (installationContext.getParamaters().getBooleanValue("install")) {
                install(installationContext, false);
            } else {
                info(" there is no 'install' parameter set to true that will start installation");
            }
        } else {

        }
        boolean displayed = false;
        while (!stopped) {

            Thread.sleep(300);
            if (!displayed) {
                displayed = true;
                informUI("Installation ready", InstallationState.READY);
            }

        }
        // stop and unload all plugins
        pluginManager.stopPlugins();
        cleanSnoopers();
        exit(0);

    }


    private static void checkHelp(CommandLine mainCmd) {
        if (mainCmd.hasOption(HELP)) {
            exit(0);
        }
    }

    static boolean stopped = false;

    /**
     * adds a task if matches the current operating system.
     *
     * @param installationContext
     * @param task
     * @param mainCmd
     * @param args
     * @param parser
     * @return
     */
    private static boolean handleTask(InstallationContext installationContext,
                                      IInstallationTask task,
                                      CommandLine mainCmd, String[] args, CommandLineParser parser) {
        boolean found = false;
        task.setContext(installationContext);
        OperatingSystem cos = task.getCurrentOperatingSystem();
        for (OperatingSystem os : task.getOperatingSystems()) {

            if (os.equals(cos)) {
                found = true;
                break;
            }
        }
        ((InstallationTask) task).setWrongOS(!found);

        installationContext.addTask(task);

        Options taskOptions = getOptions(task, installationContext);
        if (!updateParameters(true, task, installationContext, taskOptions, args, parser)) {
            severe("Error while parsing task parameters, quitting on task: " + task.getId());
            return false;
        }

        if (mainCmd.hasOption(HELP)) {
            if (taskOptions.getOptions().size() != 0) {
                InstallationTask installationTask = (InstallationTask) task;


                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(installationTask.isWindows() ? "Start.bat " : "/.Start", taskOptions);
            }
        }
        return true;
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

    private static IInstallationTask updateFilesProgress(InstallationContext context, IInstallationTask task) {
        List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());

        for (IUIComponent component : filtered) {
            component.refreshFilesCount(installationContext, task);
        }
        return task;
    }

    /**
     * will not update ui componenets without isShowing=true
     *
     * @param task
     * @param installationContext
     */
    private static void doUpdateUI(IInstallationTask task, InstallationContext installationContext) {
        List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());
        boolean someFound = false;
        for (IUIComponent component : filtered) {
            component.updateProgress(installationContext, task);
            someFound = true;
        }
        if (someFound) {
            try {
                Thread.sleep(PROGRESS_DELAY);
            } catch (InterruptedException e) {
                severe("Interrupted");
            }
        }
    }

    /**
     * support for one component to change another component value
     *
     * @param installationContext
     * @param task
     * @param parameter
     * @return
     */
    private static boolean doUpdateComponent(InstallationContext installationContext, IInstallationTask task, Parameter parameter) {
        if (uiComponents != null) {
            List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());

            for (IUIComponent component : filtered) {
                component.updateWidget(installationContext, task, parameter);
            }
        }
        return true;
    }

    /**
     * Update service status on UI
     *
     * @param installationContext
     * @param service
     * @param task
     */
    private static boolean doUpdateService(InstallationContext installationContext, Service service, IInstallationTask task) {
        List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());
        for (IUIComponent component : filtered) {
            component.updateService(installationContext, service, task);
        }
        return filtered.size() != 0;
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
    private static boolean updateParameters(boolean reallyAdd, IInstallationTask task, InstallationContext installationContext, Options taskOptions, String[] args, CommandLineParser parser) {
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
                if (reallyAdd) {
                    installationContext.getParamaters().addParameter(parameter, task);
                    count++;
                }
                //special case here

                if (name.equals("extralogs")) {
                    installationContext.setExtraLogs(Boolean.valueOf(cmd.getOptionValue("extralogs")));
                }
                if (name.equals(HELP)) {
                    installationContext.setHelpRunning(true);
                }
            }
            if (installationContext.isExtraLogs())
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
            if (installationContext.isExtraLogs())
                info("Parameter " + parameter.getName() + " default value will be taken from a properties file");
        }
        if (result.contains("&")) {
            parameter.setNonTranslatedValue(result);
            parameter.setSandSymbolPresent(true);
            result = Parameter.getReplaced(installationContext, result, parameter, null);
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
        HashMap<String, String> allReq = new HashMap<>();
        for (IInstallationTask installationTask : installationTasks.values()) {
            if (!installationTask.isFinalizerOnly()) {
                String uniqueId = installationTask.getId();
                allReq.put(uniqueId, uniqueId);
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
        }
        for (IInstallationTask installationTask : installationTasks.values()) {
            if (installationTask.isFinalizerOnly()) {
                String id;
                g.addVertex(id = installationTask.getId());
                for (String req : allReq.values()) {
                    g.addVertex(req);
                    g.addEdge(req, id);
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

            currentStatus = builder.toString();

        }
        return new TopologicalOrderIterator<>(g);
    }

    private static void informUI(String message, InstallationState state) {
        for (IUIComponent component : uiComponents) {
            component.updateStatus(installationContext, missingDependencies, message, state);
        }
        currentStatus = message;
    }

    /**
     * return true if some UI is there, so we should wait for confirmation
     *
     * @param context
     * @param inspections
     * @return
     */
    private static boolean informUIOnInspections(InstallationContext context, ArrayList<InspectionResult> inspections) {

        for (IUIComponent component : uiComponents) {
            component.handleInspections(context, inspections);
        }
        return uiComponents.size() != 0;
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

    private static boolean toggle(InstallationContext context) {
        for (IInstallationTask task : context.getiInstallationTasks().values()) {
            task.setEnabled(!task.isEnabled());
        }
        return true;
    }

    private static boolean update(InstallationContext context) {
        boolean result = false;
         updateParameter = context.getParameter("update");
        if (updateParameter != null) {
             oldUpdate = updateParameter.getValue();
            updateParameter.setValue("true");
            result = install(context, false);

        }

        return result;
    }
//    public static void testFunctionalExample() {
//        System.out.println("testFunctionalExample");
//        if (OSDetector.isWindows()) {
//            PowerShell.openSession()
//                    .executeCommandAndChain("Get-Process", (res -> System.out.println("List Processes:" + res.getCommandOutput())))
//                    .executeCommandAndChain("Get-WmiObject Win32_BIOS", (res -> System.out.println("BIOS information:" + res.getCommandOutput())))
//                    .close();
//        }
//    }
    private static boolean Uninstall(InstallationContext context) {
        return install(context, true);
    }

    private static boolean install(InstallationContext context, boolean unInstall) {

        if (!installRunning) {
            installRunning = true;

            Thread thread = new Thread(() -> {
                long startAll = System.currentTimeMillis();
                logger.info("Performing installation of " + context.getiInstallationTasks().size() + " tasks");
                informUI("Installation running , performed 0, failed 0, skipped 0", InstallationState.RUNNING);
                int failed = 0;
                int skipped = 0;
                int completed = 0;
                handleInspections(context);
                HashMap<String, String> restarters = new HashMap<>();
                String mainStatusMessage = unInstall ? "Uninstallation status" : "installation status";
                for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {
                    if (!installationTask.isEnabled() && !installationTask.isWrongOS()) {
                        info("task " + installationTask.getName() + " is disabled, skipping");
                        skipped++;
                        updateStatus(mainStatusMessage, completed, failed, skipped, InstallationState.RUNNING);
                        continue;

                    }
                    info("will now install " + installationTask.getName() + " id: " + installationTask.getId());
                    info("details: " + installationTask.getDescription());

                    installationTask.setProgress(0).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now()).setMessage(" Started installation, may take some time");
                    doUpdateUI(installationTask, installationContext);
                    try {
                        long start = System.currentTimeMillis();
                        if (!installationTask.isSnooper()) {
                            if (!installationTask.getNeedRestartTasks().isEmpty()) {
                                for (String service : installationTask.getNeedRestartTasks()) {
                                    restarters.put(service, service);
                                }
                            }
                            InstallationResult result = unInstall ? installationTask.unInstall(context) : installationTask.install(context);
                            if (result.getInstallationStatus().equals(InstallationStatus.COMPLETED)) {
                                completed++;
                                updateStatus(" installation running: ", completed, failed, skipped, InstallationState.RUNNING);
                                info("Have successfully finished installation task: " + installationTask.getName() + " after " + getSeconds(start) + " Seconds");
                                installationTask.setProgress(100).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.COMPLETED);

                            } else {
                                info("-----------Failed task: " + installationTask.getName());
                                failed++;
                                updateStatus(mainStatusMessage, completed, failed, skipped, InstallationState.RUNNING);
                                installationTask.setProgress(0).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.FAILED);
                                info("Have unsuccessfully finished installation task: " + installationTask.getName() + " after " + getSeconds(start) + " Seconds");

                            }
                            doUpdateUI(installationTask, installationContext);
                        } else {
                            Thread snooper = new Thread(() -> {
                                try {
                                    InstallationResult result = unInstall ? installationTask.unInstall(context) : installationTask.install(context);
                                } catch (Throwable throwable) {
                                    severe("Exception while running a snooper", throwable);
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


                updateStatus(mainStatusMessage + " finalizing", completed, failed, skipped, InstallationState.FINALIZING);
                info(" calling finalizers on all tasks");
                if (!unInstall) {
                    for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {
                        if (!installationTask.isSnooper()) {
                            try {
                                if (installationTask.isEnabled()) {
                                    if (installationTask.finalizeInstallation(context).getInstallationStatus().equals(InstallationStatus.COMPLETED)) {
                                        completed++;
                                        updateStatus("finalizing  status", completed, failed, skipped, InstallationState.FINALIZING);
                                    } else {
                                        failed++;
                                        info("-----------Failed task while finalizing: " + installationTask.getName());
                                        updateStatus("finalizing  status", completed, failed, skipped, InstallationState.FINALIZING);
                                    }
                                }
                            } catch (Throwable throwable) {
                                severe("Error while finalizing task: " + installationTask.getName());
                            }
                        }
                    }
                }
                info("Total installation time was: " + getSeconds(startAll) + " Seconds");
                updateStatus("starting cleanup ", completed, failed, skipped, InstallationState.CLEANUP);
                for (IInstallationTask installationTask : context.getCleanupTasks().values()) {
                    installationTask.setProgress(70).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now());
                    try {
                        if (installationTask.install(installationContext).equals(InstallationStatus.COMPLETED)) {

                        } else {

                        }
                        doUpdateUI(installationTask, installationContext);
                    } catch (Throwable throwable) {
                        severe("Error while cleanup task run " + installationTask.getName());
                    }
                }
                //ugly, we need to have one Installation task for the operation of starting required services.
                if (!unInstall) {
                    if (context.getiInstallationTasks().size() != 0) {
                        InstallationTask task = (InstallationTask) context.getiInstallationTasks().values().toArray()[0]; //use first task, as the function
                        for (String service : restarters.values()) {

                            if (task.setServiceToStart(service, "runner finalizing")) {
                                info("Have started service: " + service);
                            } else {
                                severe("Have failed to start service: " + service);
                            }
                        }
                    }
                }
                if (failed == 0) {
                    updateStatus("done, no task has failed ", completed, failed, skipped, InstallationState.COMPLETE);
                } else {
                    updateStatus("done, some tasks failed", completed, failed, skipped, InstallationState.PARTLYCOMPLETED);
                }
               if (updateParameter!=null) updateParameter.setValue(oldUpdate);
                if (!uiFoundandRun) stopped = true; //command line will execute here
            });
            thread.start();
            installRunning = false;
        } else {
            info("Install already running");
            return true;
        }
        return false;
    }

    /**
     * should allow UI notification on issues found before installation , such as free space memory , already installed
     * components that need removal etc.
     * todo: implement using wait on object or timeout.
     *
     * @param context
     */
    private static void handleInspections(InstallationContext context) {
//        ArrayList<InspectionResult> inspections=new ArrayList<>();
//        for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {
//            InspectionResult result = installationTask.inspect(installationContext);
//            if (!result.getInspectionState().equals(InspectionState.NOT_FOUND)) {
//                inspections.add(result);
//            }
//        }
//        if (inspections.size()!=0) {
//            if(informUIOnInspections(installationContext,inspections)) {
//                boolean notreplied=false;
//                while (!notreplied) {
//                    try {
//                        Thread.sleep(100);
//                        if (not)
//                    } catch (InterruptedException e) {
//                        info ("Interrupted while waiting for UI to respons");
//                    }
//                }
//            }
//        }

    }

    private static void updateStatus(String message, int completed, int failed, int skipped, InstallationState state) {
        informUI(message + " Completed tasks: " + completed + " Failed tasks: " + failed + " Skipped tasks: " + skipped, state);
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
        updateParameter=null;
        return install(context, false);
    }

    private static boolean uiComponentUnInstall(IUIComponent component, InstallationContext context) {
        updateParameter=null;
        return Uninstall(context);
    }


    private static boolean uiComponentUpdate(IUIComponent component, InstallationContext context) {
        return update(context);
    }


    private static boolean uiComponentToggle(IUIComponent component, InstallationContext context) {
        return toggle(context);
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

    private static boolean uiUpdateService(InstallationContext context, Service service, IInstallationTask task) {
        return doUpdateService(context, service, task);
    }

    private static IInstallationTask InstallerFilesProgress(InstallationContext context, IInstallationTask task) {
        updateFilesProgress(context, task);
        return task;
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
    public static interface UIAccessInterfaceUnInstall {
        boolean uiComponentUnInstall(IUIComponent uiComponent, InstallationContext context);

    }

    @FunctionalInterface
    public static interface UIAccessInterfaceUpdate {
        boolean uiComponentUpdate(IUIComponent uiComponent, InstallationContext context);

    }

    @FunctionalInterface
    public static interface UIAccessInterfaceToggle {
        boolean uiComponentToggle(IUIComponent uiComponent, InstallationContext context);

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
    public static interface installerFilesProgress {
        IInstallationTask filesProgress(InstallationContext context, IInstallationTask task);
    }

    @FunctionalInterface
    public static interface UpdateService {
        boolean serviceProgress(InstallationContext context, Service service, IInstallationTask task);
    }

    @FunctionalInterface
    public static interface UpdateSingleComponent {
        boolean updateComponent(InstallationContext context, IInstallationTask task, Parameter parameter);
    }
}


