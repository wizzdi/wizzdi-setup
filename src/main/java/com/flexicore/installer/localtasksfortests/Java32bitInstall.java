package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * fix flexicore configuration file
 */
@Extension
public class Java32bitInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };

    public Java32bitInstall(Map<String, IInstallationTask> installationTasks) {
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
        return "java32";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("common-parameters");
        return result;
    }

    @Override
    public String getDescription() {
        return "Fixing the flexicore.config file to have all paths corrected)";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
