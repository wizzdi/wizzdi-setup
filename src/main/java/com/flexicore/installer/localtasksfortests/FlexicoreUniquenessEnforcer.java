package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
make sure there are now double entries in entities and plugins, this is controllable by the two parameters-> ensureentities and ensureplugins
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
        return "flexicoreuniquenessenforcer";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");


        return result;
    }

    @Override
    public String getDescription() {
        return "Make sure that there are no double components by deleting previous once. using key names and versions, will not work with multiple versions environments ";
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
