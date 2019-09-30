package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Itamar parameters for installation
 */
@Extension
public class ItamarParameters  extends InstallationTask{
    static Logger logger;


    static Parameter[] preDefined = {
            new Parameter("itamarsource", "source of all itamar plugins and files", true,  "&serverpath"+ "/itamar"),
            new Parameter("deleteplugins", "delete all plugins before copying", true,  "false"),
            new Parameter("backupprevious", "backup previous plugins", true,  "true")

    };

    public ItamarParameters(Map<String, IInstallationTask> installationTasks) {
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
        return "itamar-parameters";
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");
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
