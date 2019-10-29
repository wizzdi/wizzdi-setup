package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install the Flexicore Deployment files inside wildfly/standalone/deployments
 * 29-oct-2019
 */
@Extension
public class FlexicoreDeploymentInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };

    public FlexicoreDeploymentInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }


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
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }

    @Override
    public String getName() {
        return "Flexicore Deployment";
    }

    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);

        try {

            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!isDry()) {
                String wildflyhome = isWindows ? installationContext.getParamaters().getValue("wildflyhome") : "/opt/wildfly/";
                File flexicore=new File(getServerPath()+"/FlexiCore.zip");
                File deployments=new File(wildflyhome+"wildfly/standalone/deployments");
                if (deployments.exists()) {

                    Path result = Files.copy(Paths.get(flexicore.getAbsolutePath())
                            , Paths.get(wildflyhome + "/standalone/FlexiCore.war.zip")
                            , StandardCopyOption.REPLACE_EXISTING);
                    deleteDirectoryStream(deployments.getAbsolutePath()+"/FlexiCore.war");
                    Files.deleteIfExists(Paths.get(deployments+"/FlexiCore.war.failed"));
                    Files.deleteIfExists(Paths.get(deployments+"/FlexiCore.war.undeployed"));
                    touch(new File(deployments.getAbsolutePath()+"/FlexiCore.war.dodeploy"));
                    ZipUtil.unpack(flexicore, deployments);
                    setOwnerFolder(Paths.get(deployments.getAbsolutePath()),"wildfly","wildfly");
                    return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
                }else {
                    severe("Wildfly deployments was not located on: "+deployments.getAbsolutePath());
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


    @Override
    public String getId() {
        return "flexicoredeployment";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("wildfly-install");
        return result;
    }

    @Override
    public String getDescription() {
        return "Install Flexicore inside Wildfly";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
