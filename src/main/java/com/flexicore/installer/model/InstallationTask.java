package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.StreamGobbler;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class InstallationTask implements IInstallationTask {

    private InstallationContext context;
    public static Parameters getPrivateParameters() {
        return null;
    }

    final public static boolean isWIndows = (System.getProperty("os.name")).toLowerCase().contains("windows");
    Queue<String> lines = new ConcurrentLinkedQueue<String>();
    Queue<String> errorLines = new ConcurrentLinkedQueue<String>();


    public boolean testServiceRunning(String serviceName, String ownerName) {
        if (isWIndows) {

            return executeCommand("sc query " + serviceName, "RUNNING", "checking if service " + serviceName + " runs");
        } else {
            return executeCommand("service  " + serviceName + " status", "active", "checking if service " + serviceName + " runs");
        }
    }
    public boolean setServiceToAuto(String serviceName, String ownerName) {
        if (isWIndows) {
            return executeCommand("sc config " + serviceName + " start= auto", "success", "Set Service To Auto");
        }
        return  true;
    }
    public void addMessage(String phase, String severity, String message) {
        if (severity != "info") {
            severe(phase + "  " + message);
        } else {
            info(phase + "  " + message);
        }

    }
    public boolean setServiceToStop(String serviceName, String ownerName) {
        if (isWIndows) {
            return executeCommand("sc stop " + serviceName, "STOP_PENDING", "Set Service To Stop " + serviceName);
        }else {
            return executeCommand("service  " + serviceName+ " stop", "", "Set Service To Stop " +serviceName);
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
            if (!isWIndows) {
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
        if (context!=null) {
            if (context.getLogger() != null) context.getLogger().info(message);
        }
    }

    public void error(String message, Throwable e) {
        if (context!=null) {
            if (context.getLogger() != null) context.getLogger().log(Level.SEVERE, message, e);
        }
    }

    public void severe(String message, Throwable e) {

        error(message, e);
    }

    public void severe(String message) {
        if (context!=null) {
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
            if (!isWIndows && exitVal == 4) {
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


    @Override
    public InstallationResult install(InstallationContext installationContext) {
            context=installationContext;
          return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public String getId() {
        return "no-id";
    }

    @Override
    public String getInstallerDescription() {
        return "No description has been provided";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        return Collections.emptySet();
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        return new Parameters();
    }

    @Override
    public boolean enabled() {
        return true;
    }

    public InstallationContext getContext() {
        return context;
    }

    public InstallationTask setContext() {
        this.context = context;
        return this;
    }

    public void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
    public void deleteDirectoryStream(String path) throws IOException {
        deleteDirectoryStream(new File(path).toPath());

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
    public  boolean move(String source, String target) {
        boolean result = false;
        try {
            if (isWIndows) {
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
      added few methods for accessing common parameters although these are not known to the Installer.
     These parameters are common in every FC installation.
     */

    public String getTargetPath() {
        return  getContext().getParamaters().getValue("targetpath");

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
        return  getContext().getParamaters().getValue("flexicorehome");

    }

    public String getWildflyHome() {
        return  getContext().getParamaters().getValue("wildflyhome");

    }
    public String getServerPath() {
          return getContext().getParamaters().getValue("serverpath");
    }
    public String getAbsoluteServerSource() {
        return  getContext().getParamaters().getValue("sourcepath")+"/wildfly";

    }

    public String getInstallationPath() {
        return  getContext().getParamaters().getValue("installlations");

    }
    /**
     *
     * @param installationDir source of the copy
     * @param targetDir target of the copy
     * @param ownerName this is for logging purposes only
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

    public boolean copy(String installationDir, String targetDir,InstallationContext context) throws InterruptedException {

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
    public   boolean zip(String zipFolderName, String zipFileName, InstallationContext context) {
        File zipFile=null;
        File sourceFile=new File(zipFolderName);
        if (sourceFile.exists()) {
            try {
                ZipUtil.pack(new File(zipFolderName), zipFile = new File(zipFileName));
            } catch (Exception e) {
                severe("Error while zipping folder: "+zipFolderName,e);
            }
            if (zipFile.exists()) {
                context.getLogger().info(" Have zipped " + zipFolderName + " into: " + zipFileName);
                return true;
            } else {
                context.getLogger().log(Level.SEVERE, " Have failed to zip  " + zipFolderName + " into: " + zipFileName);

            }
        }else {
            context.getLogger().log(Level.SEVERE, " Have not found zip source folder  " + zipFolderName );
        }
        return false;

    }
    public boolean zipEntries (String[] fileEntries,String target,InstallationContext context) {
        boolean result;
        ZipEntrySource[] entries = new ZipEntrySource[fileEntries.length];
        int i=0;
        for (String entry:fileEntries) {
            entries[i]=new FileSource(entry, new File(entry));
        }

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(new File(target)));
            ZipUtil.addEntries(new File("/tmp/demo.zip"), entries, out);
            result=true;
        } catch (FileNotFoundException e) {
            context.getLogger().log(Level.SEVERE,"error while creating stream",e);
            result=false;
        } finally {
            IOUtils.closeQuietly(out);
        }
        return result;
    }
}
