package com.flexicore.installer.model;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.StreamGobbler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
        boolean wildflyInstall = false;
        if (command.endsWith("wildfly-wizzdi-install.sh")) {
            info("------------- " + command);
            wildflyInstall = true;
        }
        Process process = null;

        try {
            if (!isWIndows) {
                String[] cmd = {"/bin/bash", "-c", "echo \"" + context.getParamaters().getValue("sudopassword") + "\" | sudo -S " + command};

                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(command);
            }


            Boolean result = contWithProcess(process, toFind, false, ownerName);
            if (wildflyInstall) {
                info(" result was " + result);
                debuglines(command, false);
            }
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

    /**
     * copy complete tree recursively , create folders on the fly
     *
     * @param installationDir
     * @param targetDir
     * @return
     * @throws InterruptedException
     */
    boolean copy(String installationDir, String targetDir, String ownerName) throws InterruptedException {
        info("copying" + ownerName + " from: " + installationDir + " to: " + targetDir);

        File target = new File(targetDir);
        Path targetPath = Paths.get(targetDir);
        Path sourcePath = Paths.get(installationDir);
        File sourceFile = new File(installationDir);
        if (sourceFile.exists()) {
            if (!context.getParamaters().getBooleanValue("dry")) {
                if (!target.exists()) {
                    target.mkdirs();
                    info("Folder : " + targetPath + " was created");
                } else {
                    info("folder :" + targetPath + " already exists");
                }
                try {
                    CopyFileVisitor copyFileVisitor = null;

                    if (!context.getParamaters().getBooleanValue("dry")) {
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setLogger(context.getLogger()).setCopyOver(true));
                    }


                } catch (IOException e) {
                    error("Error while moving " + ownerName + "  :", e);
                    return false;
                }
                info("Have copied " + ownerName + " from :" + sourcePath + " to: " + targetPath);

            }
        } else {

            severe("Cannot move " + ownerName + ", path: " + sourcePath + " cannot be found");
        }


        return true;
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

    public InstallationContext getContext() {
        return context;
    }

    public InstallationTask setContext() {
        this.context = context;
        return this;
    }
}
