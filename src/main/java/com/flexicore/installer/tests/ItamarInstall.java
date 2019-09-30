package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install Itamar specific plugins and configuration files.
 */
@Extension
public class ItamarInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {


    };

    public ItamarInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    @Override
    public boolean enabled() {
        return true;
    }
    public static Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
            logger.info("Got a default parameter: "+ parameter.toString());
        }

        return result;

    }
    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger=installationContext.getLogger();
        logger.info("Getting parameters for "+this.toString());
        return getPrivateParameters();
    }

    @Override
    public InstallationResult install (InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }
    @Override
    public String getId() {
        return "itamar-install";
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");
        result.add("itamar-parameters");


        return result;
    }
    @Override
    public String getInstallerDescription() {
        return "This component is used to define the parameters for the Itamar software installation (configuration etc.)";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }
}
