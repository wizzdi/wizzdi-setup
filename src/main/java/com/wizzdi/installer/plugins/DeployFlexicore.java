package com.wizzdi.installer.plugins;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@Extension
/**
 * Deploys Flexicore.war into existing Wildfly Installation
 */
public class DeployFlexicore extends InstallationTask {
    static Logger logger;
    static String currentFolder = System.getProperty("user.dir");
    static String parentFolder = new File(currentFolder).getParent();
    static Parameter[] preDefined = {
            /* example **
            new Parameter("targetpath", "the target path to install this installation into", true, "/temp/target",ParameterType.FOLDER),
            new Parameter("serverpath", "where to get this installation files from (not alien components)",
            true, parentFolder + "/resources/server",ParameterType.FOLDER,Parameter::validateExistingFolder),
            new Parameter("instllationspath", "where to find alien components installation files, for example Java installation. This is more relevant for Windows", true, parentFolder + "/resources/installations",ParameterType.FOLDER,Parameter::validateExistingFolder),
            new Parameter("scriptspath", "where to find operating system scripts", true, parentFolder + "/scripts",ParameterType.FOLDER,Parameter::validateExistingFolder),

            new Parameter("dry", "If set (used) installation will run but nothing really installed", false, "false",ParameterType.BOOLEAN),
            };

             */

            /*
            do not create a constructor!!!
             */

    };

    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    public Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter, this);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }

    /**
     * this is the main installation entry point. Install starts here
     */
    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);
        try {
            boolean serviceRunning = testServiceRunning("wildfly", "flexicore deploy", false);
            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!dry) {
                if (!serviceRunning || update || force) {
                    String wildflyhome = isWindows ? installationContext.getParamaters().getValue("wildflyhome") : "/opt/wildfly/";
                    File flexicore = new File(getServerPath() + "/flexicore/FlexiCore.war.zip");
                    File deployments = new File(wildflyhome + "/standalone/deployments");
                    if (deployments.exists()) {
                        if (exists(flexicore.getAbsolutePath())) {
                            Path result = Files.copy(Paths.get(flexicore.getAbsolutePath())
                                    , Paths.get(wildflyhome + "/standalone/FlexiCore.war.zip")
                                    , StandardCopyOption.REPLACE_EXISTING);
                            try {
                                deleteDirectoryStream(deployments.getAbsolutePath() + "/FlexiCore.war");
                            }catch (Exception e) {
                                severe("error while deleting folder: "+deployments.getAbsolutePath()+"/FlexiCore.war");
                            }
                            File[] files=deployments.listFiles();
                            for (File file:files) {
                                String fileis=file.getAbsolutePath();
                                try {
                                Files.deleteIfExists(Paths.get(fileis));
                                }catch (Exception e) {
                                    severe("error while deleting file: "+file.getAbsolutePath());
                                }
                            }
                       ZipUtil.unpack(flexicore, deployments);

                            if (!isWindows) {
                                setOwnerFolder(Paths.get(deployments.getAbsolutePath()), "wildfly", "wildfly");
                            }
                            touch(new File(deployments.getAbsolutePath() + "/FlexiCore.war.dodeploy")); //this should start deployment
                            if (serviceRunning) {
                                //was an update
                                updateProgress(getContext(), "Waiting up to 200 seconds till Flexicore starts");
                                DeployState state = waitForServertoDeploy(deployments.getAbsolutePath(), 200000);
                                switch (state) {
                                    case deployed:
                                        updateProgress(getContext(), "Deployment succeeded");
                                        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                                    case failed:
                                        updateProgress(getContext(), "Deployment failed");
                                        break;
                                    case undeployed:
                                        updateProgress(getContext(), "Deployment undeployed, requires repeating update");
                                        break;

                                    case dodeploy:
                                        updateProgress(getContext(), "Deployment has not started");
                                        break;

                                    case undefined:
                                        updateProgress(getContext(), "Deployment undeployed");
                                        break;
                                    case isdeploying:
                                        updateProgress(getContext(), "Deployment is still deploying after 200 seconds");
                                        break;
                                    default:

                                }
                                return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
                            }
                            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                        } else {
                            severe("Wildfly deployments was not located on: " + deployments.getAbsolutePath());
                        }
                    } else {
                        updateProgress(getContext(), "Cannot find Flexicore.war.zip at: " + flexicore.getAbsolutePath());
                        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
                    }

                } else info("Will not install FlexiCore, service is running or update/force not specified");
            } else {
                //todo: add verification on dry (like source available etc)
                return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
            }


        } catch (Exception e) {
            error("Error while installing flexicore deployment ", e);

        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);

    }

    private DeployState waitForServertoDeploy(String path, long timeout) {
        long start = System.currentTimeMillis();

        try {
            File file = new File(path);
            do {
                File[] files=file.listFiles();
                int a=3;
                if (exists(path + "/flexicore.war.deployed")) {
                    info("redeployed in " + (System.currentTimeMillis() - start) + " milliseconds");
                    return DeployState.deployed;
                }
                if (exists(path + "/flexicore.war.undeployed")) return DeployState.undeployed;
                if (exists(path + "/flexicore.war.failed")) return DeployState.failed;
                Thread.sleep(20);
            } while ((System.currentTimeMillis() - start) < timeout);
            if (exists(path + "/flexicore.war.isdeploying")) return DeployState.isdeploying;
            if (exists(path + "/flexicore.war.dodeploy")) return DeployState.dodeploy;

        } catch (InterruptedException e) {
            info("Stopped while waiting");
        }
        return DeployState.undefined;
    }

    public enum DeployState {
        isdeploying, deployed, dodeploy, undeployed, failed, undefined
    }

    /**
     * defines here what are the compatible operating systems for this installer plugin
     */
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public String getName() {
        return "deploy-flexicore";
    }

    /**
     * this must be unique, we use here the artifact-id
     */
    @Override
    public String getId() {
        return "deploy-flexicore";
    }

    /**
     * get the ids of the prerequistes tasks
     */
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("Wildfly-installer"); //otherwise we cannot install deployment
        return result;
    }

    @Override
    public String getDescription() {
        return "Deploys Flexicore into existing Wildfly Installation";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }


    @Override
    public IInstallationTask setId(String s) {
        return this;
    }

    @Override
    public IInstallationTask setDescription(String s) {
        return this;
    }

    @Override
    public Set<String> getNeedRestartTasks() {
        Set<String> result = new HashSet<>();
        result.add("Wildfly-installer"); //service must be restarted
        return result;
    }

    @Override
    public int mergeParameters(InstallationContext installationContext) {
        return 0;
    }


}
