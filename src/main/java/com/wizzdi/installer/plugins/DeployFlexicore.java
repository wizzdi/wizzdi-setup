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
    static String serviceName = "wildfly";
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
    private boolean serviceRunning;

    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    @Override
    public Parameters getPrivateParameters(InstallationContext context) {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter, this);
            if (context.isExtraLogs()) logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters(installationContext);
    }
    File deployments;
    /**
     * this is the main installation entry point. Install starts here
     */
    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);
        try {
            serviceRunning = testServiceRunning(serviceName, "flexicore deploy", false);
            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!dry) {

                String wildflyhome = isWindows ? installationContext.getParamaters().getValue("wildflyhome") : "/opt/wildfly/";
                File flexicore = new File(getServerPath() + "/flexicore/FlexiCore.war.zip");
                 deployments = new File(wildflyhome + "/standalone/deployments");
                if (deployments.exists()) {
                    if (exists(flexicore.getAbsolutePath())) {
                        if (update) {
                            if (serviceRunning) stopWildfly("flexicore update",3000);
                            Path result = Files.copy(Paths.get(flexicore.getAbsolutePath())
                                    , Paths.get(wildflyhome + "/standalone/FlexiCore.war.zip")
                                    , StandardCopyOption.REPLACE_EXISTING);
                            setProgress(50);
                            try {
                                deleteDirectoryStream(deployments.getAbsolutePath() + "/FlexiCore.war");
                            } catch (Exception e) {
                                severe("error while deleting folder: " + deployments.getAbsolutePath() + "/FlexiCore.war");
                            }
                            File[] files = deployments.listFiles();
                            for (File file : files) {
                                String fileis = file.getAbsolutePath();
                                try {
                                    Files.deleteIfExists(Paths.get(fileis));
                                    info("Deleted "+file.getAbsolutePath());
                                } catch (Exception e) {
                                    severe("error while deleting file: " + file.getAbsolutePath());
                                }
                            }

                            ZipUtil.unpack(flexicore, deployments);
                            info("Unpacked Flexicore.war.zip");
                            setProgress(70);
                            if (!isWindows) {
                                setOwnerFolder(Paths.get(deployments.getAbsolutePath()), serviceName, serviceName);
                            }
                            touch(new File(deployments.getAbsolutePath() + "/FlexiCore.war.dodeploy")); //this should start deployment
                            files = deployments.listFiles();
                            for (File file : files) {
                                String fileis = file.getAbsolutePath();
                                info("Before finishing, file: "+fileis);
                            }


                            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                        }else {
                            updateProgress(installationContext,"WIll not update Flexicore as update option was not selected");
                            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                        }
                    } else {
                        severe("Wildfly deployments was not located on: " + deployments.getAbsolutePath());
                    }
                } else {
                    updateProgress(getContext(), "Cannot find Flexicore.war.zip at: " + flexicore.getAbsolutePath());
                    return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
                }


            } else {
                //todo: add verification on dry (like source available etc)
                return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
            }


        } catch (Exception e) {
            error("Error while installing flexicore deployment ", e);

        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);

    }

    private InstallationResult waitForDeployment(File deployments) {
        //was an update
        updateProgress(getContext(), "Waiting up to 200 seconds till Flexicore starts");
        DeployState state = waitForServertoDeploy(deployments.getAbsolutePath(), 200000);
        setProgress(97);
        switch (state) {
            case deployed:
               Service service= getNewService(true).setName("Flexicore").setDescription("Flexicore Framework");
                updateService(getContext(),service,this);
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
            case nodeployfile:
                updateProgress(getContext(), "did not find a deploy file in the deployment folder!!");
                break;
            default:

        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
    }

    private DeployState waitForServertoDeploy(String path, long timeout) {
        long start = System.currentTimeMillis();

        try {
            File file = new File(path);
            do {
                Thread.sleep(20);
                File[] files = file.listFiles();
                if (files.length==1) {
                    info ("Was waiting for deploy but there is only one file in deployemts: "+files[0]);
                    return DeployState.nodeployfile;
                }
                if (exists(path + "/flexicore.war.isdeploying")) continue;
                if (exists(path + "/flexicore.war.deployed")) return DeployState.deployed;
                if (exists(path + "/flexicore.war.undeployed")) return DeployState.undeployed;
                if (exists(path + "/flexicore.war.failed")) return DeployState.failed;

            } while ((System.currentTimeMillis() - start) < timeout);
            if (exists(path + "/flexicore.war.isdeploying")) return DeployState.isdeploying;
            if (exists(path + "/flexicore.war.dodeploy")) return DeployState.dodeploy;
        } catch (InterruptedException e) {
            info("Stopped while waiting");
        }
        return DeployState.undefined;
    }

    public enum DeployState {
        isdeploying, deployed, dodeploy, undeployed, failed, undefined,nodeployfile
    }

    /**
     * defines here what are the compatible operating systems for this installer plugin
     */
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public InstallationResult finalizeInstallation(InstallationContext installationContext) throws Throwable {
        info("Finalizer on deploy flexicore called");
        Thread.sleep(300); //allow for Wildfly to start
       if (testServiceRunning(serviceName,"Flexicore deploy",false)) {
           info("Deploy flexicore found service wildfly running");
           InstallationResult result = waitForDeployment(deployments);
           return result;
       }else {
           info("Deploy flexicore found service wildfly NOT running");
           updateProgress(installationContext,"Deployed, but wildfly service not running");
           return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
       }


    }

    @Override
    public InstallationResult unInstall(InstallationContext installationContext) throws Throwable {
       stopWildfly("FlexiCore deploy",3000);
        int result = deleteDirectoryContent(getDeployments());
        if (result >= 0) {
            updateProgress(installationContext, "Have deleted :" + result);
            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
        }else {
            updateProgress(installationContext, "No need to delete deployments, already deleted");
            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
        }
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
    public Set<String> getNeedRestartTasks() {
        Set<String> result = new HashSet<>();
        result.add("wildfly");
        return result;
    }

    @Override
    public IInstallationTask setDescription(String s) {
        return this;
    }


    @Override
    public int mergeParameters(InstallationContext installationContext) {
        return 0;
    }


}
