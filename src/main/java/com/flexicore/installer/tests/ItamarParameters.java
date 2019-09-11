package com.flexicore.installer.tests;

import com.flexicore.installer.model.*;

import java.util.logging.Logger;

public class ItamarParameters  extends InstallationTask{
    static Logger logger;


    static Parameter[] preDefined = {
          //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };
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
    public String getInstallerDescription() {
        return "This component is used to define the parameters for the Itamar software installation (configuration etc.)";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }



}
