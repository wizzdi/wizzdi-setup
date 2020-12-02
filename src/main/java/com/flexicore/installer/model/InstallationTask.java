package com.flexicore.installer.model;


import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.FolderCompression;
import com.flexicore.installer.utilities.StreamGobbler;
import com.wizzdi.installer.*;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.fusesource.jansi.Ansi;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@SuppressWarnings("LossyEncoding")
public class InstallationTask implements IInstallationTask {

    private LocalDateTime started;
    private LocalDateTime ended;
    private Integer progress = 0;
    private String name = "Unnamed task";
    private String id;
    private String version = "1.0.0";
    private boolean enabled = true;
    private boolean wrongOS = false;
    private boolean completedReported;
    private boolean finalizerReported;


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
    private static ProcessorType processorType = new ProcessorType();
    final public static boolean isLinux = SystemUtils.IS_OS_LINUX;
    final public static boolean isMac = SystemUtils.IS_OS_MAC;
    final public static boolean is64 = System.getProperty("sun.arch.data.model").equals("64");
    Queue<String> lines = new ConcurrentLinkedQueue<String>();
    Queue<String> errorLines = new ConcurrentLinkedQueue<String>();

    public static ProcessorType getProcessorType() {
        return processorType;
    }

    public static void setProcessorType(ProcessorType processorType) {
        InstallationTask.processorType = processorType;
    }

    public boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public InstallationResult failed() {
        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
    }

    public InstallationResult succeeded() {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public OperatingSystem getCurrentOperatingSystem() {
        if (isWindows) return OperatingSystem.Windows;
        if (isLinux) return OperatingSystem.Linux;
        if (isMac) return OperatingSystem.OSX;
        return OperatingSystem.Linux;
    }

    public boolean stopWildfly(String ownerName, long wait) {
        long started = System.currentTimeMillis();
        if (isWindows()) {
            started = System.currentTimeMillis();
            forceKillService("java");
            info("Stopped wildlfy by force  in " + (System.currentTimeMillis() - started));
            info("Killed wildfly by PowerShell command");
            return true;
        }
        if (testServiceRunning("wildfly", ownerName, false)) {

            if (!waitForServiceToStop("wildfly", ownerName, true, wait)) {
                severe("Was not able to stop service Wildfly");

            } else {
                info("Stopped wildlfy by PS in " + (System.currentTimeMillis() - started));
            }
        } else {
            info("Wildfly service was not running, no need to stop it.");
            return true;
        }
        return false;
    }

    private String getValueByKey(PowerShellResponse response, String key) {
        if (response.isError() || response.isTimeout()) return "";

        String[] result = response.getCommandOutput().split("\n");
        for (String line : result) {
            if (line.contains(key)) {
                return line.split(":")[1];
            }
        }
        return "";
    }

    public int getNumberOfLogicalProcessor() {
        int result = -2;
        PowerShellReturn response = executePowerShellCommand("(Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors", null);
        try {
            if (response.getErrorList().size() != 0 || response.isError()) {
                if (response.isTimeout()) severe("Time out in getting processor number of logical cores");
                if (response.isError()) severe("error in getting processor number of logical cores");

                return -1;
            } else {
                info("response for getting logical processors number:\n" + response.getCommandOutput());
            }

            try {
                result = Integer.parseInt(response.getCommandOutput());
            } catch (NumberFormatException e) {
                severe("Error while parsing logical cores", e);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return result;

    }

    public ProcessorType getPorcessorType() {
        ProcessorType processorType = new ProcessorType();
        PowerShellReturn response = executePowerShellCommand("Get-WmiObject Win32_Processor", null);
        if (response.isTimeout() || response.isError()) {

            if (response.isTimeout()) severe("Time out in getting processor type");
            if (response.isError()) severe("error in getting processor type");
            return processorType;

        } else {
            info("response for getting processor type:\n" + response.getCommandOutput());
        }

        processorType.populate(response.getCommandOutput());
        return processorType;
    }

    public ProcessorData getProcessorData() {
        PowerShellReturn response = executePowerShellCommand(
                "Get-CimInstance -ClassName 'Win32_Processor'   | Select-Object -Property 'DeviceID', 'Name', 'NumberOfCores'", null);
        ProcessorData processorData = new ProcessorData();

        try {
            if (response.isTimeout() || response.isError()) {
                if (response.isTimeout()) severe("Time out in getting processor data");
                if (response.isError()) severe("error in getting processor data");
                return processorData;
            } else {
                info("response for getting processor data:\n" + response.getCommandOutput());
            }

            String[] lines = response.getCommandOutput().split("\n");
            // info("Get-CimInstance -ClassName 'Win32_Processor'  ");
            String theLine = lines[3];
            for (String line : lines) {
                if (line.startsWith("CPU")) theLine = line;
                // info(line);
            }
            if (theLine.startsWith("CPU")) {
                String[] split = theLine.split("\\s+");
                processorData.setName(split[1] + " " + split[2] + " " + split[3]);
                processorData.setProcessorFrequency(Double.parseDouble(split[6].replaceAll("[^\\d.]", "")));
                processorData.setNumberOfCores(Integer.parseInt(split[7]));
                processorData.setLogicalCores(getNumberOfLogicalProcessor());
                processorData.setProcessorType(getPorcessorType());
            } else {
                severe("Could not parse processor data ");
            }

        } catch (NumberFormatException e) {

        }
        return processorData;
    }


    private void forceKillService(String taskManagerName) {
        PowerShellReturn response = executePowerShellCommand("get-process " + taskManagerName + " -IncludeUserName", null);
        if (!response.isError() && !response.isTimeout()) {
            String asString = response.getCommandOutput();
            String[] split = asString.split("\n");
            for (String line : split) {
                if (line.contains("NT AUTHORITY")) {

                    split = line.split(" ");
                    int i = 0;
                    for (String part : split) {
                        if (part.contains("NT")) {
                            response = executePowerShellCommand("Stop-process -id " + split[i - 1] + " -force", null);
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
            PowerShellReturn response = executePowerShellCommand("Stop-Service -name " + serviceName, null);
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

    public String getUbuntuServicesLocation() {
        return "/etc/systemd/system/";
    }

    /**
     * added here although this is not a general requirement. todo: decide if to leave it here
     *
     * @param installationContext
     * @return
     */
    public boolean installSpring(InstallationContext installationContext) {

        Parameter container = installationContext.getParameter("container");
        if (container != null) {
            return container.getValue().toLowerCase().equals("spring");
        }
        return true;

    }

    /**
     * change paths in service file, points to the correct flexicore home
     *
     * @param installationContext
     * @return
     */
    public boolean fixServiceFile(String serviceLocation, InstallationContext installationContext) throws IOException {
        if (flexicoreHome.equals("/home/flexicore")) return false;

        String result = editFile(serviceLocation, "", "/home/flexicore/", flexicoreHome, false, false, true, true);


        return !result.isEmpty();


    }

    public boolean installService(String serviceLocation, String serviceName, String ownerName) {
        return installService(serviceLocation, serviceName, ownerName, true);
    }

    /**
     * copy and install as service based on a jar file, take latest version and use a symvolic link
     * handle jar files with the same prefix as the service file too
     * for example:
     * screenshot-taker.jar and screenshot-taker.service
     * and for example
     * screenshot-taker-1.03.jar and screenshot-taker.service
     * in this case a link to screenshot-taker.jar will be created in the target folder using latest jar found
     * the service file knows nothing about versions and will always point, in this example, to screenshot-taker.jar, be it a link or not
     *
     * @param servicesLocation where to take the service from (xxx.service file)
     * @param serviceName      name of the service
     * @param jarLocation      location to get the Java jar (of the service) from
     * @param targetLocation   where to put the jar files
     * @param startWith        what common string is there for all versions of the Jar so we can create a soft link to latest
     * @param toFind           string to find in service file when changing the target jar default location
     * @param ownerName        used in internal logging, should be sent to idnetify the caller.
     * @param startService     if true, starts service
     * @return
     * @throws InterruptedException
     */
    public boolean installLatestService(String servicesLocation, String serviceName, String jarLocation,
                                        String targetLocation, String startWith, String toFind,
                                        String ownerName, boolean startService) throws InterruptedException, IOException {
        if (!isLinux) return false;

        String latest = null;
        boolean ignoreLink = false;
        //fix an issue where there is no versioning, will use the file ''as is', no versioning information
        if (!exists(latest = jarLocation + "/" + startWith + ".jar")) {
            latest = getLatestVersion(jarLocation, startWith, ".jar");
        } else ignoreLink = true;

        if (latest == null || latest.isEmpty()) return false;
        String target = targetLocation + "/" + new File(latest).getName();
        if (!copySingleFile(latest, target)) {
            updateProgress(getContext(), "failed to copy : " + latest + " to: " + target);
            return false;
        }
        String cleanName = target.substring(0, target.lastIndexOf(".jar"));
        String targetServiceJarPath = target;


        boolean linkResult = true;
        if (!ignoreLink) {
            targetServiceJarPath = (cleanName = target.substring(0, target.lastIndexOf("-"))) + ".jar";
            if (exists(targetServiceJarPath)) Files.deleteIfExists(Paths.get(targetServiceJarPath));
            String[] args = {"ln", "-s", target, targetServiceJarPath};
            linkResult = executeCommandByBuilder(args, "", false, ownerName, false);

        }
        if (linkResult) {
            String sourceServiceFile;
            String targetServiceFile;
            String filename = null;
            if (exists(sourceServiceFile = servicesLocation + "/" +
                    (filename = new File(cleanName).getName() + ".service"))) {
                if (copySingleFile(sourceServiceFile, targetServiceFile = getUbuntuServicesLocation() + "/" + filename)) {

                    if (exists(targetServiceFile)) {
                        String result = editFile(targetServiceFile,
                                "",
                                toFind,
                                targetLocation,
                                false, false, true, true);
                        if (!(result == null && !result.isEmpty())) {
                            return installService(null, serviceName, ownerName, startService);
                        }

                    }

                }
            } else {
                info("failed to find the source service file at: " + sourceServiceFile);
            }
        } else {
            info("failed to create softlink to: " + latest + " " + targetServiceJarPath);
        }
        return false;
    }

    /**
     * install service from a service file, Linux only
     *
     * @param serviceLocation the location of the service file to be copied to /etc/systemd/system (on Ubuntu), if NULL will not copy
     * @param serviceName
     * @param ownerName
     * @return
     */
    public boolean installService(String serviceLocation, String serviceName, String ownerName, boolean startService) {

        if (isWindows) return false;
        try {
            if (testServiceRunning(serviceName, ownerName, false)) {
                setServiceToStop(serviceName, ownerName);
            }
            if (serviceLocation != null) {
                Files.copy(Paths.get(serviceLocation), Paths.get(getUbuntuServicesLocation() + serviceName + ".service"), StandardCopyOption.REPLACE_EXISTING);
            }
            if (executeCommand("systemctl enable " + serviceName, "", "Set " + serviceName + " to start automatically")) {
                updateProgress(getContext(), serviceName + " service will automatically start");

                if (executeCommand("systemctl daemon-reload", "", "setting service" + serviceName)) {
                    if (startService) {
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
                        setServiceToStop(serviceName, ownerName);
                        info("was not set to start service : " + serviceName);
                        return true;
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
            PowerShellReturn response = executePowerShellCommand("Start-Service -name " + serviceName, null);
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

    public ScriptResult executeBashScriptExt(String script, String toFind, String ownerName, boolean setCurrentFolder) {
        ScriptResult result = new ScriptResult();
        if (exists(script)) {
            if (isLinux) {


                String[] cmd = new String[]{"/bin/sh", script};
                try {


                    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                    if (setCurrentFolder) {
                        processBuilder.directory(new File(script).getParentFile());
                    }
                    result.setResult(processBuilder.start().waitFor());

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            processBuilder.start().getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.getOutput().add(line);
                    }
                    reader = new BufferedReader(new InputStreamReader(
                            processBuilder.start().getErrorStream()));

                    while ((line = reader.readLine()) != null) {
                        result.getErrors().add(line);
                    }
                    if (toFind!=null && !toFind.isEmpty()) {
                        for (String theLine:result.getOutput()) {
                            if (theLine.contains(toFind )) return result;
                        }
                        severe("Cannot find  "+toFind+" in the output stream");
                        return null;
                    }


                    info("Result from script execution was: " + result);
                    return result;
                } catch (IOException | InterruptedException e) {
                    severe("Exception while running script: " + script, e);

                }
            } else {

                info ("this method is for Linux only" +script );


            }
        }else {
            severe("Cannot find file: "+script);
        }
        return null;
    }

    /**
     * taskbar not supported as of 13-apr
     */
    public enum ShortCutType {
        desktop, startmenu, taskbar
    }

    /**
     * create shortcut to an app using (internally) PowerShell.
     *
     * @param targeLocation where the target for this shortcut resides.
     * @param linkName      name of the shortcut
     * @param allUsers      if true, shortcuts will be created for all users. this has no effect on start menu changes
     * @param iconFile      path to an icon file.
     * @param iconIndex     index to the icon when the iconFile contains multiple icons
     * @return true if successful, failures are logs into the logging file.
     */
    public boolean createLinkPS(String targeLocation, String linkName, String iconFile, int iconIndex, boolean allUsers, ShortCutType shortCutType) {
        try {

            linkName = removeExtension(linkName) + ".lnk";
            File tempFile = File.createTempFile("PSScript", ".ps1");
            String[] data = new String[]{"$Shell = New-Object -ComObject (\"WScript.Shell\")",
                    "$public=[Environment]::GetFolderPath(\"CommonDesktopDirectory\")",
                    "$public",
                    shortCutType == ShortCutType.desktop ?
                            (allUsers ?
                                    "$objShortCut = $Shell.CreateShortcut(\"$public\\" + linkName + "\")" :
                                    "$objShortCut = $Shell.CreateShortcut($env:USERPROFILE + \"\\Desktop\\" + linkName + "\")")
                            : (allUsers ?
                            "$objShortCut = $Shell.CreateShortcut(\"C:\\Users\\All Users\\Microsoft\\Windows\\Start Menu\\\" + \"" + linkName + "\")" :
                            "$objShortCut = $Shell.CreateShortcut(\"C:\\Users\\All Users\\Microsoft\\Windows\\Start Menu\\\" + \"" + linkName + "\")"),

                    "$objShortcut.TargetPath = \"" + targeLocation + "\"",
                    "$objShortcut.IconLocation = \"" + iconFile + "," + iconIndex + "\"",
                    "$objShortcut.Save()"
            };
            fillTempFile(data, tempFile);
            PowerShellReturn result = executeScript(context.getLogger(), tempFile.getAbsolutePath(), new String[]{});
            if (!result.isError()) {
                info("Short cut created for: " + targeLocation + " on " + (shortCutType.equals(ShortCutType.startmenu) ? " Start menu " : "Desktop"));
                return true;
            } else {

                severe("Error while creating shortcut: " + result.getError());
            }
            Files.deleteIfExists(Paths.get(tempFile.getAbsolutePath()));
        } catch (IOException e) {
            severe("Error while creating shortcut ", e);
        }
        return false;

    }

    /**
     * Creates a shortcut, Windows only
     *
     * @param targetLocation   the file (executable or other) that is the target of the link
     * @param shortcutLocation the location of the shortcut, can be System.getProperty("user.home")+"/Desktop/"+"shortcut.lnk" (example)
     * @param iconIndex        index in the SystemRoot%\system32\SHELL32.dll icons
     * @param workingDir       the working directory for the target file.
     * @param iconsPath        optional icon path, if it is null,  SHELL32.dll is used (in system32 folder)
     * @return
     */
    @Deprecated
    public boolean createLink(String targetLocation, String shortcutLocation, int iconIndex, String iconsPath, String workingDir) {
        if (!isWindows()) return false;
        File file = new File(targetLocation);
        if (file.exists()) {
            ShellLink sl = ShellLink.createLink(targetLocation)
                    .setWorkingDir(workingDir)
                    .setIconLocation(iconsPath == null ? "%SystemRoot%\\system32\\SHELL32.dll" : iconsPath);
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

    public static String dotNetVersion = "0.0";

    /**
     * @param logger
     * @param scriptLocation if empty, will look for net.ps1 in the parent of this folder
     * @return a list of available .net versions and build.
     */
    public static List<String> getDotNetVersions(Logger logger, String scriptLocation) {

        List<String> versions = new ArrayList<>();
        if (isWindows) {
            String current = Paths.get(".").toAbsolutePath().normalize().toString();
            File file;
            if (scriptLocation == null || scriptLocation.isEmpty()) {
                file = new File(new File(current).getParent() + "/net.ps1");
            } else {
                file = new File(scriptLocation);
            }
            if (file.exists()) {
                PowerShellReturn result = InstallationTask.executeScript(logger, file.getAbsolutePath(), new String[]{});
                for (String line : result.getOutput()) {
                    String[] lineData = line.split(" ");
                    if (lineData.length > 3) {
                        if (lineData[3].contains(".")) {
                            versions.add(lineData[3]);
                            String[] version = lineData[3].split("\\.");
                            if (version.length > 1) {
                                String dotnet = version[0] + "." + version[1];
                                if (dotnet.compareTo(dotNetVersion) >= 0) dotNetVersion = dotnet;
                            }
                        }
                    }
                }
            }
        }
        return versions;
    }

    /**
     * Creates a link to a URL on the desktop
     *
     * @param url      URL for the link for example:http://localhost:8080
     * @param linkName Name of the link for the shortcut.
     * @param allUsers if true, link will be created for all users.
     * @return
     */
    public boolean createUrlLink(String url, String linkName, boolean allUsers) {

        try {
            if (!linkName.endsWith(".url")) {
                linkName = removeExtension(linkName) + ".url";
            }
            File tempFile = File.createTempFile("PSScript", ".ps1");
            String[] data = new String[]{"$Shell = New-Object -ComObject (\"WScript.Shell\")",
                    "$public=[Environment]::GetFolderPath(\"CommonDesktopDirectory\")",
                    "$public",
                    !allUsers ? "$Favorite = $Shell.CreateShortcut($env:USERPROFILE + \"\\Desktop\\" + linkName + "\")" :
                            "$Favorite = $Shell.CreateShortcut(\"$public\\" + linkName + "\")",
                    "$Favorite.TargetPath = \"" + url + "\"",
                    "$Favorite.Save()"
            };
            fillTempFile(data, tempFile);
            PowerShellReturn result = executeScript(context.getLogger(), tempFile.getAbsolutePath(), new String[]{});
            if (!result.isError()) {
                return true;
            } else {
                severe(result.getError());
            }
            Files.deleteIfExists(Paths.get(tempFile.getAbsolutePath()));
        } catch (IOException e) {
            severe("Error while creating shortcut ", e);
        }
        return false;
    }

    private void fillTempFile(String[] lines, File tempFile) throws IOException {
        BufferedWriter out = new BufferedWriter
                (new OutputStreamWriter(new FileOutputStream(tempFile.getAbsolutePath()), StandardCharsets.US_ASCII));
        for (String s : lines) {
            out.write(s + "\n");
        }
        out.close();

    }

    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    @Deprecated
    public boolean executeBashScriptLocal(String name, String toFind, String ownerName) {

        return executeBashScript(getScriptsPath() + "/" + name, toFind, ownerName, true);
    }

    public static File getTempFolder() {
        return new File(System.getProperty("java.io.tmpdir"));
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
            if (context.getLogger() != null) {
                context.getLogger().info(message);
            }
        }
        System.out.println("info: " + message);
    }

    /**
     * write an error to the log file
     *
     * @param message
     * @param e
     */
    public void error(String message, Throwable e) {
        if (context != null) {
            if (context.getLogger() != null) {
                context.getLogger().log(Level.SEVERE, message, e);
                return;
            }
        }
        System.out.println("Severe: " + message);
    }

    public void severe(String message, Throwable e) {

        error(message, e);
    }

    public void severe(String message) {
        if (context != null) {
            if (context.getLogger() != null) {
                context.getLogger().log(Level.SEVERE, message);
                return;
            }
        }
        System.out.println("Severe: " + message);
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


    /**
     * Execute a single PowerShell command
     *
     * @param command
     * @param logger
     * @return
     */
    public static PowerShellReturn executePowerShellCommand(String command, Logger logger) {
        PowerShellReturn powerShellReturn = null;
        if (isWindows) {
            Runtime runtime = Runtime.getRuntime();
            Process proc = null;

            try {
                proc = runtime.exec("powershell.exe " + command);
                int result = proc.waitFor();
                powerShellReturn = new PowerShellReturn(result);

                powerShellReturn.fillInput(proc, null);
                powerShellReturn.fillError(proc, null);
                proc.getOutputStream().close();
            } catch (IOException | InterruptedException e) {
                if (logger != null) logger.log(Level.SEVERE, "Error while executing PS command: ", e);
            }
        }

        return powerShellReturn;
    }

    /**
     * Execute a PowerShell script
     *
     * @param logger
     * @param script
     * @param params
     * @return PowerShellReturn instance or null, the instance contains return value (usually 0) and list of output lines + list of error lines
     */
    public static PowerShellReturn executeScript(Logger logger, String script, String[] params) {
        PowerShellReturn unblkesult = executePowerShellCommand("Unblock-File " + script, null);
        PowerShellReturn powerShellReturn = null;
        if (new File(script).exists()) {
            String parameters = prepareParams(params);
            Runtime runtime = Runtime.getRuntime();
            Process proc = null;

            try {
                proc = runtime.exec("powershell.exe -File \"" + script + "\" " + parameters); //must be for spaces in the file
                int result = proc.waitFor();
                powerShellReturn = new PowerShellReturn(result);
                powerShellReturn.fillInput(proc, logger);
                powerShellReturn.fillError(proc, logger);
                proc.getOutputStream().close();
            } catch (IOException | InterruptedException e) {
                logger.log(Level.SEVERE, "Error while executing PowerShell Script ", e);
            }
        } else logger.log(Level.SEVERE, "cannot find script: " + script);
        return powerShellReturn;
    }

    private static String prepareParams(String[] params) {
        StringBuilder sb = new StringBuilder();
        for (String s : params) {
            s = s.trim();
            if (s.contains(" ")) {
                if (!s.startsWith("\"")) s = "\"" + s;
                if (!s.endsWith("\"")) s += "\"";
            }
            if (sb.length() != 0) sb.append(" ");
            sb.append(s);
        }
        return sb.toString();
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

    /**
     * get list of services starting with pattern (with * wildcard)
     *
     * @param pattern
     * @param logger
     * @return
     */
    public List<WindowsInstalledService> getServices(String pattern, Logger logger) {
        String command;

        command = "Get-Service " + pattern;

        PowerShellReturn response = executePowerShellCommand(command, logger);
        return parseServices(response, pattern);

    }

    public List<WindowsInstalledComponent> getInstalledClasses(String pattern) {
        String command;
        if (pattern.contains("*")) {
            command = "Get-WmiObject -Class Win32_Product | Where-Object{$_.Name -like '" + pattern + "'}";
        } else {
            command = "Get-WmiObject -Class Win32_Product | Where-Object{$_.Name -like  '*" + pattern + "*'}";
        }
        PowerShellReturn response = executePowerShellCommand(command, null);
        List<WindowsInstalledComponent> result = getComponents(response);
        if (result != null) return result;
        return null;
    }

    public List<WindowsInstalledComponent> getallInstalledClasses() {
        String command = "Get-WmiObject -Class Win32_Product";
        PowerShellReturn response = executePowerShellCommand(command, null);
        List<WindowsInstalledComponent> result = getComponents(response);
        if (result != null) return result;
        return null;
    }

    private List<WindowsInstalledComponent> getComponents(PowerShellReturn response) {
        List<WindowsInstalledComponent> result = new ArrayList<>();
        if (response != null) {

            if (response.getOutput().size() != 0) {
                WindowsInstalledComponent component = null;
                for (String line : response.getOutput()) {

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

    private List<WindowsInstalledService> parseServices(PowerShellReturn response, String pattern) {
        List<WindowsInstalledService> result = new ArrayList<>();
        if (pattern.endsWith("*")) pattern = pattern.substring(0, pattern.indexOf("*"));

        if (response != null) {

            if (response.getOutput().size() != 0) {

                boolean summaryLineFound = false;
                for (String line : response.getOutput()) {
                    if (line.contains("--")) {
                        summaryLineFound = true;
                        continue;
                    } else if (!summaryLineFound) continue;

                    String[] parsed = line.split(pattern);
                    if (parsed.length != 1) {
                        WindowsInstalledService service = new WindowsInstalledService();
                        service.setStatus(parsed[0].trim());
                        service.setName(pattern + (parsed[1].trim()));
                        service.setDisplayName(pattern + (parsed[2].trim()));
                        result.add(service);
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
     * Execute a Shell command so users can respond to instructions on screen
     * @param commandArgs
     * @return
     */
    public int executeCommandByBuilderInteractive(String [] commandArgs) {
        final ProcessBuilder p = new ProcessBuilder(commandArgs);

        try {
            p.redirectInput(ProcessBuilder.Redirect.INHERIT);
            p.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            p.redirectError(ProcessBuilder.Redirect.INHERIT);

            int result = p.start().waitFor();
        } catch (InterruptedException e) {
           severe("Error while executing command interactively ",e);
        } catch (IOException e) {
            severe("Error while executing command interactively ",e);
        }
        return -1;
    }
    public boolean executeCommandByBuilder(String[] args, String toFind, boolean notToFind, String ownerName, String currentFolder) throws IOException {


        ProcessBuilder pb = new ProcessBuilder(args);
        if (currentFolder != null && !currentFolder.isEmpty()) {
            if (isLinux) {
                if (args[0].equals("/bin/sh")) {
                    pb.directory(new File(args[1]).getParentFile()); //set current directory to the actual location of the script
                } else {
                    pb.directory(new File(args[0]).getParentFile());
                }
            } else {
                if (isWindows()) {
                    pb.directory(new File(currentFolder));
                }
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
     * run a process, collect output or error lines
     *
     * @param args
     * @return
     * @throws IOException
     */
    public ProcessResult executeCommandByBuilder(String[] args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(args);
        Process process;
        process = pb.start();
        ProcessResult result = new ProcessResult();
        try {
            boolean notFailed = contWithProcess(process, "", false, "");
            result.setResult(notFailed);
            if (notFailed) {
                result.getLines().addAll(lines);
            } else {
                result.getErrorLines().addAll(errorLines);
            }
        } catch (Exception e) {
            severe("error", e);
        }
        return result;

    }

    public class ProcessResult {
        private List<String> lines = new ArrayList<>();
        private List<String> errorLines = new ArrayList<>();
        private boolean result;

        public List<String> getLines() {
            return lines;
        }

        public ProcessResult setLines(List<String> lines) {
            this.lines = lines;
            return this;
        }

        public List<String> getErrorLines() {
            return errorLines;
        }

        public ProcessResult setErrorLines(List<String> errorLines) {
            this.errorLines = errorLines;
            return this;
        }

        public boolean isResult() {
            return result;
        }

        public ProcessResult setResult(boolean result) {
            this.result = result;
            return this;
        }
    }

    /**
     * get latest version , assuming version numbering increases the lexical location
     *
     * @param path      where to find the files
     * @param startwith common start string for all files.
     * @return
     */
    public String getLatestVersion(String path, String startwith, String endWith) {
        String result = null;
        if (exists(path)) {
            File folder = new File(path);
            File[] files = folder.listFiles();
            for (File file : files) {
                if (!file.getName().startsWith(startwith)) continue;
                if (endWith != null) {
                    if (!file.getName().endsWith(endWith)) continue;
                }
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
            //info("Process is: " + process.pid());
            int exitVal = process.waitFor();
            if (exitVal != 1062) info(ownerName + ", Exit Val for script :" + exitVal);
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
    public boolean updateThis = false;

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
     * get welcome message, usually only one plugin provides this.
     *
     * @return array of UserMessage or null
     */
    @Override
    public UserMessage[] getWelcomeMessage() {
        return null;
    }

    /**
     * get final message, usually only one plugin provides this.
     *
     * @return array of UserMessage or null
     */
    @Override
    public UserMessage[] getFinalMessage() {
        return null;
    }

    @Override
    public UserMessage[] getRunningMessages() {
        return null;
    }

    @Override
    public UserMessage[] getFinalMessageOnError() {
        return null;
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

    /**
     * provide the number of seconds for typical installation on a known hardware platform
     * this can be used for a more accurate progress indication.
     *
     * @return
     */
    @Override
    public Integer averageDuration() {
        return 60;
    }

    public double getFactoredDuration() {
        return averageDuration() * getContext().getTimeFactor();
    }

    @Override
    public Integer averageFinalizerDuration() {
        return 5;
    }

    public double getFactoredFinalizerDuration() {
        return averageFinalizerDuration() * getContext().getTimeFactor();
    }

    @Override
    public Integer averageServiceDuration() {
        return 2;
    }

    public double getFactoredServiceDuration() {
        return averageServiceDuration() * getContext().getTimeFactor();
    }

    /**
     * provide the number of seconds for typical installation on a known hardware platform
     * this can be used for a more accurate progress indication.
     *
     * @return
     */


    @Override
    public Service getService() {
        return service;
    }

    @Override
    public boolean completedReported() {
        return completedReported;
    }

    @Override
    public boolean finalizerCompletedReported() {
        return finalizerReported;
    }

    public InstallationTask setCompletedReported(boolean completedReported) {
        this.completedReported = completedReported;
        return this;
    }

    public InstallationTask setFinalizerReported(boolean finalizerReported) {
        this.finalizerReported = finalizerReported;
        return this;
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
        completedReported = false;
        finalizerReported = false;
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

    /**
     * allows installers to check if port is available and free to use
     *
     * @param port
     * @return
     */
    public static boolean checkPortAvailable(int port) {


        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
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

        String result= getContext().getParamaters().getValue("scriptspath");
        if (isLinux) result=new File(result).getAbsolutePath();
        return result+"/";

    }

    public String getServicesPath() {
        String result= getContext().getParamaters().getValue("servicespath");
        if (isLinux) result=new File(result).getAbsolutePath();
        return result+"/";

    }

    public String getIoTPath() {

        String result= getContext().getParamaters().getValue("iotpath");
        if (isLinux) result=new File(result).getAbsolutePath();
        return result+"/";

    }

    /**
     * copy a single file.
     *
     * @param source
     * @param target
     * @return
     * @throws IOException
     */
    public boolean copySingleFile(String source, String target) {
        if (exists(source)) {
            try {
                ensureTarget(Paths.get(target).getParent().toString());
                Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                severe("Error while copying: " + source + " to: " + target, e);
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean updateFolder(String source, String target, String ownerName) throws IOException, InterruptedException {
        if (!new File(source).isDirectory()) return copySingleFile(source, target);
        boolean result = true;
        if (exists(target)) {
            result = deleteDirectoryStream(target);
        }

        if (result) {
            if (!copy(source, target, ownerName)) {
                severe("Failed to copy " + source + " to " + target);
            } else {
                return true;
            }
        } else {
            severe("Failed to delete  " + target);

        }
        return result;
    }

    /**
     * recursively copy folders, will copy single file too.
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
                if (!new File(installationDir).isDirectory()) {
                    return copySingleFile(installationDir, targetDir);

                }
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

    public String fixIfWindows(String fileAsString) {
        return fixWindows(fileAsString);
    }

    public String fixWindows(String fileAsString) {
        if (isWindows()) {
            info("Fixing Windows path");
            if (fileAsString.startsWith("..")) {
                info("fixing parent replacement for Windows");
                String userDir = System.getProperty("user.dir");
                File current = new File(userDir);
                String parentPath = current.getParent();
                fileAsString = fileAsString.replace("..", parentPath);
                fileAsString = fileAsString.replaceAll("/", "\\\\");
                info("resulting file is: " + fileAsString);

            }
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
                if (properties.get(key) == null || !properties.get(key).equals(value)) {
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


    public InstallationTask setWrongOS(boolean wrongOS) {
        this.wrongOS = wrongOS;
        return this;
    }

    public static WindowsVersion getWindowsVersion() {
        if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("xp")) {
            return WindowsVersion.Windows_XP;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("vista")) {
            return WindowsVersion.Windows_VISTA;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("7")) {
            return WindowsVersion.Windows_7;
        } else if (System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("os.name").toLowerCase().contains("8")) {
            return WindowsVersion.Windows_8;
        } else return WindowsVersion.Windows_10;
    }

    public boolean isDry() {
        return dry;
    }

    public InstallationTask setDry(boolean dry) {
        this.dry = dry;
        return this;
    }

    /**
     * get a dialog (console) prompt for update in indiciudal task
     *
     * @param ownerName
     * @return
     */
    protected UserAction askUserForUpdate(String ownerName) {
        UserAction ua = new UserAction();
        ua.addMessage(new UserMessage().setMessage(ownerName + ": a version is already installed").setEmphasize(2)
                .setSide(UserMessage.Side.left).setColor(Ansi.Color.GREEN).setFontSize(26).setUseFont(true));
        ua.addMessage(new UserMessage().setMessage("Do you want to update it?").setEmphasize(2)
                .setSide(UserMessage.Side.left).setColor(Ansi.Color.BLACK).setFontSize(16).setUseFont(true));
        ua.setPossibleAnswers(new UserResponse[]{UserResponse.YES, UserResponse.NO});
        ua.setUseAnsiColorsInConsole(true);
        ua.setDefaultLeftMargin(20).setAskForUpdate(true);
        return ua;

    }

    public boolean isUpdateThis() {
        return updateThis;
    }

    public InstallationTask setUpdateThis(boolean updateThis) {
        this.updateThis = updateThis;
        return this;
    }


}


