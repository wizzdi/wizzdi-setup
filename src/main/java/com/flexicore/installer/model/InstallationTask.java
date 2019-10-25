package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.StreamGobbler;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InstallationTask implements IInstallationTask {
    private LocalDateTime started=LocalDateTime.now();
    private LocalDateTime ended=LocalDateTime.now();
    private Integer progress=0;
    private String name="Unnamed task";
    private String id;
    private String version="1.0.0";
    private boolean enabled=true;
    private InstallationStatus status=InstallationStatus.CREATED;
    private String description="no description";

    private InstallationContext context;
    private int order=-1;

    public Parameters getPrivateParameters() {
        return null;
    }


   final public static boolean isWindows= SystemUtils.IS_OS_WINDOWS;
    final public static boolean isLinux= SystemUtils.IS_OS_LINUX;
    final public static boolean isMac= SystemUtils.IS_OS_MAC;
    Queue<String> lines = new ConcurrentLinkedQueue<String>();
    Queue<String> errorLines = new ConcurrentLinkedQueue<String>();
    @Override
    public OperatingSystem getCurrentOperatingSystem() {
        if (isWindows) return  OperatingSystem.Windows;
        if (isLinux) return  OperatingSystem.Linux;
        if (isMac) return  OperatingSystem.OSX;
        return OperatingSystem.Linux;
    }

    public boolean testServiceRunning(String serviceName, String ownerName) {
        if (isWindows) {

            return executeCommand("sc query " + serviceName, "RUNNING", "checking if service " + serviceName + " runs");
        } else {
            return executeCommand("service  " + serviceName + " status", "active", "checking if service " + serviceName + " runs");
        }
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

    public boolean setServiceToAuto(String serviceName, String ownerName) {
        if (isWindows) {
            return executeCommand("sc config " + serviceName + " start= auto", "success", "Set Service To Auto");
        }
        return true;
    }

    public void addMessage(String phase, String severity, String message) {
        if (severity != "info") {
            severe(phase + "  " + message);
        } else {
            info(phase + "  " + message);
        }

    }

    public boolean setServiceToStop(String serviceName, String ownerName) {
        if (isWindows) {
            return executeCommand("sc stop " + serviceName, "STOP_PENDING", "Set Service To Stop " + serviceName);
        } else {
            return executeCommand("service  " + serviceName + " stop", "", "Set Service To Stop " + serviceName);
        }
    }

    public boolean setServiceToStart(String serviceName, String ownerName) {
        return executeCommand("sc start " + serviceName, "START_PENDING", "Set Service To Stop " + serviceName);
    }

    public boolean executeBashScript(String script, String toFind, String ownerName) {

        if (new File(script).exists()) {

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

            severe("******Cannot find script: " + script);

            return false;
        }
    }

    public boolean executeBashScriptLocal(String name, String toFind, String ownerName) {
        String dir = System.getProperty("user.dir");
        return executeBashScript(dir + "/" + name, toFind, ownerName);
    }

    public boolean executeCommand(String command, String toFind, String ownerName) {


        Process process = null;

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
    }

    boolean executeCommandByBuilder(String[] args, String toFind, boolean notTofind, String ownerName) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(args);
        Process process;
        process = pb.start();
        return contWithProcess(process, toFind, notTofind, ownerName);


    }


    private boolean contWithProcess(Process process, String toFind, boolean notTofind, String ownerName) {
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
            errorLines.clear();
            errorLines.addAll(errorGobbler.getLines());
            lines.clear();
            lines.addAll(outputGobbler.getLines()); //for debugging purposes
            debuglines(ownerName, false);
            if (!isWindows && exitVal == 4) {
                return false; //seems to be the response when looking for a non-existent service. TODO:make sure that this is the case
            } else {
                if (exitVal == 1603) {
                    severe("Installation should run with administrative rights");
                }
            }
            if (toFind == null || toFind.isEmpty()) return exitVal == 0;
            if (notTofind) {
                return (exitVal == 0 && !outputGobbler.findString(toFind));
            } else {
                boolean r = outputGobbler.findString(toFind);
                return ((exitVal == 0) && r);
            }

        } catch (InterruptedException e) {

            error("Error while executing command :  ownername: " + ownerName, e);
            return false;
        }
    }

    public String flexicoreSource;
    public String flexicoreHome;


    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        context = installationContext;
         flexicoreSource = getServerPath() + "/flexicore";
         flexicoreHome = getFlexicoreHome();

        if (isDry()) {
            info("Dry run  of " + this.getId() + " -> " + this.getDescription() + getParameters(installationContext).toString());
            return new InstallationResult().setInstallationStatus(InstallationStatus.DRY);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.CONTINUE);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public IInstallationTask setOrder(int order) {
        this.order=order;
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
        this.id=id;
        return this;
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

    @Override
    public InstallationStatus getStatus() {
        return status;
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        return new Parameters();
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public IInstallationTask setEnabled(boolean value) {
        enabled=value;
        return this;
    }


    @Override
    public boolean cleanup() {
        return false;
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
    public IInstallationTask setProgress(Integer progress) {
        this.progress=progress;
        return this;
    }

    @Override
    public IInstallationTask setStatus(InstallationStatus status) {
        this.status=status;
        return this;
    }

    @Override
    public InstallationTask setEnded(LocalDateTime ended) {
        this.ended = ended;
        return this;
    }

    @Override
    public IInstallationTask setName(String name) {
        this.name=name;
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

    public void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public boolean deleteDirectoryStream(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            deleteDirectoryStream(file.toPath());
            return true;
        }

        return false;
    }

    protected void ensureTarget(String targetDir) {
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
                severe(message);
                break;
            default:
                info(message);

        }
    }

    /**
     * added few methods for accessing common parameters although these are not known to the Installer.
     * These parameters are common in every FC installation.
     */

    public String getTargetPath() {
        return getContext().getParamaters().getValue("targetpath");

    }

    public boolean isDry() {
        return getContext().getParamaters().getBooleanValue("dry");
    }

    /**
     * get
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


    public String getServerPath() {
        return getContext().getParamaters().getValue("serverpath");
    }

    public String getAbsoluteServerSource() {
        return getContext().getParamaters().getValue("sourcepath") + "/wildfly";

    }

    public String getInstallationsPath() {
        return getContext().getParamaters().getValue("installlations");

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
            if (!isDry()) {
                ensureTarget(targetDir);
                try {
                    CopyFileVisitor copyFileVisitor = null;
                    addMessage("" + ownerName + " copying server", "info", "copy started, make take few minutes");
                    if (!isDry()) {
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setLogger(getContext().getLogger()).setCopyOver(true));
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

    public boolean copy(String installationDir, String targetDir, InstallationContext context) throws InterruptedException {

        addMessage("application server-Sanity", "info", "starting parameters sanity check");
        File target = new File(targetDir);
        Path targetPath = Paths.get(targetDir);
        Path sourcePath = Paths.get(installationDir);
        File sourceFile = new File(installationDir);
        if (sourceFile.exists()) {
            if (!isDry()) {
                if (!target.exists()) {
                    target.mkdirs();
                    info("Folder : " + targetPath + " was created");
                } else {
                    info("folder :" + targetPath + " already exists");
                }
                try {
                    CopyFileVisitor copyFileVisitor = null;

                    if (!isDry()) {
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setLogger(context.getLogger()).setCopyOver(true));
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
                addMessage("Edit file", "severe", "Cannot find the file: " + path + " for editing");
            }
            return null;
        }
        info("[Edit file] file " + path + " exists");
        toReplace = toReplace.replace("\\", "/");
        toFind = toFind.replace("\\", "/");
        String fileAsString = existingString;
        if (existingString == null) {
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

                if (reverseSlash) {
                    fileAsString = fileAsString.replaceAll("/", "\\");
                }
                is.close();
            } catch (IOException e) {
                severe("Error while reading file", e);
            }
        }
        if (fileAsString != null) {
            info("[Edit file] [Edit file]  file as string is not null");
            if (!fileAsString.contains(toFind)) {
                info("[Edit file] [Edit file]  file as string doesn't contain: " + toFind);
                return null;
            }
            fileAsString = fileAsString.replaceAll(toFind, toReplace);
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
}
