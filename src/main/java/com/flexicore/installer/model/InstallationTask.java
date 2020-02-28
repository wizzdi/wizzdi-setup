package com.flexicore.installer.model;


import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.FolderCompression;
import com.flexicore.installer.utilities.StreamGobbler;
import com.wizzdi.installer.*;
import jpowershell.PowerShell;
import jpowershell.PowerShellResponse;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class InstallationTask implements IInstallationTask {
    private LocalDateTime started;
    private LocalDateTime ended;
    private Integer progress = 0;
    private String name = "Unnamed task";
    private String id;
    private String version = "1.0.0";
    private boolean enabled = true;
    private boolean wrongOS = false;


    private InstallationStatus status = InstallationStatus.CREATED;
    private String description = "no description";
    private String message = "";

    private InstallationContext context;
    private int order = -1;
    private boolean admin = false;
    public boolean stop = false;
    private Service service;
    private int filesCopied = 0;
    private int filesFailed = 0;
    private int foldersCopied = 0;
    private int foldersFailed = 0;
    private int totalFiles;
    private int totalFolders;
    private boolean progressOnFolders = false;

    public Parameters getPrivateParameters(InstallationContext installationContext) {
        return new Parameters();
    }


    final public static boolean isWindows = SystemUtils.IS_OS_WINDOWS;
    final public static boolean isLinux = SystemUtils.IS_OS_LINUX;
    final public static boolean isMac = SystemUtils.IS_OS_MAC;
    final public static boolean is64 = System.getProperty("sun.arch.data.model").equals("64");
    Queue<String> lines = new ConcurrentLinkedQueue<String>();
    Queue<String> errorLines = new ConcurrentLinkedQueue<String>();

    public boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Override
    public OperatingSystem getCurrentOperatingSystem() {
        if (isWindows) return OperatingSystem.Windows;
        if (isLinux) return OperatingSystem.Linux;
        if (isMac) return OperatingSystem.OSX;
        return OperatingSystem.Linux;
    }

    public boolean stopWildfly(String ownerName, long wait) {
        long started=System.currentTimeMillis();
        if (isWindows()) {
            started=System.currentTimeMillis();
            forceKillService("java");
            info ("Stopped wildlfy by force  in "+(System.currentTimeMillis()-started));
            info("Killed wildfly by PowerShell command");
            return true;
        }
        if (testServiceRunning("wildfly", ownerName, false)) {

            if (!waitForServiceToStop("wildfly", ownerName, true, wait)) {
                severe("Was not able to stop service Wildfly");

            }else {
                info ("Stopped wildlfy by PS in "+(System.currentTimeMillis()-started));
            }
        } else {
            info("Wildfly service was not running, no need to stop it.");
            return true;
        }
        return false;
    }

    private void forceKillService(String taskManagerName) {
        PowerShellResponse response = executePowerShellCommand("get-process " + taskManagerName + " -IncludeUserName", 3000, 5);
        if (!response.isError() && !response.isTimeout()) {
            String asString = response.getCommandOutput();
            String[] split = asString.split("\n");
            for (String line : split) {
                if (line.contains("NT AUTHORITY")) {

                    split = line.split(" ");
                    int i = 0;
                    for (String part : split) {
                        if (part.contains("NT")) {
                            response = executePowerShellCommand("Stop-process -id " + split[i - 1] + " -force", 1000, 5);
                            info("Have killed service " + taskManagerName);
                        }
                        i++;
                    }
                }
            }

        }
    }

    /**
     * wait for service to stop
     *
     * @param serviceName
     * @param owner
     * @param waitTimeMS
     * @return
     */
    public boolean waitForServiceToStop(String serviceName, String owner, boolean stopit, long waitTimeMS) {
        if (stopit) setServiceToStop(serviceName, owner);
        long started = System.currentTimeMillis();
        do {
            if (testServiceRunning(serviceName, "loop while waiting for service to stop", false)) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    return false;
                }
            } else {
                info("Service has stopped after " + (System.currentTimeMillis() - started + " milliseconds"));
                return true;
            }
        } while ((System.currentTimeMillis() - started) < waitTimeMS);
        info("Service has NOT stopped after " + (System.currentTimeMillis() - started + " milliseconds"));
        return false;
    }

    /**
     * check of a service by a known name is running.
     *
     * @param serviceName
     * @param ownerName   , for logging purposes (name)
     * @param wait        wait for a defined time (now second) before declaring service not running, useful when the service has just started
     * @return
     */
    public boolean testServiceRunning(String serviceName, String ownerName, boolean wait) {
        boolean result = false;
        int times = 0;
        do {
            if (isWindows) {

                result = executeCommand("sc query " + serviceName, "RUNNING", "checking if service " + serviceName + " runs");
            } else {
                result = executeCommand("service  " + serviceName + " status", "active", "checking if service " + serviceName + " runs");
            }
            if (result) break;
            times++;
            try {
                if (wait) Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        } while (!result && times < 20 && wait);
        return result;
    }

    public InstallationTask() {
    }

    /**
     * set here for easier testing (shorter code)
     *
     * @param installationTasks
     */
    public InstallationTask(Map<String, IInstallationTask> installationTasks) {
        installationTasks.put(this.getId(), this);
    }

    /**
     * Windows only, set service to run on startup
     *
     * @param serviceName
     * @param ownerName
     * @return
     */
    public boolean setServiceToAuto(String serviceName, String ownerName) {
        if (isWindows) {
            return executeCommand("sc config " + serviceName + " start= auto", "success", "Set Service To Auto");
        }
        return true;
    }

    /**
     * old method of informing UI
     *
     * @param phase
     * @param severity
     * @param message
     */
    @Deprecated
    public void addMessage(String phase, String severity, String message) {
        if (severity != "info") {
            severe(phase + "  " + message);
        } else {
            info(phase + "  " + message);
        }

    }

    /**
     * Stop service, this is required when updating
     *
     * @param serviceName
     * @param ownerName
     * @return
     */
    public boolean setServiceToStop(String serviceName, String ownerName) {
        if (isWindows) {
            PowerShellResponse response = executePowerShellCommand("Stop-Service -name " + serviceName, 10000, 5);
            return true;
            //return executeCommand("sc stop " + serviceName, "STOP_PENDING", "Set Service To Stop " + serviceName);
        } else {
            return executeCommand("service  " + serviceName + " stop", "", "Set Service To Stop " + serviceName);
        }
    }

    public boolean removeService(String serviceName, String ownerName) {
        if (!waitForServiceToStop(serviceName, ownerName, true, 10000)) {
            info("Could not stop service: " + serviceName);
            return false;
        }
        if (isWindows) {
            boolean result;
            result = executeCommand("sc delete " + serviceName, "", "remove service " + serviceName);
            return result;
        } else {
            if (executeCommand("systemctl disable " + serviceName, "", ownerName)) {
                if (executeCommand("rm /etc/systemd/system/" + serviceName, "", ownerName)) {
                    if (executeCommand(" systemctl daemon-reload", "", ownerName)) {
                        return (executeCommand(" systemctl reset-failed", "", ownerName));
                    }
                }
            }
        }
        return false;
    }

    /**
     * install service from a service file, Linux only
     *
     * @param serviceLocation
     * @param serviceName
     * @param ownerName
     * @return
     */
    public boolean installService(String serviceLocation, String serviceName, String ownerName) {

        if (isWindows) return false;
        try {
            if (testServiceRunning(serviceName, ownerName, false)) {
                setServiceToStop(serviceName, ownerName);
            }
            Files.copy(Paths.get(serviceLocation), Paths.get("/etc/systemd/system/" + serviceName + ".service"), StandardCopyOption.REPLACE_EXISTING);

            if (executeCommand("systemctl enable " + serviceName, "", "Set " + serviceName + " to start automatically")) {
                updateProgress(getContext(), serviceName + " service will automatically start");

                if (executeCommand("systemctl daemon-reload", "", "setting service" + serviceName)) {
                    if (setServiceToStart(serviceName, ownerName)) {
                        if (testServiceRunning(serviceName, ownerName, true)) {
                            updateProgress(getContext(), serviceName + " service has been started");
                            return true;
                        } else {
                            info("Cannot start service: " + serviceName);
                        }
                    } else {
                        info("Cannot start service: " + serviceName);
                    }
                } else {
                    info("Cannot reload systemctl daemon: " + serviceName);
                }

            } else {
                info("Cannot enable service: " + serviceName);
            }
        } catch (Exception ex) {
            severe("Exception while starting service: " + serviceName);
        }
        return false;
    }

    //todo: check that Linux version works.
    public boolean setServiceToStart(String serviceName, String ownerName) {
        if (isWindows()) {
            PowerShellResponse response = executePowerShellCommand("Start-Service -name " + serviceName, 10000, 5);
            return true;

            //return executeCommand("sc start " + serviceName, "START_PENDING", "Set Service To Stop " + serviceName);
        } else {
            return executeCommand("service  " + serviceName + " restart", "", "Set Service To restart  " + serviceName);
        }
    }

    /**
     * check if path exists
     *
     * @param file String, name of path
     * @return
     */
    public boolean exists(String file) {
        return new File(file).exists();
    }

    /**
     * @param script    execute a bash script. it includes setting the script to X flag
     * @param toFind
     * @param ownerName
     * @return
     */
    public boolean executeBashScript(String script, String toFind, String ownerName, boolean setCurrentFolder) {

        if (new File(script).exists() && !isWindows()) {

            executeCommand("chmod +x " + script, "", "change mode to execute");
            String[] cmd = new String[]{"/bin/sh", script};
            try {
                boolean result = executeCommandByBuilder(cmd, toFind, false, ownerName, setCurrentFolder);

                info("Result from script execution was: " + result);
                return result;
            } catch (IOException e) {
                severe("Exception while running script: " + script, e);
                return false;
            }
        } else {

            severe("******Cannot find script: " + script);

            return false;
        }
    }

    /**
     * Creates a shortcut, Windows only
     *
     * @param targetLocation   the file (executable or other) that is the target of the link
     * @param shortcutLocation the location of the shortcut, can be System.getProperty("user.home")+"/Desktop/"+"shortcut.lnk" (example)
     * @param iconIndex        index in the SystemRoot%\system32\SHELL32.dll icons
     * @param workingDir       the working directory for the target file.
     * @return
     */
    public boolean createLink(String targetLocation, String shortcutLocation, int iconIndex, String workingDir) {
        if (!isWindows()) return false;
        File file = new File(targetLocation);
        if (file.exists()) {
            ShellLink sl = ShellLink.createLink(targetLocation)
                    .setWorkingDir(workingDir)
                    .setIconLocation("%SystemRoot%\\system32\\SHELL32.dll");
            sl.getHeader().setIconIndex(iconIndex);
            try {
                sl.saveTo(shortcutLocation);
                info("Have successfully created a shortcut at: " + shortcutLocation);
                return true;
            } catch (IOException e) {
                severe("Error while creating shortcut", e);
            }
        }
        return false;

    }

    @Deprecated
    public boolean executeBashScriptLocal(String name, String toFind, String ownerName) {

        return executeBashScript(getScriptsPath() + "/" + name, toFind, ownerName, true);
    }

    /**
     * execute an os command
     *
     * @param command
     * @param toFind
     * @param ownerName
     * @return true if successful
     */
    public boolean executeCommand(String command, String toFind, String ownerName) {


        Process process;

        try {
            if (!isWindows) {
                String[] cmd = {"/bin/bash", "-c", "echo \"" + context.getParamaters().getValue("sudopassword") + "\" | sudo -S " + command};

                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(command);
            }


            Boolean result = contWithProcess(process, toFind, false, ownerName);
            return result;

        } catch (IOException e) {
            error("Error while executing command : " + command + " ownername: " + ownerName, e);
            return false;
        }

    }

    public void info(String message) {
        if (context != null) {
            if (context.getLogger() != null) context.getLogger().info(message);
        }
    }

    /**
     * write an error to the log file
     *
     * @param message
     * @param e
     */
    public void error(String message, Throwable e) {
        if (context != null) {
            if (context.getLogger() != null) context.getLogger().log(Level.SEVERE, message, e);
        }
    }

    public void severe(String message, Throwable e) {

        error(message, e);
    }

    public void severe(String message) {
        if (context != null) {
            if (context.getLogger() != null) context.getLogger().log(Level.SEVERE, message);
        }
    }

    void debuglines(String command, boolean force) {

        if (errorLines.size() != 0) {

            for (String message : errorLines) {
                if (!message.isEmpty()) {
                    info(message);
                }
            }

        }
        if (lines.size() != 0 && (force || isExtraLogs())) {

            for (String message : lines) {
                if (!message.isEmpty()) {
                    info(message);
                }
            }

        }
    }

    public static PowerShellResponse executePowerShellCommand(String command, Integer maxWait, Integer waitPause) {

        Map<String, String> myConfig = new HashMap<>();
        myConfig.put("maxWait", maxWait.toString());
        myConfig.put("waitPause", waitPause.toString());
        PowerShellResponse response = PowerShell.executeSingleCommand(command);
        return response;
    }


    public boolean uninstallByName(String name) {

        List<WindowsInstalledComponent> result = getInstalledClasses(name);
        switch (result.size()) {

            case 1:
                boolean uninstallResult = uninsatllGUID(result.get(0).getIdentifyingNumber(), result.get(0).getName());
                if (uninstallResult) {
                    return true;
                }

                break;
            case 0:
            default:

        }

        return false;
    }

    /**
     * Windows only, uninstall by product guid
     *
     * @param identifyingNumber
     * @param name
     * @return
     */
    public boolean uninsatllGUID(String identifyingNumber, String name) {
        if (!isWindows()) return false;
        boolean result = false;
        String currentFolder = System.getProperty("user.dir");
        String logFile = currentFolder + "/uninstall-" + name + ".log";
        logFile = logFile.replace("/", "\\");
        String[] args = new String[]{"msiexec.exe",
                "/x",
                "\"" + identifyingNumber + "\"",
                "/QN",
                "/L*V",
                logFile,
                "REBOOT=R"};

        try {
            result = executeCommandByBuilder(args, "", false, "", false);
        } catch (IOException e) {
            severe("Error while uninstalling " + name, e);
        }
        return result;
    }

    public List<WindowsInstalledComponent> getInstalledClasses(String pattern) {
        String command;
        if (pattern.contains("*")) {
            command = "Get-WmiObject -Class Win32_Product | Where-Object{$_.Name -like '" + pattern + "'}";
        } else {
            command = "Get-WmiObject -Class Win32_Product | Where-Object{$_.Name -like  '*" + pattern + "*'}";
        }
        PowerShellResponse response = executePowerShellCommand(command, 120000, 10);
        List<WindowsInstalledComponent> result = getComponents(response);
        if (result != null) return result;
        return null;
    }

    public List<WindowsInstalledComponent> getallInstalledClasses() {
        String command = "Get-WmiObject -Class Win32_Product";
        PowerShellResponse response = executePowerShellCommand(command, 120000, 10);
        List<WindowsInstalledComponent> result = getComponents(response);
        if (result != null) return result;
        return null;
    }

    private List<WindowsInstalledComponent> getComponents(PowerShellResponse response) {
        List<WindowsInstalledComponent> result = new ArrayList<>();
        if (response != null) {
            String[] split = response.getCommandOutput().split("\n");
            if (split != null && split.length > 1) {
                WindowsInstalledComponent component = null;
                for (String line : split) {

                    String[] parsed = line.split(":");
                    if (parsed.length == 2) {

                        String key = parsed[0].trim();
                        String value = parsed[1].trim();
                        switch (key) {
                            case "IdentifyingNumber":
                                component = new WindowsInstalledComponent();
                                result.add(component);
                                component.setIdentifyingNumber(value);
                                break;
                            case "Name":
                                component.setName(value);
                                break;
                            case "Vendor":
                                component.setVendor(value);
                                break;
                            case "Caption":
                                component.setCaption(value);
                                break;
                            case "Version":
                                component.setVersion(value);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }


        }
        return result;
    }

    public boolean executeCommandByRuntime(String target, String ownerName) {
        return executeCommand(target, "", ownerName);

    }

    /**
     * execute
     *
     * @param args
     * @param toFind
     * @param notToFind
     * @param ownerName
     * @param setCurrentFolder
     * @return
     * @throws IOException
     */
    public boolean executeCommandByBuilder(String[] args, String toFind, boolean notToFind, String ownerName, boolean setCurrentFolder) throws IOException {


        ProcessBuilder pb = new ProcessBuilder(args);
        if (setCurrentFolder) {
            if (args[0].equals("/bin/sh")) {
                pb.directory(new File(args[1]).getParentFile()); //set current directory to the actual location of the script
            } else {
                pb.directory(new File(args[0]).getParentFile());
            }
        }

        Process process;
        process = pb.start();

        boolean result = false;
        try {
            result = contWithProcess(process, toFind, notToFind, ownerName);
        } catch (Exception e) {
            severe("error", e);
        }
        return result;

    }

    /**
     * get latest version , assuming version numbering increases the lexical location
     *
     * @param path      where to find the files
     * @param startwith common start string for all files.
     * @return
     */
    public String getLatestVersion(String path, String startwith) {
        String result = null;
        if (exists(path)) {
            File folder = new File(path);
            File[] files = folder.listFiles();
            for (File file : files) {
                if (!file.getName().startsWith(startwith)) continue;
                if (result == null) {
                    result = file.getAbsolutePath();
                } else {
                    if (file.getAbsolutePath().compareTo(result) > 0) {
                        result = file.getAbsolutePath();
                    }
                }
            }
        }
        return result;
    }

    private boolean contWithProcess(Process process, String toFind, boolean notToFind, String ownerName) {
        try {
            boolean show = false; //supress output even if updating linux
            StreamGobbler errorGobbler = new
                    StreamGobbler(process.getErrorStream(), "ERROR", context.getLogger(), show);

            StreamGobbler outputGobbler = new
                    StreamGobbler(process.getInputStream(), "OUTPUT", context.getLogger(), show);

            errorGobbler.start();
            outputGobbler.start();
            info("Process is: " + process.pid());
            int exitVal = process.waitFor();
            info(ownerName + ", Exit Val for script :" + exitVal);
            errorLines.clear();
            errorLines.addAll(errorGobbler.getLines());
            lines.clear();
            lines.addAll(outputGobbler.getLines()); //for debugging purposes
            debuglines(ownerName, exitVal != 0); //write debug lines when exit val is not zero
            if (!isWindows && exitVal == 4) {
                return false; //seems to be the response when looking for a non-existent service. TODO:make sure that this is the case
            } else {
                if (exitVal == 1603) {
                    severe("Installation should run with administrative rights");
                }
            }
            if (toFind == null || toFind.isEmpty()) return exitVal == 0;
            if (notToFind) {

                return (exitVal == 0 && !(outputGobbler.dryString(toFind) || errorGobbler.dryString(toFind)));
            } else {
                boolean r = outputGobbler.dryString(toFind);
                if (!r) r = errorGobbler.dryString(toFind); //pg_dump sends to error gobbler ....
                return ((exitVal == 0) && r);
            }

        } catch (InterruptedException e) {

            error("Error while executing command :  ownername: " + ownerName, e);
            return false;
        }
    }

    public String flexicoreSource;
    public String flexicoreHome;
    public boolean force;
    public boolean dry;
    public boolean update;
    public String phase = "";

    /**
     * called before the installation allowing tasks to inform on previous installations, removals etc.
     *
     * @param context
     * @return
     */
    @Override
    public InspectionResult inspect(InstallationContext context) {
        return new InspectionResult().setInspectionState(InspectionState.NOT_FOUND).setSkip(true);

    }

    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        context = installationContext;
        flexicoreSource = getServerPath() + "flexicore";
        flexicoreHome = getFlexicoreHome();
        dry = getContext().getParamaters().getBooleanValue("dry");
        update = getContext().getParamaters().getBooleanValue("update");
        force = getContext().getParamaters().getBooleanValue("force");
        if (dry) {
            info("Dry run  of " + this.getId() + " -> " + this.getDescription() + getParameters(installationContext).toString());
            return new InstallationResult().setInstallationStatus(InstallationStatus.ISDRY);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public InstallationResult unInstall(InstallationContext installationContext) throws Throwable {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public InstallationResult update(InstallationContext installationContext) throws Throwable {
        return new InstallationResult().setInstallationStatus(InstallationStatus.CONTINUE);
    }

    /**
     * this is optionally handled by an installation plugin once all plugins have been installed. can be used for restarting a service
     *
     * @param installationContext
     * @return
     * @throws Throwable
     */
    @Override
    public InstallationResult finalizeInstallation(InstallationContext installationContext) throws Throwable {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public InstallationResult restartService(InstallationContext installationContext) throws Throwable {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    /**
     * create a service for uodating running services (makes sense only if there is a UI available)
     *
     * @param running
     * @return
     */
    public Service getNewService(boolean running) {
        Service service = new Service().
                setServiceId(getId()).
                setName(getName()).
                setDescription(getDescription()).
                setRunning(running);
        if (running) {
            service.setRunningFrom(LocalDateTime.now()).setLastChecked(LocalDateTime.now());
        }
        return service;
    }

    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }

    public boolean setAdmin() {

        if (!admin) {
            if (isWindows()) {
                String path = getScriptsPath() + "/setadmin.bat";
                if (new File(path).exists()) {
                    try {
                        if (executeCommandByBuilder(new String[]{path}, "", false, "", false)) {
                            admin = true;

                        }
                    } catch (IOException e) {
                        severe("Error while setting to administrator", e);
                        return false;
                    }
                } else {
                    updateProgress(context, "Cannot find script for elevation request at: " + path);
                    severe("Cannot find elevation script at: " + path);
                }
            }
        }
        return admin;
    }

    public void updateProgress(InstallationContext context, String message) {
        this.setMessage(message);
        context.getInstallerProgress().installationProgress(this, context);
        info(message);
    }


    public boolean updateService(InstallationContext context, Service service, IInstallationTask task) {

        return context.getUpdateService().serviceProgress(context, service, this);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public IInstallationTask setOrder(int order) {
        this.order = order;
        return this;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public IInstallationTask setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public IInstallationTask setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    /**
     * if true, will be sorted as if all other plugins are pre-requisites, effectively put finalizers last in the list
     * finalizers order is not fixed and it is assumed that these need to run last.
     *
     * @return
     */
    @Override
    public boolean isFinalizerOnly() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IInstallationTask setDescription(String description) {
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public IInstallationTask setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        return Collections.emptySet();
    }

    /**
     * list of ids we need to restart.
     *
     * @return
     */
    @Override
    public Set<String> getNeedRestartTasks() {
        return Collections.emptySet();
    }

    /**
     * set of installation tasks that must be installed before this task if exist.
     *
     * @return
     */
    @Override
    public Set<String> getSoftPrerequisitesTask() {
        return Collections.emptySet();
    } //todo:: check if this is the best solution


    @Override
    public InstallationStatus getStatus() {
        return status;
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        return new Parameters();
    }

    @Override
    public int mergeParameters(InstallationContext installationContext) {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public IInstallationTask setEnabled(boolean value) {
        enabled = value;
        return this;
    }

    @Override
    public boolean isStop() {
        return false;
    }

    @Override
    public IInstallationTask setSTop(boolean value) {
        this.stop = value;
        return this;
    }

    @Override
    public boolean isSnooper() {
        return false;
    }

    /**
     * called when installation is beyond finalizing.
     *
     * @return
     */
    @Override
    public boolean cleanup() {
        return false;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public IInstallationTask setService(Service service) {
        this.service = service;
        return this;
    }

    @Override
    public LocalDateTime getStarted() {
        return started;
    }

    @Override
    public boolean initialize(InstallationContext context) {
        setContext(context);
        return true;
    }

    @Override
    public boolean refreshData(InstallationContext context, Parameter parameter) {
        return false;
    }

    @Override
    public void setSubscribers(InstallationContext installationContext) {

    }

    @Override
    public void parameterChanged(Parameter parameter) {

    }

    @Override
    public LocalDateTime getEnded() {
        return ended;
    }

    @Override
    public Integer getProgress() {
        return progress;
    }

    @Override
    public InstallationTask setStarted(LocalDateTime started) {
        this.started = started;
        return this;
    }

    @Override
    public boolean isWrongOS() {
        return wrongOS;
    }

    @Override
    public InstallationTask setEnded(LocalDateTime ended) {
        this.ended = ended;
        return this;
    }

    @Override
    public IInstallationTask setProgress(Integer progress) {
        this.progress = progress;
        return this;
    }

    @Override
    public IInstallationTask setStatus(InstallationStatus status) {
        this.status = status;
        return this;
    }


    @Override
    public IInstallationTask setName(String name) {
        this.name = name;
        return this;
    }


    public InstallationContext getContext() {
        return context;
    }

    @Override
    public InstallationTask setContext(InstallationContext context) {
        this.context = context;
        return this;
    }

    /**
     * delete a directory
     *
     * @param path
     * @throws IOException
     */
    public static void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * delete the content of a folder.
     *
     * @param path
     * @return number of files and folders deleted (not recursively)
     */
    public int deleteDirectoryContent(String path) {
        if (exists(path)) {
            File[] files = new File(path).listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    try {
                        deleteDirectoryStream(Paths.get(file.getAbsolutePath()));
                    } catch (IOException e) {
                        severe("Error while deleting folder: " + file.getAbsolutePath(), e);
                    }
                } else {
                    try {
                        Files.deleteIfExists(Paths.get(file.getAbsolutePath()));
                    } catch (IOException e) {
                        severe("Error while deleting file: " + file.getAbsolutePath(), e);
                    }
                }
            }
            return files.length;
        } else return -1;
    }
//todo:regex doesn't work here

    /**
     * delete with wildcard including folders, folders will be deleted completely if name matches
     *
     * @param path
     * @param regex   make sure regex can be used
     * @param context
     * @return
     */
    public static int deleteFilesByPattern(String path, String regex, InstallationContext context) {

        if (isPatternValid(regex)) {
            File folder = new File(path);
            if (folder.exists()) {
                final Pattern p = Pattern.compile(regex);
                File[] files = folder.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return p.matcher(f.getName()).matches();
                    }
                });
                int i = 0;
                if (files.length == 0) return -2;
                for (final File file : files) {
                    if (file.isDirectory()) {
                        try {
                            deleteDirectoryStream(file.getAbsolutePath());
                            i++;
                        } catch (IOException e) {
                            context.getLogger().severe("Failed to delete folder: " + e.toString());
                        }
                    } else {
                        if (!file.delete()) {
                            System.err.println("Can't remove " + file.getAbsolutePath());
                        } else i++;
                    }
                }
                return i;
            } else return -1;
        } else return -2;
    }

    /**
     * check if regex pattern is valid
     *
     * @param pattern
     * @return
     */
    public static boolean isPatternValid(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException exception) {

        }
        return false;
    }

    public void setOwnerFolder(Path path, String userName, String group) throws IOException {
        if (!isWindows()) {
            info("Setting owner on path: " + path);
            List<Path> list = Files.walk(path).collect(Collectors.toList());
            for (Path thePath : list) {
                if (!setOwner(userName, group, thePath)) {
                    info("Cannot set owner on file: " + thePath);
                    break;
                }
            }
        } else {
            info("Setting owner on windows is not supported: " + path);
        }

    }

    public static boolean deleteDirectoryStream(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            deleteDirectoryStream(file.toPath());
            return true;
        }

        return false;
    }

    public static void touch(File file) throws IOException {
        long timestamp = System.currentTimeMillis();
        touch(file, timestamp);
    }

    public static void touch(File file, long timestamp) throws IOException {
        if (!file.exists()) {
            new FileOutputStream(file).close();
        }

        file.setLastModified(timestamp);
    }

    /**
     * make sure a target folder is created if it doesn't exist
     *
     * @param targetDir
     */
    public void ensureTarget(String targetDir) {
        File target = new File(targetDir);
        if (!target.exists()) {
            target.mkdirs();
            info("Folder : " + target.getAbsolutePath() + " was created");
        } else {
            info("folder :" + target.getAbsolutePath() + " already exists");
        }
    }

    public boolean move(String source, String target) {
        boolean result = false;
        try {
            if (isWindows) {
                result = executeCommandByBuilder(new String[]{"cmd", "/C", "move", source, target},
                        "", false, "move server sources", false);
            }
        } catch (IOException e) {
            severe("Error while moving server sources", e);
        }

        return result;
    }

    public void simpleMessage(String owner, String info, String message) {
        switch (info) {
            case "severe":
                severe("Phase: " + owner + " " + message);
                break;
            default:
                info("Phase: " + owner + " " + message);

        }
    }

    /**
     * added few methods for accessing common parameters although these are not known to the Installer.
     * These parameters are common in every FC installation.
     */

    public String getTargetPath() {
        return getContext().getParamaters().getValue("targetpath");

    }


    public String getServerPath() {
        return getContext().getParamaters().getValue("serverpath") + "/";
    }


    public String getInstallationsPath() {
        return getContext().getParamaters().getValue("installationspath") + "/";

    }

    public boolean isExtraLogs() {
        return getContext().getParamaters().getBooleanValue("extralogs");

    }

    public boolean isHelpRunning() {
        return getContext().getParamaters().getBooleanValue("h");

    }

    public String getScriptsPath() {
        return getContext().getParamaters().getValue("scriptspath") + "/";

    }

    public String getServicesPath() {
        return getContext().getParamaters().getValue("servicespath") + "/";

    }

    public String getIoTPath() {
        String value = context.getParameter("iotpath").getValue();
        return value + "/";

    }

    /**
     * copy a single file.
     *
     * @param source
     * @param target
     * @return
     * @throws IOException
     */
    public boolean copySingleFile(String source, String target) throws IOException {
        if (exists(source)) {
            Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        return false;
    }

    /**
     * recursively copy folders
     *
     * @param installationDir source of the copy
     * @param targetDir       target of the copy
     * @param ownerName       this is for logging purposes only
     * @return
     * @throws InterruptedException
     */
    public boolean copy(String installationDir, String targetDir, String ownerName) throws InterruptedException {
        info("copying" + ownerName + " from: " + installationDir + " to: " + targetDir);
        addMessage("application server-Sanity", "info", "starting parameters sanity check");

        Path targetPath = Paths.get(targetDir);
        Path sourcePath = Paths.get(installationDir);
        File sourceFile = new File(installationDir);
        if (sourceFile.exists()) {
            if (!dry) {
                ensureTarget(targetDir);
                try {
                    CopyFileVisitor copyFileVisitor = null;
                    if (!dry) {
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setContext(getContext()).setCopyOver(true));
                    }
                    addMessage(ownerName + " copying server", "info", "copy finished " + ((copyFileVisitor == null) ? "" : ((copyFileVisitor.getCount() + "  files copied" + (copyFileVisitor.getErrors() == 0 ? "" : "  Errors: " + copyFileVisitor.getErrors())))));
                    copyFileVisitor.clear();


                } catch (IOException e) {
                    error("Error while moving " + ownerName + "  :", e);
                    return false;
                }
                info("Have copied " + ownerName + " from :" + sourcePath + " to: " + targetPath);
                addMessage("application server-closing", "info", "done");
            } else {
                addMessage("application server-closing", "info", "done, dry run in effect");
            }
        } else {
            addMessage("application server-sanity", "error", "source server files cannot be found");
            severe("Cannot move " + ownerName + ", path: " + sourcePath + " cannot be found");
        }


        return true;
    }

    private String addToLast(String add, String path) {
        path.replace("\\", "/");
        String[] result = path.split("/");
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String component : result) {
            builder.append(i++ == 0 ? "" : "/");
            if (i == result.length) {

                builder.append(add);

                builder.append(component);
                break;
            } else {

                builder.append(component);
            }

        }
        return builder.toString();
    }

    /**
     * make a bacukp of all files in a folder.
     *
     * @param path
     * @return
     */
    public boolean zipAll(String path, String zipPath, InstallationContext context) throws IOException {
        File zipFile = new File(zipPath);
        if (zipFile.exists()) {
            Files.move(zipFile.toPath(), Paths.get(addToLast("old-", zipPath)), StandardCopyOption.REPLACE_EXISTING);
        }
        List<File> files = new ArrayList<>();
        if ((new File(path)).exists()) {
            files = Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile).filter(f -> !(f.getFileName().toString().endsWith("zip"))).map(f -> f.toFile()).collect(Collectors.toList());
            File[] all = new File[files.size()];
            files.toArray(all);
            zipFiles(all, new File(zipPath));
            return true;
        }
        return false;
    }

    public boolean zipFolder(String foldername, String output) {
        File parent;
        if (!(parent = new File(output).getParentFile()).exists()) {
            parent.mkdirs();
        }
        return FolderCompression.zipFolder(foldername, output, getContext().getLogger());

    }

    public boolean zipFiles(File[] files, File zipFile) {
        ZipUtil.packEntries(files, zipFile);
        return true;
    }

    /**
     * returns a path (if a folder) with list of folders and files
     *
     * @param path
     * @return
     */
    public Pair<List<String>, List<String>> getComponents(String path, String excludeTtrailer) {
        File source = null;
        List<String> files = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        if ((source = new File(path)).exists()) {
            if (source.isDirectory()) {
                for (File file : source.listFiles()) {
                    int index = file.getAbsolutePath().lastIndexOf("/") != -1 ? file.getAbsolutePath().lastIndexOf("/") : file.getAbsolutePath().lastIndexOf("\\");

                    if (index != -1) {

                        if (file.isDirectory()) {
                            folders.add(file.getAbsolutePath().substring(index + 1));
                        } else {
                            if (excludeTtrailer == null) {
                                files.add(file.getAbsolutePath().substring(index + 1));
                            } else {
                                if (!file.getAbsolutePath().endsWith(excludeTtrailer)) {
                                    files.add(file.getAbsolutePath().substring(index + 1));
                                }

                            }
                        }
                    }
                }
            }
        }
        return new ImmutablePair<>(folders, files);

    }

    /**
     * recursively copy folders
     *
     * @param installationDir copy source folder
     * @param targetDir       copy target folder
     * @param context
     * @return
     * @throws InterruptedException
     */
    public boolean copy(String installationDir, String targetDir, InstallationContext context) throws InterruptedException {

        addMessage("application server-Sanity", "info", "starting parameters sanity check");
        File target = new File(targetDir);
        Path targetPath = Paths.get(targetDir);
        Path sourcePath = Paths.get(installationDir);
        File sourceFile = new File(installationDir);
        if (sourceFile.exists()) {
            if (!dry) {
                if (!target.exists()) {
                    target.mkdirs();
                    info("Folder : " + targetPath + " was created");
                } else {
                    info("folder :" + targetPath + " already exists");
                }
                try {
                    CopyFileVisitor copyFileVisitor = null;

                    if (!dry) {
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setContext(getContext()).setCopyOver(true));
                    }


                } catch (IOException e) {
                    return false;
                }
                addMessage("application server-closing", "info", "done");
            } else {
                addMessage("application server-closing", "info", "done, dry run in effect");
            }
        } else {
            addMessage("application server-sanity", "error", "source server files cannot be found");
        }


        return true;
    }

    //https://github.com/zeroturnaround/zt-zip
    public boolean zip(String zipFolderName, String zipFileName, InstallationContext context) {
        File zipFile = null;
        File sourceFile = new File(zipFolderName);
        if (sourceFile.exists()) {
            try {
                ZipUtil.pack(new File(zipFolderName), zipFile = new File(zipFileName));
            } catch (Exception e) {
                severe("Error while zipping folder: " + zipFolderName, e);
            }
            if (zipFile.exists()) {
                context.getLogger().info(" Have zipped " + zipFolderName + " into: " + zipFileName);
                return true;
            } else {
                context.getLogger().log(Level.SEVERE, " Have failed to zip  " + zipFolderName + " into: " + zipFileName);

            }
        } else {
            context.getLogger().log(Level.SEVERE, " Have not found zip source folder  " + zipFolderName);
        }
        return false;

    }

    /**
     * @param firstFile
     * @param secondFile
     * @param resultFile result
     * @return
     */
    public boolean mergeFiles(String firstFile, String secondFile, String resultFile, String separator) throws IOException {
        List<String> data1 = new ArrayList<>();
        List<String> data2 = new ArrayList<>();
        InputStream is1 = null;
        InputStream is2 = null;
        FileOutputStream fos = null;
        boolean result = false;
        try {
            is1 = new FileInputStream(firstFile);
            is2 = new FileInputStream(secondFile);
            BufferedReader buf1 = new BufferedReader(new InputStreamReader(is1));
            BufferedReader buf2 = new BufferedReader(new InputStreamReader(is2));
            String line1 = buf1.readLine();

            while (line1 != null) {
                data1.add(line1);
                line1 = buf1.readLine();
            }
            is1.close();
            ;
            is1 = null;
            String line2 = buf2.readLine();
            while (line2 != null) {
                data2.add(line2);
                line2 = buf2.readLine();
            }
            is2.close();
            is2 = null;
            //remove lines in first file if a line from list 2 exists or a key from list 2 exists
            List<String> toRemove = new ArrayList<>();
            List<String> keysin2 = new ArrayList<>();
            for (String in2 : data2) {
                String split[] = in2.split(separator);
                if (split.length > 0) {
                    keysin2.add(split[0]);
                }
            }
            for (String in1 : data1) {
                if (data2.contains(in1)) {
                    toRemove.add(in1);
                } else {
                    String[] split = in1.split(separator);
                    if (split.length > 0) {
                        if (keysin2.contains(split[0])) toRemove.add(in1);
                    }
                }
            }
            for (String tr : toRemove) {
                data1.remove(tr);
            }
            data1.addAll(data2);
            fos = new FileOutputStream(new File(resultFile));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String line : data1) {
                bw.write(line);
                bw.newLine();
            }
            fos.close();
            result = true;
        } catch (FileNotFoundException e) {
            severe("Error while merging files", e);
        } catch (IOException e) {
            severe("Error while merging files", e);
        } finally {
            if (is1 != null) is1.close();
            if (is2 != null) is2.close();
            if (fos != null) fos.close();
            return result;
        }
    }

    /**
     * assumes that configuration files are built with the name of the parameter as the placeholder for the value, simplifies code
     *
     * @param path
     * @param id
     * @return
     */
    public boolean editFile(String path, String id) {
        IInstallationTask task = null;
        try {

            Collection<Parameter> parameters = (task = getContext().getiInstallationTasks().get(id)).getParameters(getContext()).getValues();
            String previousContent = "";
            int i = parameters.size();
            for (Parameter parameter : parameters) {
                previousContent = editFile(path, previousContent, parameter.getName(), parameter.getValue(), false, false, --i == 0, false);

            }
        } catch (Exception e) {
            if (task == null) {
                severe("Could not dry a task with id: " + id);
            }
        }
        return true;
    }

    public boolean addToPath(String path, String owner) {
        boolean result = false;
        if (isWindows()) {
            if (exists(path)) {
                result = executeCommandByRuntime("setx path  \"" + "%PATH%;" + path + "\"", owner);
                if (result) {
                    info("have added " + path + " to path variable ");
                }
            }
        }
        return result;
    }

    /**
     * Add line to existing file, will not add if already there or if cannot find the location after specified file
     *
     * @param path           path to file
     * @param existingString allow editing on a string so file operations reduced
     * @param afterLine      location after line
     * @param line           the line to insert
     * @param warning
     * @param reverseSlash   fix windows backslash
     * @param close          close the file.
     * @return null or the changed string to be used on subsequent similar actions
     */
    public String addLine(String path, String existingString, String afterLine,
                          String line, boolean warning, boolean reverseSlash, boolean close) {
        String fileAsString = existingString;
        if (existingString == null || existingString.isEmpty()) {
            fileAsString = getFile(path);
        }
        if (!fileAsString.contains(line)) {
            if (fileAsString.contains(afterLine)) {
                fileAsString = fileAsString.replaceAll(afterLine, afterLine + "\n" + line);
            } else {
                info("[addLine] cannot find the preceding line!!");
            }
        } else {
            info("[addLine] no need to add line, it exists");

        }
        if (isWindows() && reverseSlash) {
            fileAsString = fixWindows(fileAsString);
        }
        if (close) {
            writeFile(path, fileAsString);
        }
        // info("[addline file] [addline file] ->" + fileAsString);
        return fileAsString;
    }

    /**
     * @param path
     * @param existingString from a previous call, saves time in open/close file sequence, all stages but one in memory
     * @param toFind
     * @param toReplace
     * @param warning
     * @param reverseSlash
     * @param close          , if false returns the content as string to be passed to a next call in existingString
     * @param allowRepeat,   if replacing string already there do no generate an error
     * @return
     */
    public String editFile(String path, String existingString, String toFind,
                           String toReplace, boolean warning, boolean reverseSlash, boolean close, boolean allowRepeat) {
        if (!new File(path).exists()) {
            if (warning) {
                addMessage("Edit file", "severe", "Cannot open the file: " + path + " for editing");
            }
            return null;
        }
        info("[Edit file] file " + path + " exists");
        toReplace = toReplace.replace("\\", "/");
        toFind = toFind.replace("\\", "/");
        String fileAsString = existingString;
        if (existingString == null || existingString.isEmpty()) {
            fileAsString = getFile(path);
        }
        if (fileAsString != null) {
            info("[Edit file] [Edit file]  file as string is not null");
            if (!fileAsString.contains(toFind) && !allowRepeat) {
                //info("[Edit file] [Edit file]  file as string doesn't contain: " + toFind);
                return null;
            }
            fileAsString = fileAsString.replaceAll(toFind, toReplace);
            if (isWindows() && reverseSlash) {
                fileAsString = fixWindows(fileAsString);
            }
        }
        if (close) {
            writeFile(path, fileAsString);
            return "";
        }
        // info("[Edit file] [Edit file] ->" + fileAsString);
        return fileAsString;
    }

    private boolean writeFile(String path, String fileAsString) {
        try {
            Files.write(Paths.get(path), fileAsString.getBytes());
            return true;
        } catch (IOException e) {
            severe("Error while writing file", e);
        }
        return false;
    }

    private String fixWindows(String fileAsString) {
        info("Fixing Windows backslash");
        fileAsString = fileAsString.replaceAll("/", "\\\\");
        String lines[] = fileAsString.split("\n");
        boolean replaced = false;
        String[] newLines = new String[lines.length];
        int i = 0;
        for (String line : lines) {
            String eq[] = line.split("=");
            if (eq.length == 2) {
                if (eq[1].trim().startsWith("\\")) {
                    line = eq[0] + "=" + "c:" + eq[1];
                    replaced = true;
                }
            }
            newLines[i++] = line;
        }
        if (replaced) {
            StringBuilder sb = new StringBuilder();
            for (String line : newLines) {
                sb.append(line).append("\n");
            }
            fileAsString = sb.toString();
        }
        return fileAsString;
    }

    public String getFile(String path) {
        String result;
        try {

            FileInputStream is = new FileInputStream(path);

            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }
            result = sb.toString();
            if (result.length() == 0) {
                is.close();
                severe("[Edit file]  file was empty");
                return null;
            }


            is.close();
        } catch (IOException e) {
            severe("Error while reading file", e);
            return null;
        }
        return result;
    }

    /**
     * change a property in a Java compatible properties file
     *
     * @param path
     * @param key
     * @param value
     * @return
     */
    public boolean editPorperties(String path, String key, String value) {
        Properties properties = new Properties();
        if (exists(path)) {
            try {
                FileInputStream is = new FileInputStream(path);
                properties.load(is);
                is.close();
                if (!properties.get(key).equals(value)) {
                    properties.setProperty(key, value);
                    FileWriter fileWriter = new FileWriter(path);
                    properties.store(fileWriter, "Written on: " + LocalDateTime.now());
                    fileWriter.close();

                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    @Override
    public InstallationTask setVersion(String version) {
        this.version = version;
        return this;
    }

    public boolean setOwner(String userName, String groupName, Path path) {
        Pair<GroupPrincipal, UserPrincipal> pair = getGroupUser(groupName, userName);
        if (pair != null) {
            try {
                if (setOwner(path, pair.getRight(), pair.getLeft())) {
                    info("Have set owner of path: " + path + " to group: " + groupName + " user: " + userName);
                    return true;
                }
            } catch (IOException e) {
                severe("Have failed to set owner", e);
            }
        }
        return false;
    }

    public Pair<GroupPrincipal, UserPrincipal> getGroupUser(String groupName, String userName) {
        UserPrincipalLookupService lookupService =
                FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal user = null;
        try {
            user = lookupService.lookupPrincipalByName(userName);
        } catch (IOException e) {
            // severe("Cannot get user named: " + userName, e);
        }
        try {
            if (user != null) {
                GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupName);
                Pair<GroupPrincipal, UserPrincipal> result = new ImmutablePair<>(group, user);
                return result;
            }

        } catch (IOException e) {
            //severe("Cannot get  group named: " + groupName, e);
        }
        return null;

    }

    private boolean setOwner(Path path, UserPrincipal user, GroupPrincipal group) throws IOException {

        if (group != null && user != null) {
            Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
            Files.setOwner(path, user);
            return true;
        }
        return false;
    }

    public boolean startEnableService(String serviceName) {
        if (executeCommand("systemctl daemon-reload", "", "Reloading services")) {

            if (executeCommand("systemctl enable " + serviceName, "", " enabling " + serviceName)) {
                simpleMessage("service " + serviceName, "info", "enabled service");
                if (testServiceRunning(serviceName, "installing " + serviceName, true)) {
                    executeCommand("service " + serviceName + "stop", "", " stopping " + serviceName);
                }
                if (executeCommand("service " + serviceName + " start", "", " starting " + serviceName)) {
                    simpleMessage(serviceName, "info", "have started service " + serviceName);
                    if (testServiceRunning(serviceName, serviceName + " installation", true)) {
                        simpleMessage(serviceName, "info", serviceName + " started");
                        return true;
                    } else {
                        simpleMessage(serviceName, "severe", serviceName + " service is not running");
                    }

                } else {
                    simpleMessage(serviceName, "severe", serviceName + " starting service ");
                }
            } else {
                simpleMessage(serviceName, "severe", serviceName + "enabling service failed");
            }
        } else {
            simpleMessage(serviceName, "severe", serviceName + " cannot reload daemon");
        }
        return false;
    }

    /**
     * set Windows service dependencies, service will not start unless the dependson are running
     *
     * @param serviceName
     * @param dependsOn
     * @param ownerName
     * @return
     */
    public boolean setServiceDependencies(String serviceName, String[] dependsOn, String ownerName) {
        if (isWindows()) {


            String services = null;
            for (String service : dependsOn) {
                if (services == null) {
                    services = service;
                } else {
                    services += "/" + service;
                }
            }
            info("Setting server dependencies for service name:" + serviceName + " depends on" + services);
            if (services == null || services.isEmpty()) return false;

            return executeCommand("sc config " + serviceName + " depend= " + services, "success", " Setting dependencies on: " + ownerName);
        } else return true;


    }

    /**
     * @param pathtoMSI full path to msi
     * @param options   any of: [/quiet][/passive][/q{n|b|r|f}]
     * @return
     */
    public boolean installMSI(String pathtoMSI, String... options) {
        if (isWindows) {
            pathtoMSI = pathtoMSI.replace("/", "\\"); //MSI will not be installed with "/"
            Runtime rf = Runtime.getRuntime();
            StringBuilder builder = new StringBuilder();
            for (String option : options) {
                builder.append(option);
                builder.append(" ");
            }
            try {
                Process pf = rf.exec("msiexec /i \"\\" + pathtoMSI + "\"" + " " + builder.toString());
                return true;

            } catch (IOException e) {
                severe("Exception while running MSI ");

            }
        }
        return false;
    }

    /**
     * these are not really needed here
     *
     * @return
     */
    public String getFlexicoreHome() {
        return getContext().getParamaters().getValue("flexicorehome") + "/";

    }

    public String getWildflyHome() {
        Parameter parameter = getContext().getParameter("wildflyhome");
        if (parameter != null) return parameter.getValue() + "/";
        if (isLinux) return "/opt/wildfly/";
        return null;

    }

    public String getStandalone() {
        return getWildflyHome() + "standalone";
    }

    public String getDeployments() {
        return getStandalone() + "/deployments";
    }

    public String getStandaloneConfiguration() {
        return getWildflyHome() + "standalone/configuration/standalone.xml";
    }

    public String getWildflySource() {
        return getContext().getParamaters().getValue("wildflysourcepath");

    }

    public int getFilesCopied() {
        return filesCopied;
    }

    public InstallationTask setFilesCopied(int filesCopied) {
        this.filesCopied = filesCopied;
        return this;
    }

    public int getFilesFailed() {
        return filesFailed;
    }

    public InstallationTask setFilesFailed(int filesFailed) {
        this.filesFailed = filesFailed;
        return this;
    }

    public int getFoldersCopied() {
        return foldersCopied;
    }

    public InstallationTask setFoldersCopied(int foldersCopied) {
        this.foldersCopied = foldersCopied;
        return this;
    }

    public int getFoldersFailed() {
        return foldersFailed;
    }

    public InstallationTask setFoldersFailed(int foldersFailed) {
        this.foldersFailed = foldersFailed;
        return this;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public InstallationTask setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public int getTotalFolders() {
        return totalFolders;
    }

    public InstallationTask setTotalFolders(int totalFolders) {
        this.totalFolders = totalFolders;
        return this;
    }

    public boolean isProgressOnFolders() {
        return progressOnFolders;
    }

    public InstallationTask setProgressOnFolders(boolean progressOnFolders) {
        this.progressOnFolders = progressOnFolders;
        return this;
    }

    /**
     * this is to help in using editfile, may be redundant, no need to reverse slash
     *
     * @param path
     * @return
     */
    public boolean fixFlexicoreConfig(String path) {
        return editFile(path, "", "/home/flexicore", flexicoreHome, false, false, true, false) != null;
    }

    public String getJavaVersion(String ownerName) {
        String[] args = {"java", "-version"};
        try {
            if (executeCommandByBuilder(args, "", false, ownerName, false)) {
                ArrayList<String> list = new ArrayList(errorLines);
                for (String line : list) {
                    String[] split = line.split("version");
                    if (split.length == 2) {
                        return split[1];
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static final Random RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generatePassword(int length) {
        StringBuilder returnValue = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            returnValue.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new String(returnValue);
    }

    /**
     * change "/" to reverse slash, assume C drive (todo:fix to any drive);
     *
     * @param path
     * @param existingString
     * @param close
     * @return
     */
    public String fixWindowsPath(String path, String existingString, boolean close) {
        String fileAsString = null;
        if (existingString == null || existingString.isEmpty()) {
            fileAsString = getFile(path);
        }
        if (fileAsString != null) {
            String[] lines = fileAsString.split("\n");
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                String[] split = line.split(":");
                if (split.length == 2) {
                    split[1] = split[1].trim();
                    if (split[1].startsWith("\"/")) {
                        split[1] = split[1].replace("\"/", "c:\\");
                        split[1] = split[1].replace("//", "/");
                        split[1] = split[1].replace("/", "\\");
                        split[1] = split[1].replace("\\", "\\\\");
                        line = split[0] + ": " + split[1];
                    }
                }
                // System.out.println(line);

                result.append(line).append("\n");
            }
            fileAsString = result.toString();
            if (close) {
                if (writeFile(path, fileAsString)) {

                    return fileAsString;
                }
            }
            return fileAsString;
        }
        return "";

    }

    public void incrementFiles() {
        filesCopied++;
    }

    public void incrementFilesErrors() {
        filesFailed++;
    }

    public void incrementFolders() {
        foldersCopied++;
    }

    public void incrementFoldersFailures() {
        foldersFailed++;
    }

    public enum WindowsVersion {
        xp, vista, seven, eight, ten
    }


    public InstallationTask setWrongOS(boolean wrongOS) {
        this.wrongOS = wrongOS;
        return this;
    }

    public static WindowsVersion getWindowsVersion() {
        if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("xp")) {
            return WindowsVersion.xp;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("vista")) {
            return WindowsVersion.vista;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("7")) {
            return WindowsVersion.seven;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("8")) {
            return WindowsVersion.eight;
        } else return WindowsVersion.ten;
    }

}


