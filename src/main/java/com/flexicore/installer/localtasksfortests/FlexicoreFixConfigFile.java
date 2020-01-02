package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * fix flexicore configuration file to match tru locations.
 * 29-oct-2019
 */
@Extension
public class FlexicoreFixConfigFile extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };

    public FlexicoreFixConfigFile(Map<String, IInstallationTask> installationTasks) {
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


            String flexicoreHome = getFlexicoreHome();
            if (!dry) {
                editFile(flexicoreHome + "/flexicore.config", null, "/home/flexicore/", flexicoreHome + "/", false, false, true);

            }


        } catch (Exception e) {
            error("Error while configuring flexicore", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux,OperatingSystem.Windows};
    }
    @Override
    public String getName() {
        return "Adjust flexicore.config";
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
    public String getDescription() {
        return "Fixing the flexicore.config file to have all paths corrected)";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}