package com.flexicore.installer.tests;

import com.flexicore.installer.model.*;
import com.flexicore.installer.utilities.CopyFileVisitor;
import com.flexicore.installer.utilities.Utilities;
import org.pf4j.Extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Extension
public class WildflyInstall extends InstallationTask {
    static Logger logger;
    public static String[] toDeleteOnUpdate = {"itamar-app/", "entities/", "plugins", "wildfly/standalone/deployments", "wildfly/standalone/log"};
    public static  String[] toCopyOnUpdate = {"itamar-app", "Itamar Report", "entities", "plug-ins", "wildfly/standalone/deployments", "wildfly/standalone/log"};
    static Parameter[] preDefined = {
            new Parameter("heapsize", "Heap size for Wildfly application server", true, "768")
    };
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String > result=new HashSet<>();
        result.add("common-parameters");
        result.add("wildfly-parameters");

        return result;
    }
    @Override
    public boolean enabled() {
        return true;
    }
    public static Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }
    @Override
    public InstallationResult install (InstallationContext installationContext) {
        try {

            if (!installationContext.getParamaters().getBooleanValue("dry")) {
                boolean serviceRunning = testServiceRunning("wildfly", "Wildfly installation");

                if (serviceRunning && getContext().getParamaters().getBooleanValue("force")) {


                    boolean stoppedSucceeded = setServiceToStop("wildfly", "Wildfly Installation");
                    if (stoppedSucceeded) {
                        addMessage("Wildfly Update", "info", "Have stopped service, no system restart will be required");
                    }


                    for (String delete : toDeleteOnUpdate) {
                        Path path = Paths.get(getTargetPath() + "/server/" + delete);
                        if (new File(path.toString()).exists()) {

                            try {
                                deleteDirectoryStream(path);
                                info("Deleted: " + path);
                            } catch (IOException e) {
                                error("Error while deleting: " + path.toString(), e);

                            }
                        }
                    } //loop of delete
                    addMessage("Wildfly Update", "info", "Have deleted old files");

                    for (String update : toCopyOnUpdate) {
                        Path path = Paths.get(getSourcePath() + "/server/" + update);
                        if (new File(path.toString()).exists()) {
                            String s, t;
                            copy(s = getSourcePath() + "/server/" + update, t = getTargetPath() + "/server/" + update, "Wildfly installer");
                            info(" Copied source: " + s + " to: " + t);
                        }

                    } //loop of copy
                    addMessage("Wildfly Update", "info", "Have copied new files");
                    if (serviceRunning && stoppedSucceeded) {
                        if (setServiceToStart("wildfly", "Wildfly Installation")) {
                            {
                                addMessage("Wildfly Update", "info", "Have started Wildfly service");
                            }
                        }
                    }
                    if (!stoppedSucceeded) {
                        addMessage("Wildfly Update", "info", "as the service wasn't stopped, a system restart is required or a manual wildfly service stop");
                    }

                } else {
                    //service not running

                    if (!isWIndows) {

                        executeBashScript(getInstallationPath() + "/wildfly-Install.sh", "success", "Wildfly installation");
                        String updatedWildfly = getInstallationPath() + "/updatedWildfly";
                        if (new File(updatedWildfly).exists()) {
                            if (executeCommand("cp -r " + getInstallationPath() + "/updatedWildfly/*" + "/opt/wildfly/", "", "wildflyinstallation")) {


                            } else {
                                severe("Failed to copy new wildfly files");
                            }
                        }

                    } else {

                        File movefile = new File(getTargetPath());

                        if (!movefile.exists()) {
                            addMessage("Wildfly installation", "info", "Clean installation , will move server files, it is faster but files from: " + getSourcePath() + " will disappear");
                            ensureTarget(getTargetPath());
                            File source = new File(getContext().getParamaters().getValue("absoluteserversource"));
                            File[] files = source.listFiles();
                            int directory = 0;
                            int directoryErrors = 0;
                            int fileErrors = 0;
                            int fileSuccess = 0;
                            for (File file : files) {
                                boolean isDirectory = file.isDirectory();
                                info(isDirectory ? "File: "+ file.getAbsolutePath()+" is directory":"File :"+file.getAbsolutePath()+" is a file");
                                boolean result = move(file.getAbsolutePath(), getTargetPath());

                                if (isDirectory) { //for some reason isDirectory wasn't working.
                                    if (result) {
                                        directory++;
                                    } else {
                                        directoryErrors++;
                                    }
                                } else {
                                    if (result) {
                                        fileSuccess++;
                                    }else {
                                        fileErrors++;
                                    }

                                }
                            }

                            addMessage("Wildfly installation", "info", directoryErrors == 0 ?
                                    "Have moved successfully " + directory + " folders " : "Have moved successfully "
                                    + directory + " folders , had errors in " + directoryErrors + " folders");
                            addMessage("Wildfly installation", "info", fileErrors == 0 ?
                                    "Have moved successfully " + directory + " files " : "Have moved successfully "
                                    + fileSuccess + " folders , had errors in " + fileErrors + " folders");



                        } else {
                            addMessage("Wildfly installation", "info", "Target folder exists, cannot use move to install Wildfly");
                            copy(getAbsoluteServerSource(), getTargetPath());
                        }
                        /**
                         * edit files to reflect the target location
                         */
                        if (isWIndows) {
                            Utilities.editFile(getWildflyHome() + "/bin/standalone.conf.bat", "", "/home/flexicore", getFlexicoreHome(), true, false, true);
                            Utilities.editFile(getFlexicoreHome() + "/flexicore.config","", "/home/flexicore", getFlexicoreHome(), true, false,true);
                            Utilities.editFile(getWildflyHome() + "/standalone/configuration/standalone.xml", "","/home/flexicore", getFlexicoreHome(),false,false,true);
                            Utilities.editFile(getFlexicoreHome() + "/itamar.config", "","/home/flexicore", getFlexicoreHome(),false,true,true);

                            addMessage("Wildfly-env vars", "info", " setting environment vars");
                            Map<String, String> env = System.getenv();

                            File file = new File(getTargetPath() + "/wildfly");

                            //todo: create itamar installer plugin
                            boolean result;
                            if (installationContext.getParamaters().getBooleanValue("dry")) {
                                result = executeCommandByRuntime("setx JBOSS_HOME \"" + file.getAbsolutePath() + "\"", "wildfly");
                                if (result) {
                                    addMessage("wildfly-env vars", "info", "Have set Application Server environment vars");
                                    info("have set wildfly home  folder: " + getTargetPath());

                                } else {
                                    addMessage("wildfly-env vars", "error", "JBOSS_HOME cannot be set");
                                    severe("cannot set wildfly home  folder: " + getTargetPath() + " doesn't exist");
                                }
                            }


                        } else {
                            addMessage("wildfly-env vars", "error", "JBOSS_HOME cannot be set, target not found");
                            severe("cannot set wildfly home  folder: " + getTargetPath() + " doesn't exist");

                        }
                    }
                }
            }

        } catch (Exception e) {
            error("Error while installing Wildfly", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }

    boolean executeCommandByRuntime(String target, String ownerName) {
        return executeCommand(target, "", ownerName);

    }


    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }


    @Override
    public String getId() {
        return "wildfly-install";
    }

    @Override
    public String getInstallerDescription() {
        return "This component is used to install the Wildfly Application Server from Red Hat. ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

    /**
     * where the source of files is located (for copying)
     * @return
     */
    private String getSourcePath() {
        return  getContext().getParamaters().getValue("sourcepath");
    }

    /**
     * get the target server installation , this is usually c:\server in Windows and not relevant under Linux
     * @return
     */
    private String getTargetPath() {
        return  getContext().getParamaters().getValue("targetpath");
    }
    private boolean isDry() {
        return getContext().getParamaters().getBooleanValue("dry");
    }

    /**
     * get
     * @return
     */
    private String getFlexicoreHome() {
        return  getContext().getParamaters().getValue("flexicorehome");
    }
    private String getWildflyHome() {
        return  getContext().getParamaters().getValue("wildflyhome");
    }
    private String getAbsoluteServerSource() {
        return  getContext().getParamaters().getValue("sourcepath")+"/wildfly";
    }
    private String getInstallationPath() {
        return  getContext().getParamaters().getValue("installlations");
    }
    boolean copy(String installationDir, String targetDir) throws InterruptedException {

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
                        Files.walkFileTree(sourcePath, copyFileVisitor = new CopyFileVisitor(targetPath).setInstallationTask(this).setLogger(logger).setCopyOver(true));
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
}

