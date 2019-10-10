package com.flexicore.installer.localtasksfortests;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import com.flexicore.installer.utilities.Utilities;
import org.pf4j.Extension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Extension
public class WildflyInstall extends InstallationTask {
    static Logger logger;
    public static String[] toDeleteOnUpdate = {"itamar-app/", "entities/", "plugins", "wildfly/standalone/deployments", "wildfly/standalone/log"};
    public static String[] toCopyOnUpdate = {"itamar-app", "Itamar Report", "entities", "plug-ins", "wildfly/standalone/deployments", "wildfly/standalone/log"};
    static Parameter[] preDefined = {

    };

    public WildflyInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("wildfly-parameters");

        return result;
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
    public InstallationResult install(InstallationContext installationContext) throws Throwable{
        super.install(installationContext);
        try {

            if (!isDry()) {
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
                        Path path = Paths.get(getServerPath() + "/" + update);
                        if (new File(path.toString()).exists()) {
                            String s, t;
                            copy(s = getServerPath() + "/" + update, t = getFlexicoreHome() + "/" + update, "Wildfly installer");
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

                        boolean wildfly14exists = new File("/opt/wildfly-14.0.1.Final").exists();
                        // code for upgrading Wildfly
                       if (wildfly14exists) {
                            boolean result = false;
                            info("Will now remove old wildfly installation");
                            result = executeCommand("rm /opt/wildfly", "", "install wildfly");
                            info("result of removing symbolic link of factory installed wildfly: " + result);
                            result = executeCommand("rm -r /opt/wildfly-14.0.1.Final", "", "install wildfly");
                            info("result of removing wildfly-14.0.1.final : " + result);
                            result = executeCommand("cp /home/firefly/flexicore/installations/standalone-java11-32bit.conf /opt/wildfly-16.0.0.Final/bin/standalone.conf", "", "install wildfly");
                            info("result of copying the correct standalone.conf for Java 11, 32 bits " + result);
                        } else {
                            info("cannot find wildfly 14 on this device, not a new installation");
                        }
                       //installation in Linux (using an installation script)
                       if (!new File("/opt/wildfly").exists()) {
                           info("will run Linux wildfly installer from "+ getInstallationsPath()+"/wildfly-wizzdi-install.sh");
                            if (executeBashScriptLocal(getInstallationsPath()+"/wildfly-wizzdi-install.sh", "", "Wildfly install as a service")) {
                                simpleMessage("Wildfly installation", "info", "Have installed Wildfly ");
                                if (executeCommand("service wildfly stop", "", "Wildfly installation")) {
                                    simpleMessage("Wildfly installation", "info", "Have stopped Wildfly service");
                                } else {
                                    simpleMessage("Wildfly installation", "severe", "Have failed to stop Wildfly ");
                                }
                            } else {
                                simpleMessage("Wildfly installation", "severe", "Have failed to install Wildfly ");
                            }
                        } else {
                            info("Wildfly link found, not a fresh installation");
                        }


                    } else {

                        File movefile = new File(getWildflyHome());
                        boolean toMove=isMove();
                        if (!movefile.exists() && toMove) {
                            addMessage("Wildfly installation", "info", "Clean installation , will move server files, it is faster but files from: " + getServerPath() + " will disappear");
                            ensureTarget(getTargetPath());
                            File source = new File(getServerPath());
                            File[] files = source.listFiles();
                            int directory = 0;
                            int directoryErrors = 0;
                            int fileErrors = 0;
                            int fileSuccess = 0;
                            for (File file : files) {
                                boolean isDirectory = file.isDirectory();
                                info(isDirectory ? "File: " + file.getAbsolutePath() + " is directory" : "File :" + file.getAbsolutePath() + " is a file");
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
                                    } else {
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
                            copy(getWildflySource(), getWildflyHome(),installationContext);
                        }
                        /**
                         * edit files to reflect the target location
                         */
                        if (isWIndows) {
                            Utilities.editFile(getWildflyHome() + "/bin/standalone.conf.bat", "", "/home/flexicore", getFlexicoreHome(), true, false, true);
                            Utilities.editFile(getFlexicoreHome() + "/flexicore.config", "", "/home/flexicore", getFlexicoreHome(), true, false, true);
                            Utilities.editFile(getWildflyHome() + "/standalone/configuration/standalone.xml", "", "/home/flexicore", getFlexicoreHome(), false, false, true);
                            Utilities.editFile(getFlexicoreHome() + "/itamar.config", "", "/home/flexicore", getFlexicoreHome(), false, true, true);

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

    public boolean isMove() {
        return getContext().getParamaters().getBooleanValue("wildflymove");
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
     *
     * @return
     */



}

