package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.interfaces.IUIComponent;
import com.flexicore.installer.model.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.fusesource.jansi.AnsiConsole;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.flexicore.installer.model.InstallationTask.*;
import static com.flexicore.installer.utilities.LoggerUtilities.initLogger;
import static java.lang.System.exit;
import static org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.ansi;


public class Start {

    private static final String HELP = "h";
    private static final String PROPERTIES = "p";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";
    private static final String DEMO_POWERSHELL = "ps";
    private static final long PROGRESS_DELAY = 300;

    private static String currentFolder = System.getProperty("user.dir");
    private static Logger logger;
    private static List<String> versions = new ArrayList<>();
    private static PluginManager pluginManager;
    private static List<IUIComponent> uiComponents;
    private static InstallationContext installationContext = null;

    static boolean uiFoundandRun = false;
    static String currentStatus = "";

    static Map<String, Set<String>> missingDependencies = new HashMap<>();
    private static String oldUpdate = "false";
    private static Parameter updateParameter = null;
    static ProcessorData processorData;
    static SystemData systemData = new SystemData();

    static boolean errorInUiComponents = false;
    private static String propertiesFile;
    private static boolean tested;

    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException, InterruptedException {

        Options options = initOptions();
        CommandLineParser parser = new DefaultParser();
        String[] trueArgs = getTrueArgs(args, options);


        CommandLine mainCmd = parser.parse(options, trueArgs, false); //will not fail if fed with plugins options.

        logger = initLogger("Installer", mainCmd.getOptionValue(LOG_PATH_OPT, "logs"));


        if (isWindows()) {

            Thread startThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    systemData.setStart(System.currentTimeMillis());
                    info("************* starting data gathering ");


                    processorData = getProcessorData();
                    systemData.setProcessorData(processorData);
                    int logical = getNumberOfLogicalProcessor();
                    if (logical > 0) {
                        processorData.setLogicalCores(logical);
                    } else {
                        info("****************************** no logical processors received");
                    }

                    systemData.setPhysicalMemory(getPhysicalMemory());
                    systemData.setFreeDiskSpace(getFreeDiskSpace("C:"));
                    systemData.setWindowsVersion(getWindowsVersion());
                    systemData.setDotNetVersions(InstallationTask.getDotNetVersions(logger, ""));

                    info("+-+-+-+-+ .NET  installed: " + dotNetVersion);
                    info("************* ended  data gathering in: " + (systemData.setTotal(System.currentTimeMillis() - systemData.getStart()).getTotal() / 1000 + " Seconds"));

                    info("System data: " + systemData);
                    systemData.setDone(true);
                    if (errorInUiComponents) {
                        ahowErrorandExit();
                    }
                }

            });
            startThread.start();
        }
        installationContext = new InstallationContext()
                .setLogger(logger).setParameters(new Parameters()).
                        setOperatingSystem(InstallationTask.isWindows ?
                                OperatingSystem.Windows : (InstallationTask.isLinux ? OperatingSystem.Linux : OperatingSystem.OSX)).
                        setUiQuit(Start::uiComponentQuit).
                        setUiPause(Start::uiComponentPause).
                        setUiResume(Start::uiComponentResume).
                        setUiInstall(Start::uiComponentInstall).
                        setUiInstallDry(Start::uiComponentInstallDry).
                        setUiUninstall(Start::uiComponentUnInstall).
                        setUiUpdate(Start::uiComponentUpdate).
                        setUiShowLogs(Start::uiComponentShowLogs).
                        setUiAbout(Start::UIAccessAbout).
                        setUiStopInstall(Start::UIAccessInterfaceStop).
                        setInstallerProgress(Start::InstallerProgress).
                        setUpdateService(Start::uiUpdateService).
                        setFilesProgress(Start::updateFilesProgress).
                        setUiToggle(Start::uiComponentToggle).setUpdateSingleComponent(Start::doUpdateComponent).
                        setUiAskUSer(Start::getUserResponse).
                        setShowSystemData(Start::uiComponentShowSystemData).
                        setShowPagedData(Start::uiShowPagedList);
        if (isLinux) {
            getCPUData();

        }
        File pluginRoot = new File(mainCmd.getOptionValue(INSTALLATION_TASKS_FOLDER, "tasks"));
        String path = pluginRoot.getAbsolutePath();
        if (!pluginRoot.exists()) {
            severe("cannot find path for tasks at" + pluginRoot.getAbsolutePath() + " !!, exiting");
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
            versions.add(task.getVersion());

        }
        for (IInstallationTask task : installationTasks.values()) {
            if (task.isSnooper()) {

                installationContext.addTask(task);
            }
        }

        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);
        if (!mainCmd.hasOption(PROPERTIES)) {
            propertiesFile = currentFolder + "/properties";
        } else {
            propertiesFile = mainCmd.getOptionValue(PROPERTIES, currentFolder + "/properties");
        }
        readProperties(installationContext, propertiesFile);
        installationContext.getiInstallationTasks().clear();
        ArrayList<String> tasksNeededAssoft = new ArrayList<>();
        ArrayList<IInstallationTask> finalizers = new ArrayList<>();
        ArrayList<IInstallationTask> taskNeedingSoft = new ArrayList<>();

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
                for (String requiredSoft : task.getSoftPrerequisitesTask()) {
                    if (installationTasks.containsKey(requiredSoft)) {
                        int a=installationContext.getiInstallationTasks().size();
                        if (installationContext.getTask(requiredSoft)==null){
                            defer = true;
                            if (!tasksNeededAssoft.contains(requiredSoft)) {
                                tasksNeededAssoft.add(requiredSoft);
                            }
                        }
                    }
                }
                if (defer) {
                    taskNeedingSoft.add(task);
                    continue; //we will not add this task now
                }
            }
            ArrayList<IInstallationTask> needThis = new ArrayList<>(); //build a list of tasks soft need this task.
            for (IInstallationTask needingTask : taskNeedingSoft) {
                if (needingTask.getSoftPrerequisitesTask().contains(installationTaskUniqueId)) {
                    needThis.add(needingTask); //these tasks will be added after this task
                }
            }

            if (!handleTask(installationContext, task, mainCmd, args, parser)) {
                exit(0);
            }
            if (tasksNeededAssoft.contains(task.getId())) {
                for (IInstallationTask task1 : taskNeedingSoft) {
                    if (task1.getSoftPrerequisitesTask().contains(task.getId())) {
                        task1.getSoftPrerequisitesTask().remove(task.getId());
                        if (task1.getSoftPrerequisitesTask().size() == 0) { //adding task only if found
                            if (!handleTask(installationContext, task1, mainCmd, args, parser)) {
                                exit(0);
                            }
                            taskNeedingSoft.remove(task1);
                        }
                    }

                }
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

        } //while ends here


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
        if (!verifyDependecies()) {
            severe  ("++++++++++++++++ cannot continue, dependencies are not properly set ++++++++++++++++");
            exit(0);
        }
        Collections.sort(versions);
        String displayTasks = installationContext.getParamaters().getValue("display");
        displayTasks(displayTasks);
        loadUiComponents();  //currently asynchronous

        if (!uiFoundandRun) {
            checkHelp(mainCmd);
            if (installationContext.getiInstallationTasks().size() == 0) {
                info(" no installation tasks found in tasks folder, cannot proceed, quitting");
                exit(0);
            }
            Parameter psave = installationContext.getParameter("psave");
            String savePath;
            File file;
            if (psave != null && !(savePath = psave.getValue()).isEmpty()) {
                //save all paramters in a properties file
                try {
                    file = new File(savePath);
                    saveProperties(installationContext, file);
                    info("Have saved a properties file at: " + savePath + "\n will now exit");
                    exit(0);
                } catch (Exception e) {
                    severe("Error while saving properties file to: " + savePath, e);
                    exit(0);
                }

            }

            if (installationContext.getParamaters().getBooleanValue("install")) {
                install(installationContext, false, false);
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
                informUI("Installation ready", InstallationState.READY, progress);
            }
            if (!tested) {
                UserAction ua = getUIAction();
                tested = true;
                getUserResponse(installationContext, ua);
            }

        }
        // stop and unload all plugins
        pluginManager.stopPlugins();
        cleanSnoopers();
        exit(0);

    }

    private static boolean  verifyDependecies() {

        boolean result = true;
        for (IInstallationTask task: installationContext.getiInstallationTasks().values()) {
            Set<String> depemdOnTasks = task.getPrerequisitesTask();
            for (String ptask:depemdOnTasks) {
                IInstallationTask dependon=installationContext.getiInstallationTasks().get(ptask);
                if (dependon!=null) {
                    if (dependon.getOrder() >= task.getOrder()) {
                        result = false;
                        severe("------hard, order of tasks is wrong " + task.getId() + " will be performed ahead of depend on task: " + dependon);
                    }
                }else {
                    info ("+_+_+_+_+_+_+_+_+_ Warning! task "+ptask+" which is requird by "+task.getId()+" is not present");

                }
            }
            depemdOnTasks = task.getSoftPrerequisitesTask();
            if (depemdOnTasks!=null) {
                for (String ptask : depemdOnTasks) {
                    IInstallationTask dependon = installationContext.getiInstallationTasks().get(ptask);
                    if (dependon!=null) {
                        if (dependon.getOrder() >= task.getOrder()) {
                            result = false;
                            severe("------soft order of tasks is wrong " + task.getId() + " will be performed ahead of depend on task: " + dependon);
                        }
                    }
                }
            }

        }
        return result;
    }

    private static void displayTasks(String argument) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n******************** installation tasks included *******************\n");
        int i = 1;
        for (IInstallationTask task : installationContext.getiInstallationTasks().values()) {

            if (argument.contains("details")) {
                sb.append("\n------------\n" + i++ + "  " + task.getId() + "\n");
                List<Parameter> parameters = installationContext.getParamaters().byTask(task);
                if (parameters != null) {
                    if (parameters.size() > 0) {
                        sb.append("   parameters:\n");
                    }
                    for (Parameter parameter : parameters) {
                        sb.append("\n    name: " + parameter.getName() + "\n");
                        sb.append("              " + parameter.getDescription().replace("\n", " ") + "\n");
                        sb.append("     Type: " + parameter.getType() + "\n");
                        sb.append("     Value: " + parameter.getValue() + "\n");
                        sb.append("     Value Source: " + parameter.getSource() + "\n");
                        if (parameter.getType().equals(ParameterType.LIST)) {
                            sb.append("     available options:\n");
                            for (String option : parameter.getListOptions()) {
                                sb.append("        " + option + "\n");

                            }
                        }
                    }
                }
            } else {
                sb.append(i++ + " " + task.getId() + "\n");
            }
        }

        sb.append("\n***************************\n");
        info(sb.toString());
        if (argument.contains("quit")) {
            exit(0);
        }
    }


    /**
     * for tests only
     *
     * @return
     */
    private static UserAction getUIAction() {
        UserAction ua = new UserAction();
        ua.addMessage(new UserMessage().setMessage("Enter existing database administrator username").
                setInputType(UserMessage.InputType.string).
                setCreateText(false).
                setLeftMargin(50).
                setSide(UserMessage.Side.left)
                .setInputWidth(200));
        ua.addMessage(new UserMessage().setMessage("Enter existing database administrator username********").
                setInputType(UserMessage.InputType.string).
                setCreateText(false).
                setLeftMargin(50).
                setSide(UserMessage.Side.left)
                .setInputWidth(200));
        ua.addMessage(new UserMessage().setPrompt("postgres").
                setInputType(UserMessage.InputType.string).
                setCreateText(true).
                setLeftMargin(50).
                setSide(UserMessage.Side.left)
                .setInputWidth(200));
        ua.addMessage(new UserMessage().setMessage("password").
                setInputType(UserMessage.InputType.password).
                setCreateText(true).

                setLeftMargin(50).
                setSide(UserMessage.Side.left)
                .setInputWidth(200));
        ua.setPossibleAnswers(new UserResponse[]{UserResponse.IGNORE, UserResponse.CONTINUE});
        return ua;
    }

    private static void getCPUData() {
        String p[] = {"lscpu"};
        InstallationTask task = new InstallationTask();
        task.setContext(installationContext);
        try {
            InstallationTask.ProcessResult result = task.executeCommandByBuilder(p);
            if (result.isResult()) {
                info("have received cpu information");
                HashMap<String, String> cpuData = new HashMap<>();
                for (String line : result.getLines()) {
                    String[] split = line.split(":");
                    if (split.length > 0) {
                        cpuData.put(split[0], split[1].trim());
                    }
                }

                ProcessorType processorType = getLinuxProcessorType(cpuData);
                if (processorType != null) {
                    InstallationTask.setProcessorType(processorType);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Error while getting cpu data:\n");
                for (String line : result.getErrorLines()) {
                    sb.append(line + "\n");
                }
                info(sb.toString());
            }
        } catch (IOException e) {
            severe("Error while executing lscpu on a linux system", e);
        }
    }


    static int logicalCores = -1;

    public static int getNumberOfLogicalProcessor() {
        if (logicalCores != -1) return logicalCores;
        PowerShellReturn response = executePowerShellCommand("(Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors", null);
        try {
            if (response.isTimeout() || response.isError()) {

                return 2;
            }
            String[] split = response.getCommandOutput().split("\n");
            logicalCores = Integer.parseInt(split[0]);

        } catch (NumberFormatException e) {

            return 2;
        }
        return logicalCores;


    }

    private static String getValueByKey(PowerShellReturn response, String key) {
        if (response.isError() || response.isTimeout()) return "";

        String[] result = response.getCommandOutput().split("\n");
        for (String line : result) {
            if (line.contains(key)) {
                return line.split(":")[1];
            }
        }
        return "";
    }


    static double gb = 1024 * 1024 * 1024;

    public static double getFreeDiskSpace(String driveLetter) {

        if (isWindows()) {
            if (driveLetter == null || driveLetter.isEmpty()) driveLetter = "C:";
            PowerShellReturn response = executePowerShellCommand("Get-PSDrive C | Select-Object Used,Free", null);

            try {
                if (!response.isError()) {
                    for (String s : response.getOutput()) {
                        String[] split = s.split(" ");
                        if (split.length == 2) {
                            if (isNumeric(split[1])) {
                                return Double.parseDouble(split[1]) / gb;
                            }
                        }
                    }
                }

            } catch (NumberFormatException e) {

            }
        }
        return -1;
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static double getPhysicalMemory() {
        if (isWindows()) {
            PowerShellReturn response = executePowerShellCommand(" Get-WmiObject -Class Win32_ComputerSystem", null);

            try {
                return Double.parseDouble(getValueByKey(response, "TotalPhysicalMemory")) / gb;
            } catch (NumberFormatException e) {

            }
        }
        return -1;
    }

    static ProcessorData cacheProcessorData = null;

    public static ProcessorData getProcessorData() {
        if (isWindows()) {
            if (cacheProcessorData != null) return cacheProcessorData;
            PowerShellReturn response = executePowerShellCommand(
                    "Get-CimInstance -ClassName 'Win32_Processor'   | Select-Object -Property 'DeviceID', 'Name', 'NumberOfCores'", null);
            ProcessorData processorData = new ProcessorData();

            try {
                if (response.isTimeout() || response.isError()) {
                    if (response.isTimeout()) severe("Time out in getting processor data");
                    if (response.isError()) severe("error in getting processor data");
                    return processorData;
                }

                String[] lines = response.getCommandOutput().split("\n");

                // info("Get-CimInstance -ClassName 'Win32_Processor'  ");
                String theLine = lines[3];

                boolean found = false;
                for (String line : lines) {
                    if (line.toLowerCase().startsWith("cpu")) {
                        theLine = line;
                        found = true;
                    }

                }
                if (theLine.toLowerCase().startsWith("cpu")) {
                    String[] split = theLine.split("\\s+");
                    int i = 0;
                    int at = -1;
                    for (String s : split) {
                        if (s.equals("@")) at = i;
                        i++;
                    }
                    StringBuilder b = new StringBuilder();
                    for (int j = 1; j < at; j++) {
                        b.append(split[j]);
                        if (j + 1 != at) b.append(" ");
                    }
                    processorData.setName(b.toString());
                    try {
                        processorData.setProcessorFrequency(Double.parseDouble(split[at + 1].replaceAll("[^\\d.]", "")));
                    } catch (NumberFormatException e) {
                        severe("Issue while parsing processor frequency");
                    }
                    try {
                        processorData.setNumberOfCores(Integer.parseInt(split[at + 2]));
                    } catch (NumberFormatException e) {
                        severe("Issue while parsing processor number of true cores");
                    }
                    try {
                        processorData.setLogicalCores(getNumberOfLogicalProcessor());
                    } catch (Exception e) {
                        severe("Issue while parsing get number of logical processors");
                    }
                    processorData.setProcessorType(getPorcessorType());
                } else {
                    severe("Could not parse processor data ");
                }

            } catch (NumberFormatException e) {
                severe("General Error while parsing processor data ", e);
            }
            cacheProcessorData = processorData;
            return processorData;
        } else {
            return null;
        }
    }

    private static ProcessorType getLinuxProcessorType(HashMap<String, String> raw) {
        ProcessorType result = new ProcessorType();
        result.setArchitecture(raw.get("Architecture"));
        result.setBits64(result.getArchitecture().contains("64"));
        result.setManufacturer(raw.get("Vendor ID"));
        result.setMaxClockSpeed(raw.get("CPU max MHz"));
        result.setModel(Integer.parseInt(raw.get("Model")));
        result.setName(raw.get("Model name"));
        return result;
    }

    static ProcessorType cacheProcessorType = null;

    public static ProcessorType getPorcessorType() {
        if (cacheProcessorType != null) return cacheProcessorType;
        ProcessorType processorType = new ProcessorType();
        PowerShellReturn response = executePowerShellCommand("Get-WmiObject Win32_Processor", null);
        if (response.isTimeout() || response.isError()) {
            return processorType;
        }

        processorType.populate(response.getCommandOutput());
        cacheProcessorType = processorType;

        return processorType;
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
        handleMessages(task, installationContext);
        if (mainCmd.hasOption(HELP)) {
            if (taskOptions.getOptions().size() != 0) {
                InstallationTask installationTask = (InstallationTask) task;


                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(installationTask.isWindows() ? "Start.bat " : "/.Start", taskOptions);
            }
        }
        task.setAdded(true);
        return true;
    }

    private static void handleMessages(IInstallationTask task, InstallationContext installationContext) {
        UserMessage[] wm = task.getWelcomeMessage();
        if (wm != null && wm.length != 0) {
            installationContext.setWelcomeMessage(wm);
        }
        UserMessage[] fm = task.getFinalMessage();
        if (fm != null && fm.length != 0) {
            installationContext.setFinalMessage(fm);
        }
        UserMessage[] rm = task.getRunningMessages();
        if (rm != null && rm.length != 0) {
            installationContext.setRunningMessage(rm);
        }
        UserMessage[] fme = task.getFinalMessageOnError();
        if (fme != null && fme.length != 0) {
            installationContext.setFinalErrorMessage(fme);
        }
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

    private static void ahowErrorandExit() {
        severe("an issue was discovered with UI plugins");

        System.out.println("***************************************************************");
        System.out.println("Please check if you have two UI plugins in the bin/tasks folder");
        System.out.println("for example maininstaller-ui-xxx.jar and wizzdiWizard-yyy.jar");
        System.out.println("You need to delete one");
        exit(0);
    }

    /**
     * start user interface(s) if exist, only one can be started
     */
    private static void loadUiComponents() {
        errorInUiComponents = true;
        uiComponents = pluginManager.getExtensions(IUIComponent.class);
        errorInUiComponents = false;
        int maxPririty = -1;
        IUIComponent selected = null;
        for (IUIComponent component : uiComponents) {
            if (component.priority() > maxPririty) {
                maxPririty = component.priority();
                selected = component;
            }
        }
        if (selected != null) {
            uiComponents.clear();
            uiComponents.add(selected);
            versions.add(0, selected.getVersion());
            selected.setContext(installationContext);
            installationContext.addUIComponent(selected);
            if (selected.isAutoStart()) {
                info("Have started  a UI plugin");
                if (selected.startAsynch(installationContext)) {
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

    public static UserResponse getUserResponse(InstallationContext context, UserAction userAction) {

        if (uiComponents != null && uiComponents.size() != 0) {
            List<IUIComponent> filtered = uiComponents.stream().filter(IUIComponent::isShowing).collect(Collectors.toList());
            if (filtered.size() > 0) {
                return filtered.get(0).askUser(context, userAction);
            }

        }
        return consoleAskUser(context, userAction);


    }


    private static UserResponse uiShowSystemData(IUIComponent iuiComponent, InstallationContext context) {
        if (iuiComponent != null) {

            return iuiComponent.showSystemData(context, systemData);

        }

        return null;
    }

    private static UserResponse uiShowPagedList(IUIComponent iuiComponent, InstallationContext context, PagedList pagedList) {
        if (iuiComponent != null) {
            return iuiComponent.showPagedList(context, pagedList);

        }

        return null;
    }

    /**
     * prompt user using console
     *
     * @param context
     * @param userAction
     * @return
     */
    private static UserResponse consoleAskUser(InstallationContext context, UserAction userAction) {
        if (userAction.isUseAnsiColorsInConsole()) AnsiConsole.systemInstall();
        UserResponse response = null;
        UserMessage last = null;

        try {
            do {
                if (last == null) {
                    for (UserMessage userMessage : userAction.getMessages()) {
                        last = printUserMessage(userMessage, userAction.getDefaultResponse().toString());
                    }
                    printDefaults(userAction);
                } else {

                    printUserMessage(last, userAction.getDefaultResponse().toString());
                    printDefaults(userAction);
                }
                String answer = getLine();

                try {
                    response = UserResponse.valueOf(answer.toUpperCase());

                } catch (IllegalArgumentException e) {

                }

            } while (response == null);
        } catch (Exception e) {
            //severe("Error while getting user console input", e);
        } finally {
            if (userAction.isUseAnsiColorsInConsole()) AnsiConsole.systemUninstall();

            return response;
        }
    }

    private static void printDefaults(UserAction userAction) {
        System.out.println(ansi().a("").reset());

        String prompt = userAction.getOptionalPrompt().isEmpty() ? "Possible answers" : userAction.getOptionalPrompt();
        System.out.println(ansi().bold().a(prompt).reset());
        if (userAction.getOptionalPrompt().isEmpty()) {
            switch (userAction.getResponseType()) {
                case BOOLEAN:
                    System.out.println(ansi().bold().a("true,false,yes,no,continue,stop").reset());
                    break;
                case EXISTINGFILE:
                    System.out.println(ansi().bold().a("type the full path to an existing file").reset());
                    break;
                case EXISTINGFOLDER:
                    System.out.println(ansi().bold().a("type the full path to an existing folder").reset());
                    break;
                case FILE:
                    System.out.println(ansi().bold().a("type the full path to a new file").reset());
                    break;
                case FROMLIST:
                    System.out.println(ansi().bold().a(userAction.getAllAnswers()).reset());
                    break;
                case LIST:
                    System.out.println(ansi().bold().a("type a coma separated list of strings").reset());
                    break;
                case STRING:
                    System.out.println(ansi().bold().a("Any String").reset());
                    break;
            }
        }

    }

    static java.io.BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private static String getLine() {


        try {
            String result = in.readLine();

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "";
    }

    /**
     * print user message to console, treats \n in message text and use left margin correctly.
     * Ansi color used when supported by the OS,
     * default response can be prompted
     *
     * @param userMessage
     * @param defaultResponse
     * @return
     */
    private static UserMessage printUserMessage(UserMessage userMessage, String defaultResponse) {
        StringBuilder leftMargin = new StringBuilder();
        String[] lines;

        if (userMessage.getLeftMargin() != -1) {
            lines = userMessage.getMessage().split("\n");
            for (int i = 0; i < userMessage.getLeftMargin(); i++) leftMargin.append(" ");

        } else {
            lines = new String[1];
            lines[0] = userMessage.getMessage();
        }
        UserMessage last;
        if (userMessage.isCrlf()) {
            for (String line : lines) {
                if (line == lines[lines.length - 1]) {
                    System.out.println(ansi().fg(userMessage.getColor()).a(leftMargin.toString() + line).reset());
                } else {
                    System.out.println(ansi().fg(userMessage.getColor()).a(leftMargin.toString() + line));
                }
            }
            last = userMessage;
        } else {
            for (String line : lines) {
                if (line == lines[lines.length - 1]) {
                    System.out.print(ansi().fg(userMessage.getColor()).a(userMessage.getMessage() + "[" + defaultResponse + "] ? ").reset());
                } else {
                    System.out.println(ansi().fg(userMessage.getColor()).a(leftMargin.toString() + line));

                }
            }
            last = userMessage;

        }
        return last;
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
     * @param propertiesFile
     */
    private static void readProperties(InstallationContext installationContext, String propertiesFile) {


        info("Will look for  properties file in: " + propertiesFile);
        try (InputStream input = new FileInputStream(propertiesFile)) {
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            installationContext.setProperties(prop);
            StringBuilder sb = new StringBuilder();
            sb.append("\n*******************************************\n");
            sb.append("********** a properties file is used because one is available here:*******\n");
            sb.append("*****************");
            sb.append(propertiesFile);
            sb.append("********** \n");
            sb.append("********** edit it, delete or use -p parameter to specify another one*******\n");
            sb.append("********** you can also save a new one with -psave [filename] options  *********, \n ******** it will be saved with new parameters defined on the command line*******\n");
            info(sb.toString());
            System.out.println(sb);

        } catch (IOException ex) {
            info("No properties file provided at: " + propertiesFile);
            installationContext.setProperties(new Properties());
        }
    }

    private static void saveProperties(InstallationContext context, File file) {
        if (file != null) {

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
                writer.write("# properties file created on: " + LocalDateTime.now() + "\n");
                writer.write("#Important notes:\n");
                writer.write("#you can edit locations in this file to reference other locations\n");
                writer.write("#so for example, it is possible to replace c:\\temp\\installations\\\n");
                writer.write("#so for example, with &installations , see names of parameters pointing to folder and files and use them as references\n");
                writer.write("#in other parameters\n");
                writer.write("#if this file is named 'properties' and placed in the bin folder of the installer it will replace default values set in code\n");
                for (IInstallationTask task : context.getiInstallationTasks().values()) {
                    writer.write("# -----------------task:" + task.getName() + " :" + getTaskDescription(task) + "# task id is: " + task.getId() + "\n");
                    writer.write(task.getId() + "=" + task.isEnabled() + "\n");
                    List<Parameter> parameters = context.getParamaters().byTask(task);
                    String toWrite = null;
                    if (parameters != null && parameters.size() != 0) {

                        for (Parameter parameter : parameters) {

                            if (parameter.getName().equals("psave"))
                                continue; // we need a properties file that cannot save itself
                            if (parameter.getValue() != null) {
                                if (parameter.isSandSymbolPresent()) {
                                    if (parameter.getValue().contains("&")) {
                                        toWrite = parameter.getName() + "=" + parameter.getValueForProperties() + getParameterDescription(parameter) + "\n";
                                        writer.write(toWrite);
                                    } else {
                                        if (parameter.getNonTranslatedValue() != null) {
                                            String description = getParameterDescription(parameter);
                                            String nonT = parameter.getNonTranslatedValue();
                                            toWrite = parameter.getName() + "=" + nonT + "\n" + description;
                                            writer.write(toWrite);
                                        }
                                    }
                                } else {
                                    toWrite = parameter.getName() + "=" + parameter.getValueForProperties() + getParameterDescription(parameter) + "\n\n";
                                    writer.write(toWrite);
                                }
                                if (parameter.getType().equals(ParameterType.LIST)) {
                                    if (parameter.getListOptions() != null) {
                                        writer.write("\n# possible options for " + parameter.getName());
                                        int i = 1;
                                        for (String option : parameter.getListOptions()) {
                                            writer.write("\n#     " + (i++) + "." + option);
                                        }
                                        writer.write("\n");
                                    }
                                }
                            }
                        }
                    } else {
                        writer.write("# task  " + task.getName() + " id: " + task.getId() + " has no parameters \n");
                    }
                    writer.write("#----------------- end of task: " + task.getName() + "\n\n\n");

                }
                logger.log(Level.INFO, "Have saved properties file :" + file.getAbsolutePath());
            } catch (IOException e) {
                logger.log(Level.SEVERE, " Error while writing properties file", e);
            }

        }
    }

    //make sure multiple lines are properly commanted
    private static String getParameterDescription(Parameter parameter) {
        String result = !(parameter.getDescription() == null || parameter.getDescription().isEmpty()) ? "\n#" + parameter.getDescription() + "\n" : "\n";
        return getCommented(result);
    }

    private static String getCommented(String result) {
        if (result.isEmpty()) return result;
        String[] lines = result.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) {
                sb.append("\n" + line + "\n");
            } else {
                sb.append("\n      #" + line + "\n");
            }

        }
        return sb.toString();
    }

    private static String getTaskDescription(IInstallationTask task) {
        String result = !(task.getDescription() == null || task.getDescription().isEmpty()) ? "#" + task.getDescription() : "";
        return getCommented(result);
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
            logger.log(Level.SEVERE, "error while parsing command line: " + e.getMessage(), e);
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
            parameter.setValueFromProperties(result);
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
                String[] split1 = result.split(":");
                if (split1.length > 0) {
                    result = split1[0];
                    String[] split = new String[0];
                    if (split1.length > 1) {
                        if (split1[1].contains(",")) split = split1[1].split(",");
                        if (split1[1].contains("|")) split = split1[1].split("\\|");
                    } else {
                        split = new String[1];
                        split[0] = split1[0];
                    }
                    if (parameter.getListOptions() == null) parameter.setListOptions(new ArrayList<>());

                    parameter.getListOptions().clear();


                    parameter.getListOptions().addAll(Arrays.asList(split));
                    parameter.getListOptions().remove(result);
                    Collections.sort(parameter.getListOptions());
                    parameter.getListOptions().add(0, result); //selected must be first in the list

                } else {
                    severe("the parameter: " + parameter.getName() + " is of list type but has no list in the properties file");
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


    private static TopologicalOrderIterator<String, DefaultEdge> getInstallationTaskIterator
            (Map<String, IInstallationTask> installationTasks) throws MissingInstallationTaskDependency {
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

    private static void informUI(String message, InstallationState state, double progress) {
        for (IUIComponent component : uiComponents) {
            component.updateStatus(installationContext, progress, missingDependencies, message, state);
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
    private static boolean informUIOnInspections(InstallationContext
                                                         context, ArrayList<InspectionResult> inspections) {

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

                .addOption(HELP, false, "shows this message")
                .addOption(DEMO_POWERSHELL, false, "Add powerShell command for tests")
                .addOption(PROPERTIES, true, "points to a properties file");

    }


    private static boolean doStop() {
        logger.info("Performing installation stop");
        killInstallation();
        return true;
    }

    private static boolean installationPaused = false;

    private static boolean doPause() {
        logger.info("Performing installation pause");
        installationPaused = true;
        informUI("Installation paused", InstallationState.PAUSED, progress);
        return true;
    }

    private static boolean doResume() {
        logger.info("Performing installation resume");
        installationPaused = false;
        informUI("Installation resumed", InstallationState.RESUMED, progress);
        return true;
    }

    private static boolean doInstallDry() {
        updateParameter = null;
        return install(installationContext, false, true);

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
            result = install(context, false, false);

        }

        return result;
    }

    private static boolean Uninstall(InstallationContext context) {
        return install(context, true, false);
    }

    static boolean stopInstall = false;
    static double startLoop;

    static Thread updateThread;
    public static AtomicBoolean finishIngTask = new AtomicBoolean(false);

    private static boolean install(InstallationContext context, boolean unInstall, boolean dry) {
        if (!dry) {
            dry = context.getParamaters().getBooleanValue("dry");
        }

        UserResponse response = showInstallPrompt(context, unInstall, dry);
        if (response.equals(UserResponse.CONTINUE) || response.equals(UserResponse.YES) || response.equals(UserResponse.TRUE)) {
            if (!installRunning) {
                boolean finalDry = dry;
                installationThread = new Thread(() -> {
                    installRunning = true;
                    long startAll = System.currentTimeMillis();
                    logger.info("Performing installation of " + context.getiInstallationTasks().size() + " tasks");
                    informUI(getInstallationMessage(unInstall) + "  running , performed 0, failed 0, skipped 0", InstallationState.STARTING, progress);
                    int failed = 0;
                    int skipped = 0;
                    int completed = 0;


                    updateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {

                            do {
                                double completedTimeLocal = completedTime;
                                double currentTaskDurationLocal = currentTaskDuration;
                                if (totalTime != 0 && !finishIngTask.get()) {
                                    long elapsedInTask = getSeconds(taskStarted);
                                    double suggestedTimeInSec = Math.min(elapsedInTask + completedTimeLocal, completedTimeLocal + currentTaskDurationLocal);

                                    // info ("Suggested time in seconds: "+suggestedTimeInSec+"\n" +
                                    //         "completedTime: "+completedTimeLocal+" current task duration: "+currentTaskDurationLocal+" current task is: "+currentTask.getName());

                                    if (suggestedTimeInSec > totalTime) {
                                        //info("************ suggested time is bigger than total time");
                                        suggestedTimeInSec = 0.98 * totalTime;
                                    }

                                    progress = suggestedTimeInSec / totalTime;
                                    informUI("", InstallationState.PROGRESSREPORT, progress);
                                }
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {

                                }
                            } while (installRunning);


                        }
                    });
                    updateThread.start();
                    updateThread.setName("progressBar");
                    HashMap<String, String> restarters = new HashMap<>();
                    String mainStatusMessage = unInstall ? "Un-installation status" : "installation status";
                    InstallProper installProper = new InstallProper(context,
                            unInstall,
                            finalDry,
                            failed,
                            skipped,
                            completed,
                            restarters,
                            mainStatusMessage).invoke();

                    if (installProper.is()) { //handling stop installation;
                        updateStatus(" Installation was stopped ", installProper.getCompleted(), installProper.getFailed(), installProper.getSkipped(), InstallationState.ABORTED);
                        installRunning = false;
                        return;
                    }
                    failed = installProper.getFailed();
                    skipped = installProper.getSkipped();
                    completed = installProper.getCompleted();
                    updateStatus(mainStatusMessage + " finalizing", completed, failed, skipped, InstallationState.FINALIZING);
                    info(" calling finalizers on all tasks");
                    if (!unInstall) {
                        FinishInstall finishInstall = new FinishInstall(context, unInstall, finalDry, failed, skipped, completed, restarters).invoke();
                        if (finishInstall.is()) {
                            updateStatus(" Installation was stopped ", installProper.getCompleted(), installProper.getFailed(), installProper.getSkipped(), InstallationState.ABORTED);
                            installRunning = false;
                            return;
                        }
                        failed = finishInstall.getFailed();
                        completed = finishInstall.getCompleted();
                    }
                    info("Total installation time was: " + getSeconds(startAll) + " Seconds");
                    updateStatus("starting cleanup ", completed, failed, skipped, InstallationState.CLEANUP);
                    if (cleanUp(context, failed, skipped, completed)) {
                        updateStatus(" Installation was stopped ", installProper.getCompleted(), installProper.getFailed(), installProper.getSkipped(), InstallationState.ABORTED);
                        installRunning = false;
                        return;
                    }
                    //ugly, we need to have one Installation task for the operation of starting required services.
                    context.setSuccessFullyInstalled(completed);
                    context.setFailedToInstall(failed);

                    if (failed == 0) {
                        updateStatus("done, no task has failed ", completed, failed, skipped, InstallationState.COMPLETE);
                    } else {
                        updateStatus("done, some tasks failed", completed, failed, skipped, InstallationState.PARTLYCOMPLETED);
                    }

                    if (updateParameter != null) updateParameter.setValue(oldUpdate);
                    if (!uiFoundandRun) stopped = true; //command line will execute here
                    installRunning = false;
                    informUI("", InstallationState.PROGRESSREPORT, 1);
                });
                installationThread.start();
            } else {
                info("Install already running");
                return true;
            }
        }
        return false;
    }

    /**
     * for now it does nothing.
     *
     * @param context
     * @param failed
     * @param skipped
     * @param completed
     * @return
     */
    private static boolean cleanUp(InstallationContext context, int failed, int skipped, int completed) {
        for (IInstallationTask installationTask : context.getCleanupTasks().values()) {
            checkStopInstall();
            installationTask.setProgress(70).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now());
            try {
                if (installationTask.install(installationContext).equals(InstallationStatus.COMPLETED)) {

                } else {

                }
                doUpdateUI(installationTask, installationContext);
            } catch (Throwable throwable) {
                if (throwable instanceof InterruptedException) {
                    installRunning = false;
                    return true;
                }
                severe("Error while cleanup task run " + installationTask.getName());
            }
        }
        return false;
    }

    static Thread installationThread;
    static boolean wasStopped = false;

    /**
     * check if any task has stopped installation
     *
     * @return
     */
    private static boolean checkStopInstall() {
        if (stopInstall) {
            installRunning = false;
            return true;
        }
        return false;
    }

    private static UserResponse showInstallPrompt(InstallationContext context, boolean unInstall, boolean dry) {
        if (!context.getParamaters().getBooleanValue("quiet")) {
            if (installRunning) {
                UserAction ua = new UserAction();
                ua.addMessage(new UserMessage().setMessage("Installation is running, stop it?").
                        setEmphasize(3).
                        setColor(Color.RED));
                ua.setPossibleAnswers(new UserResponse[]{UserResponse.NO, UserResponse.FORCESTOP});
                ua.setUseAnsiColorsInConsole(true);

                UserResponse userResponse = getUserResponse(context, ua);
                if (!userResponse.equals(UserResponse.FORCESTOP)) {
                    return UserResponse.STOP;
                }


                killInstallation();
            }
            UserAction ua = new UserAction();

            ua.setTitle((updateParameter == null ? (unInstall ? "Please confirm un-installation" :
                    (dry ? "Please confirm dry installation" : "Please confirm installation")) : (updateParameter.getBoolean() ? "Please confirm update " : "Please confirm installation")));
            ua.addMessage(new UserMessage().setMessage(""));
            ua.addMessage(new UserMessage().setMessage(updateParameter == null ? unInstall ? "will un-install the following tasks:" : "will install the following tasks:" : updateParameter.getBoolean() ? "will update the following tasks " : "will install the following tasks:").
                    setEmphasize(2).
                    setColor(Color.GREEN).setLeftMargin(20).setFontSize(16));
            ua.addMessage(new UserMessage().setMessage(""));
            for (IInstallationTask task : context.getiInstallationTasks().values()) {
                if (task.isEnabled()) {
                    ua.addMessage(new UserMessage().setMessage(task.getName()).
                            setEmphasize(1).
                            setColor(uiFoundandRun ? Color.BLACK : Color.WHITE).setLeftMargin(10));
                }
            }
            ua.setPossibleAnswers(new UserResponse[]{UserResponse.CONTINUE, UserResponse.STOP});
            ua.setOptionalPrompt("Type YES to continue with the installation/update");
            ua.setUseAnsiColorsInConsole(true);
            UserResponse response = getUserResponse(context, ua);
            return response; //
        } else {
            info("Will not prompt for continue installation");
            return !installRunning ? UserResponse.CONTINUE : UserResponse.STOP;
        }


    }

    private static void killInstallation() {
        stopInstall = true;
        while (installRunning) { //wait here till installationThread is confirming stop
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {

            }
        }
        installationThread.interrupt();

        while (installationThread.isAlive()) ;
        updateStatus(("installation is ") + "aborted: ", 0, 0, 0, InstallationState.ABORTED);
        info("Have aborted installation by user");


    }

    private static String getInstallationMessage(boolean unInstall) {
        return unInstall ? "un-installation" : "installation";
    }


    private static void updateStatus(String message, int completed, int failed, int skipped, InstallationState state) {
        informUI(message + " Completed tasks: " + completed + " Failed tasks: " + failed + " Skipped tasks: " + skipped, state, progress);
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
        updateParameter = null;
        return install(context, false, false);
    }

    private static boolean uiComponentUnInstall(IUIComponent component, InstallationContext context) {
        updateParameter = null;
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

    private static void uiComponentShowLogs(IUIComponent uiComponent, InstallationContext context) {
        try {
            doshowLogs(uiComponent, context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    private static UserResponse uiComponentShowSystemData(IUIComponent iuiComponent, InstallationContext context) {

        return doShowSystemData(iuiComponent, context);


    }


    private static String UIAccessAbout(IUIComponent uiComponent, InstallationContext context) {

        return doAbout(uiComponent, context);
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

    private static String doAbout(IUIComponent uiComponent, InstallationContext context) {
        String title = "Installer plugins versions";
        Parameter paramater = context.getParameter("maintitle");
        if (paramater != null) title = paramater.getValue();
        logger.info("Performing show about ");
        PagedList<String> pLines = new PagedList();

        pLines.setPageSize(25).setShowSearch(false).setTitle(title).addAll(versions);
        UserResponse result = uiComponent.showPagedList(context, pLines);
        return "";
    }

    private static UserResponse doShowSystemData(IUIComponent iuiComponent, InstallationContext context) {
        UserResponse result = uiShowSystemData(iuiComponent, context);
        return result;
    }

    private static void doshowLogs(IUIComponent uiComponent, InstallationContext context) throws IOException {
        logger.info("Performing show logs");

        File file = new File(System.getProperty("user.dir") + "/logs/installer.log");
        if (file.exists()) {
            List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
            PagedList<String> pLines = new PagedList();
            pLines.setPageSize(25).setShowSearch(true).setTitle("Installer log").addAll(lines);
            UserResponse result = uiComponent.showPagedList(context, pLines);
        }
    }

    /**
     * returns the next page
     *
     * @param lines
     * @param pageSize
     * @param pageNumber
     * @return
     */
    private static String[] getPage(List<String> lines, int pageSize, int pageNumber) {
        String[] result = new String[(Math.min(lines.size() - pageNumber * pageSize, pageSize))];
        int j = 0;
        for (int i = pageNumber * pageSize; i < (pageNumber + 1) * pageSize; i++) {
            if (i == lines.size()) return result;
            result[j++] = lines.get(i);
        }
        return result;
    }

    static double progress = 0d;
    /**
     * the estimated time for performing all tasks, note that if waiting for user actions
     */
    static double totalTime = 0;
    /**
     * the time passed till the end of next task.
     * before the task is completed or failed, the time will be the earlier of the current time or calculated
     * updated every two seconds.
     */
    static double completedTime = 0;
    static double currentTaskDuration = 0;
    static long taskStarted;
    static InstallationTask currentTask;


    private static class InstallProper {
        private static final long DRY_FAKE_WAIT = 500;
        private boolean myResult;
        private InstallationContext context;
        private boolean unInstall;
        private boolean dry;
        private int failed;
        private int skipped;
        private int completed;
        private HashMap<String, String> restarters;
        private String mainStatusMessage;
        private UserResponse response = UserResponse.CONTINUE;


        public InstallProper(InstallationContext context, boolean unInstall, boolean dry, int failed, int skipped, int completed, HashMap<String, String> restarters, String mainStatusMessage) {
            this.context = context;
            this.unInstall = unInstall;
            this.dry = dry;
            this.failed = failed;
            this.skipped = skipped;
            this.completed = completed;
            this.restarters = restarters;
            this.mainStatusMessage = mainStatusMessage;
        }

        boolean is() {
            return myResult;
        }

        public int getFailed() {
            return failed;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getCompleted() {
            return completed;
        }

        public UserResponse getResponse() {
            return response;
        }

        boolean wasPaused = false;


        public InstallProper invoke() {

            double totalInstallersTime = 0d;
            double totalServiceRestartTime = 0d;
            double totalFinalizersTime = 0d;
            context.calculateFactor(); //allow properties file to affect duration (an UI too)
            boolean stopatend=context.getParamaters().getBooleanValue("stopattaskend");
            StringBuilder sb = new StringBuilder();
            int count = 0;
            sb.append("\n---------------------- tsaks to be installed in the calculated order--------");

            for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {

                sb.append("\n\n\n " + (++count) + "    " + installationTask.getName() + "  " + installationTask.getDescription());
                ((InstallationTask) installationTask).setDry(dry); //cater for dry in parameters
                totalInstallersTime += (installationTask.isEnabled() && !installationTask.isWrongOS()) ? ((InstallationTask) installationTask).getFactoredDuration() : 0d;
                totalServiceRestartTime += (installationTask.isEnabled() && !installationTask.isWrongOS()) ? ((InstallationTask) installationTask).getFactoredServiceDuration() : 0d;
                totalFinalizersTime += (installationTask.isEnabled() && !installationTask.isWrongOS()) ? ((InstallationTask) installationTask).getFactoredFinalizerDuration() : 0d;
                if (stopatend) {
                    UserResponse result = askUserToContinue(installationTask);
                    if (result.equals(UserResponse.SKIP)) continue;
                    if (result.equals(UserResponse.FORCESTOP)) break;
                }
            }
            sb.append("----------------------done with installation-------------------------------------------------------");
            info(sb.toString());

            info("Total installer time is: " + totalInstallersTime);
            info("Total service restart time is: " + totalServiceRestartTime);
            info("total finalizers time is: " + totalFinalizersTime);
            totalTime = totalInstallersTime + totalServiceRestartTime + totalFinalizersTime;
            info("Total time is: " + totalTime);

            startLoop = System.currentTimeMillis();


            for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {

                long pauseStarted = System.currentTimeMillis();
                while (installationPaused) {
                    wasPaused = true;
                    updateStatus("Installation paused for the last " + (System.currentTimeMillis() - pauseStarted) / 1000, completed, failed, skipped, InstallationState.PAUSED);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                }
                if (wasPaused) {
                    wasPaused = false;
                    updateStatus("Installation resumed  after a pause of " + (System.currentTimeMillis() - pauseStarted) / 1000, completed, failed, skipped, InstallationState.RUNNING);

                }
                if (checkStopInstall()) {
                    myResult = true;
                    return this;
                }
                if (!installationTask.isEnabled() && !installationTask.isWrongOS()) {
                    info("task " + installationTask.getName() + " is disabled, skipping");
                    skipped++;
                    updateStatus(mainStatusMessage, completed, failed, skipped, InstallationState.RUNNING);
                    continue;

                }
                sb = new StringBuilder();
                sb.append("\n**********************************************");
                sb.append("\nwill now " + (unInstall ? "uninstall" : "install ") + installationTask.getName() + " id: " + installationTask.getId());
                sb.append("\ndetails: " + installationTask.getDescription());
                info(sb.toString());
                installationTask.setProgress(0).setStatus(InstallationStatus.STARTED).setStarted(LocalDateTime.now()).setMessage(" Started " + getInstallationMessage(unInstall));
                doUpdateUI(installationTask, installationContext);

                try {
                    taskStarted = System.currentTimeMillis();
                    currentTask = (InstallationTask) installationTask;
                    boolean test = currentTask.installSpring(context);
                    if (!installationTask.isSnooper()) {
                        if (!installationTask.getNeedRestartTasks().isEmpty()) {
                            for (String service : installationTask.getNeedRestartTasks()) {
                                restarters.put(service, service);
                            }
                        }
                        InstallationResult result = null;
                        currentTaskDuration = ((InstallationTask) installationTask).getFactoredDuration();
                        Start.finishIngTask.getAndSet(false);
                        if (!dry) {
                            info("++++++++++++ about to install: " + installationTask.getName());
                            result = unInstall ? installationTask.unInstall(context) : installationTask.install(context);
                            if (!unInstall && result.getUserAction() != null && result.getUserAction().isAskForUpdate()) {
                                UserResponse userResponce = getUserResponse(installationContext, result.getUserAction());
                                if (userResponce.equals(UserResponse.YES)) {
                                    info("User has asked to update this task: " + installationTask.getName());
                                    ((InstallationTask) installationTask).setUpdateThis(true);
                                    result = unInstall ? installationTask.unInstall(context) : installationTask.install(context);
                                }
                            }
                        } else {
                            result = new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                        }
                        Start.finishIngTask.getAndSet(true);
                        completedTime += ((InstallationTask) installationTask).getFactoredDuration();

                        info("Actual time for task: " + installationTask.getName() + " is:" + getSeconds(taskStarted) + " defined time: " + ((InstallationTask) installationTask).getFactoredDuration());
                        if (result.getUserAction() != null)
                            response = getUserResponse(context, result.getUserAction());

                        switch (result.getInstallationStatus()) {
                            case COMPLETED:
                                completed++;
                                updateStatus((unInstall ? "un-installation" : "installation is ") + "running: ", completed, failed, skipped, InstallationState.RUNNING);
                                info("Have successfully finished " + (unInstall ? "un-installation task: " : "installation task: ") + installationTask.getName() + " after " + getSeconds(taskStarted) + " Seconds");
                                installationTask.setProgress(100).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.COMPLETED);
                                installationTask.setMessage(getInstallationMessage(unInstall) + " succeeded");

                                if (dry) {
                                    info("Will now wait so dry run will take some time...");
                                    // Thread.sleep(DRY_FAKE_WAIT);
                                }
                                break;
                            case FORCEABORT:
                                updateStatus((unInstall ? "un-installation" : "installation is ") + "aborted: ", completed, failed, skipped, InstallationState.ABORTED);
                                info("Have aborted installation by " + (unInstall ? "un-installation task: " : "installation task: ") + installationTask.getName() + " after " + getSeconds(taskStarted) + " Seconds");
                                myResult = true;
                                installationTask.setMessage(getInstallationMessage(unInstall) + " aborted");

                                return this;

                            case FAILED:

                                info("-----------Failed task: " + installationTask.getName());
                                failed++;
                                updateStatus(mainStatusMessage, completed, failed, skipped, InstallationState.RUNNING);
                                installationTask.setProgress(0).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.FAILED);
                                info("Have unsuccessfully finished " + (unInstall ? "un-installation task" : " installation task: ") + installationTask.getName() + " after " + getSeconds(taskStarted) + " Seconds");
                                installationTask.setMessage(getInstallationMessage(unInstall) + " failed");


                        }

                        doUpdateUI(installationTask, installationContext);

                    } else {
                        Thread snooper = new Thread(() -> {
                            try {
                                InstallationResult result = unInstall ? installationTask.unInstall(context) : installationTask.install(context);
                            } catch (Throwable throwable) {
                                if (throwable instanceof InterruptedException) {
                                    installRunning = false;
                                    return;
                                }
                                severe("Exception while running a snooper", throwable);
                            }
                        });
                        snooper.setName(installationTask.getId());
                        snoopers.add(snooper);
                        snooper.start();
                    }
                } catch (Throwable throwable) {
                    if (throwable instanceof InterruptedException) {
                        installRunning = false;
                        myResult = true;
                        return this;
                    }
                    severe("Exception while installing: " + installationTask.getName(), throwable);
                    installationTask.setProgress(0).setEnded(LocalDateTime.now()).setStatus(InstallationStatus.FAILED);


                }
                if (context.getConsumer() != null) {
                    context.getConsumer().updateProgress(installationTask, context);
                }
                // installTask(installationTask, context);
            }
            myResult = false;
            return this;
        }

        private UserResponse askUserToContinue(IInstallationTask task) {
            UserAction ua = new UserAction();
            ua.addMessage(new UserMessage().setMessage("New task "+ task.getId()+" is ready to install ").
                    setEmphasize(3).
                    setColor(Color.RED));
            ua.setPossibleAnswers(new UserResponse[]{UserResponse.CONTINUE, UserResponse.SKIP,UserResponse.FORCESTOP});
            ua.setUseAnsiColorsInConsole(true);

            UserResponse userResponse = getUserResponse(context, ua);
            return  userResponse;
        }


    }

    private static class FinishInstall {
        private boolean myResult;
        private InstallationContext context;
        private boolean uninstall;
        private boolean dry;
        private int failed;
        private int skipped;
        private int completed;
        private HashMap<String, String> restarters;

        /**
         * handle services
         *
         * @param context
         * @param failed
         * @param skipped
         * @param completed
         * @param restarters
         */
        public FinishInstall(InstallationContext context, boolean uninstall, boolean dry, int failed, int skipped, int completed, HashMap<String, String> restarters) {
            this.context = context;
            this.uninstall = uninstall;
            this.dry = dry;
            this.failed = failed;
            this.skipped = skipped;
            this.completed = completed;
            this.restarters = restarters;
        }

        boolean is() {
            return myResult;
        }

        public int getFailed() {
            return failed;
        }

        public int getCompleted() {
            return completed;
        }

        public FinishInstall invoke() {
            checkStopInstall();
            if (context.getiInstallationTasks().size() != 0) {
                InstallationTask task = (InstallationTask) context.getiInstallationTasks().values().toArray()[0]; //use first task, as the function
                if (!dry) {

                    taskStarted = System.currentTimeMillis();
                    currentTask = task;
                    for (String service : restarters.values()) {

                        if (!task.testServiceRunning(service, "Installer runner", false)) {
                            if (task.setServiceToStart(service, "Starting services")) {
                                info("Have started service: " + service);
                                task.setMessage("Service started");
                                //doUpdateService(context,new Service().setName(service),task);
                                // doUpdateUI(task, installationContext);
                            } else {
                                severe("Have failed to start service: " + service);
                                task.setMessage("Service failed to start");
                                //  doUpdateService(context,new Service().setName(service),task);
                                //  doUpdateUI(task, installationContext);
                            }
                        }
                    }
                    info("restarted all required services in: " + getSeconds(taskStarted) + " seconds");
                }
            }
/**
 * call finalizers here.
 */
            for (IInstallationTask installationTask : context.getiInstallationTasks().values()) {
                if (!installationTask.isSnooper()) {
                    checkStopInstall();
                    InstallationResult result;
                    try {
                        if (installationTask.isEnabled()) {

                            if (!dry) {
                                taskStarted = System.currentTimeMillis();
                                currentTaskDuration = ((InstallationTask) installationTask).getFactoredFinalizerDuration();
                                Start.finishIngTask.getAndSet(false);
                                info("******* going to finalize task: " + installationTask.getName());
                                result = installationTask.finalizeInstallation(context);
                                info("Finalizer for task: " + installationTask.getName() + " took " + getSeconds(taskStarted) + " seconds");
                                Start.finishIngTask.getAndSet(true);
                                completedTime += ((InstallationTask) installationTask).getFactoredFinalizerDuration();
                                if (result.getInstallationStatus().equals(InstallationStatus.COMPLETED)) {
                                    result.setInstallationStatus(InstallationStatus.FINALIZERCOMPELETED);
                                }
                            } else {
                                result = new InstallationResult().setInstallationStatus(InstallationStatus.FINALIZERCOMPELETED);
                            }
                            InstallationStatus status = result.getInstallationStatus();
                            if (status.equals(InstallationStatus.COMPLETED) || status.equals(InstallationStatus.FINALIZERCOMPELETED)) {
                                completed++;
                                installationTask.setMessage("Finalizer completed");
                                doUpdateUI(installationTask, installationContext);
                            } else {
                                failed++;
                                info("-----------Failed task while finalizing: " + installationTask.getName());
                                installationTask.setMessage("Finalizer failed");
                                doUpdateUI(installationTask, installationContext);
                            }
                            updateStatus("finalizing  status", completed, failed, skipped, InstallationState.FINALIZING);

                        }
                    } catch (Throwable throwable) {
                        if (throwable instanceof InterruptedException) {
                            installRunning = false;
                            myResult = true;
                            return this;
                        }
                        severe("Error while finalizing task: " + installationTask.getName());
                        installationTask.setMessage("Finalizer failed");
                        doUpdateUI(installationTask, installationContext);
                    }
                }
            }
            myResult = false;
            return this;
        }
    }

    public static SystemData getSystemData() {
        return systemData;
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
        void uiComponentShowLogs(IUIComponent uiComponent, InstallationContext context);
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

    @FunctionalInterface
    public static interface AskUser {
        UserResponse dialog(InstallationContext context, UserAction userAction);
    }

    @FunctionalInterface
    public static interface ShowSystemData {
        UserResponse showSystemData(IUIComponent uiComponent, InstallationContext context);
    }

    /**
     * see a function implementing this interface and a varaible in InstallationContext saving a point to this function.
     */
    @FunctionalInterface
    public static interface ShowPagedData {
        UserResponse showSystemData(IUIComponent uiComponent, InstallationContext context, PagedList pagedList);
    }


}


