package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install Auto-SSH , AUTO-SSH provides access through the cloud into units behind NAT
 */
@Extension
public class AutosshInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {


    };

    public AutosshInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
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
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux};
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
        return "Autossh";
    }
    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        InstallationResult result = null;
        if ((result = super.install(installationContext)).equals(InstallationStatus.DRY)) return result;
        if (!isWindows) {
            try {

            } catch (Exception e) {
                error("Error while configuring flexicore", e);
                return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
            }
        }

        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);


    }

    @Override
    public String getId() {
        return "autossh";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("autossh-parameters");
        return result;
    }

    @Override
    public String getDescription() {
        return "IOT installation, adding the required files  ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
