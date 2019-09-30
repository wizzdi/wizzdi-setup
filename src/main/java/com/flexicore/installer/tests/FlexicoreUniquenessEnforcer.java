package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
make sure there are now double entries in entities and plugins
 */
@Extension
public class FlexicoreUniquenessEnforcer extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
             new Parameter("ensureentities", "ensure now entities of the same type are installed", true,  "true"),
            new Parameter("ensureplugins", "ensure no plugins of the same type are installed, rules out multiple versions support", true,  "true")

    };

    /**
     * set here for easier testing (shorter code)
     *
     * @param installationTasks
     */
    public FlexicoreUniquenessEnforcer(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    public static Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
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
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);

        try {

            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!isDry()) {

            }


        } catch (Exception e) {
            error("Error while configuring flexicore", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }

    @Override
    public String getId() {
        return "flexicoreFixConfigFile";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");


        return result;
    }

    @Override
    public String getInstallerDescription() {
        return "Fixing the flexicore.config file to have all paths corrected)";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }
    @Override
    public boolean cleanup() {
        return true;
    }
}
