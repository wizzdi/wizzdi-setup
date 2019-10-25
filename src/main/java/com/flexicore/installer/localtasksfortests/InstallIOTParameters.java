package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 Parameters for IOT installation
 */
@Extension
public class InstallIOTParameters extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            new Parameter("activate-IOT", "if true, IOT access to remote server will be activated", true, "true",ParameterType.BOOLEAN),
            new Parameter("remote-server-url", "remote server URL, must be the URL of the remote server this unit is defined at", true, "default value"),
            new Parameter("remote-server-port", "example description", true, "80",ParameterType.NUMBER),
            new Parameter("remote-server-security", "example description", true, "true",ParameterType.BOOLEAN),
            new Parameter("remote-server-username", "example description", true, "admin@flexicore.com"),
            new Parameter("remote-server-password", "example description", true, "")

    };

    public InstallIOTParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux,OperatingSystem.Windows};
    }
    @Override
    public String getName() {
        return "IOT installation parameters";
    }

    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    public  Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter,this);
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

    @Override
    public InstallationResult install(InstallationContext installationContext) throws  Throwable{

        super.install(installationContext);

        try {

            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!isDry()) {
                editFile(flexicoreHome + "/flexicore.config", null, "/home/flexicore/", flexicoreHome + "/", false, false, true);

            }


        } catch (Exception e) {
            error("Error while configuring flexicore", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }

    @Override
    public String getId() {
        return "installIOTParameters";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");
         return result;
    }

    @Override
    public String getDescription() {
        return "Parameters for IOT installation ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
