package com.flexicore.installer.tests;

import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * install the standard Flexicore system (plugins, configuration, entities etc)
 */
@Extension
public class ShekelComponentsParameters extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };

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
    public InstallationResult install(InstallationContext installationContext) {

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

}
