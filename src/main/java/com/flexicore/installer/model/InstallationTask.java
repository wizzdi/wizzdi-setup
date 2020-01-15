package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.StreamGobbler;
import com.wizzdi.installer.*;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InstallationTask implements IInstallationTask {
    private LocalDateTime started;
    private LocalDateTime ended;
    private Integer progress = 0;
    private String name = "Unnamed task";
    private String id;
    private String version = "1.0.0";
    private boolean enabled = true;
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

    public Parameters getPrivateParameters() {
        return null;
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
            return executeCommand("sc stop " + serviceName, "STOP_PENDING", "Set Service To Stop " + serviceName);
        } else {
            return executeCommand("service  " + serviceName + " stop", "", "Set Service To Stop " + serviceName);
        }
    }

    //todo: check that Linux version works.
    public boolean setServiceToStart(String serviceName, String ownerName) {
        if (isWindows()) {
            return executeCommand("sc start " + serviceName, "START_PENDING", "Set Service To Stop " + serviceName);
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
     * @param script
     * @param toFind
     * @param ownerName
     * @return
     */
    public boolean executeBashScript(String script, String toFind, String ownerName) {

        if (new File(script).exists() && !isWindows()) {

            executeCommand("chmod +x " + script, "", "change mode to execute");
            String[] cmd = new String[]{"/bin/sh", script};
            try {
                boolean result = executeCommandByBuilder(cmd, toFind, false, ownerName);

                info("Result from script execution was: " + result);
                return result;
            } catch (IOException e) {
                severe("Exception while running script: " + script, e);
                return false;
            }
        } else {

            severe("******Cannot dry script: " + script);

            return false;
        }
    }

    /**
     *Creates a shortcut, Windows only
     * @param targetLocation the file (executable or other) that is the target of the link
     * @param shortcutLocation the location of the shortcut, can be System.getProperty("user.home")+"/Desktop/"+"shortcut.lnk" (example)
     * @param iconIndex index in the SystemRoot%\system32\SHELL32.dll icons
     * @param workingDir the working directory for the target file.
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
                info("Have successfully created a shortcut at: "+shortcutLocation);
                return true;
            } catch (IOException e) {
                severe("Error while creating shortcut", e);
            }
        }
        return false;

    }

    public boolean executeBashScriptLocal(String name, String toFind, String ownerName) {

        return executeBashScript(getScriptsPath() + "/" + name, toFind, ownerName);
    }

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

            if (!result) debuglines(command, false);
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

        if (errorLines.size() != 0 && (force || context.getParamaters().getBooleanValue("debug"))) {
            info("debug lines from E stream  while executing OS shell command or script");
            for (String message : errorLines) {
                if (!message.isEmpty()) {

                    info("****** debug line  : " + command + ": " + message);
                }
            }
        }
        if (lines.size() != 0 && (force || context.getParamaters().getBooleanValue("debug"))) {
            info("normal lines from E stream  while executing OS shell command or script");
            for (String message : lines) {
                if (!message.isEmpty()) {

                    info("****** debug line  : " + command + ": " + message);
                }
            }
        }
    }

    public boolean executeCommandByRuntime(String target, String ownerName) {
        return executeCommand(target, "", ownerName);

    }

    public boolean executeCommandByBuilder(String[] args, String toFind, boolean notToFind, String ownerName) throws IOException {
        if (args[0].equals("msiexec")) {
            args[1] = args[1].replace("/", "\\");
        }
        ProcessBuilder pb = new ProcessBuilder(args);
        Process process;
        process = pb.start();
        return contWithProcess(process, toFind, notToFind, ownerName);


    }


    private boolean contWithProcess(Process process, String toFind, boolean notToFind, String ownerName) {
        try {

            boolean show = ownerName.contains("Update Linux repositories");
            show = false; //supress output even if updating linux
            StreamGobbler errorGobbler = new
                    StreamGobbler(process.getErrorStream(), "ERROR", context.getLogger(), show);

            StreamGobbler outputGobbler = new
                    StreamGobbler(process.getInputStream(), "OUTPUT", context.getLogger(), show);

            errorGobbler.start();
            outputGobbler.start();

            int exitVal = process.waitFor();
            info(ownerName + ", Exit Val for script :" + exitVal);
            errorLines.clear();
            errorLines.addAll(errorGobbler.getLines());
            lines.clear();
            lines.addAll(outputGobbler.getLines()); //for debugging purposes
            debuglines(ownerName, true || exitVal != 0); //write debug lines when exit val is not zero
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
        flexicoreSource = getServerPath() + "/flexicore";
        flexicoreHome = getFlexicoreHome();
        dry = getContext().getParamaters().getBooleanValue("dry");
        update = getContext().getParamaters().getBooleanValue("update");
        force = getContext().getParamaters().getBooleanValue("force");
        if (dry) {
            info("Dry run  of " + this.getId() + " -> " + this.getDescription() + getParameters(installationContext).toString());
            return new InstallationResult().setInstallationStatus(InstallationStatus.ISDRY);
        }
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
                        if (executeCommandByBuilder(new String[]{path}, "", false, "")) {
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
                result = executeCommandByBuilder(new String[]{"cmd", "/C", "move", source, target}, "", false, "move server sources");
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
        return getContext().getParamaters().getValue("serverpath");
    }


    public String getInstallationsPath() {
        return getContext().getParamaters().getValue("installationspath") + "/";

    }

    public String getScriptsPath() {
        return getContext().getParamaters().getValue("scriptspath") + "/";

    }

    public String getServicesPath() {
        return getContext().getParamaters().getValue("servicespath") + "/";

    }

    public boolean copySingleFile(String source, String target) throws IOException {
        if (exists(source)) {
            Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        return false;
    }

    /**
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
     * @param installationDir
     * @param targetDir
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
                previousContent = editFile(path, previousContent, parameter.getName(), parameter.getValue(), false, false, --i == 0);

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
     * @param path
     * @param existingString from a previous call, saves time in open/close file sequence, all stages but one in memory
     * @param toFind
     * @param toReplace
     * @param warning
     * @param reverseSlash
     * @param close          , if false returns the content as string to be passed to a next call in existingString
     * @return
     */
    public String editFile(String path, String existingString, String toFind, String toReplace, boolean warning, boolean reverseSlash, boolean close) {
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
            InputStream is;

            try {
                is = new FileInputStream(path);

                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();
                while (line != null) {
                    sb.append(line).append("\n");
                    line = buf.readLine();
                }
                fileAsString = sb.toString();
                if (fileAsString.length() == 0) {
                    is.close();
                    severe("[Edit file]  file was empty");
                    return null;
                }


                is.close();
            } catch (IOException e) {
                severe("Error while reading file", e);
                return null;
            }
        }
        if (fileAsString != null) {
            info("[Edit file] [Edit file]  file as string is not null");
            if (!fileAsString.contains(toFind)) {
                info("[Edit file] [Edit file]  file as string doesn't contain: " + toFind);
                return null;
            }
            fileAsString = fileAsString.replaceAll(toFind, toReplace);
            if (isWindows() && reverseSlash) {
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
            }
        }
        if (close) {
            try {
                Files.write(Paths.get(path), fileAsString.getBytes());
            } catch (IOException e) {
                severe("Error while writing file", e);
            }
        }
        info("[Edit file] [Edit file] ->" + fileAsString);
        return fileAsString;
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
        return getContext().getParamaters().getValue("flexicorehome");

    }

    public String getWildflyHome() {
        return getContext().getParamaters().getValue("wildflyhome");

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
        return editFile(path, "", "/home/flexicore", flexicoreHome, false, false, true) != null;
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


