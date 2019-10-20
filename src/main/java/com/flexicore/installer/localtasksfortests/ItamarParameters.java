package com.flexicore.installer.localtasksfortests;

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
            new Parameter("itamarsource", "source of all itamar plugins and files", true,  "&serverpath"+ "/itamar",ParameterType.FOLDER),
            new Parameter("itamardeleteplugins", "delete all plugins before copying, if set to false the FlexicoreUniquenessEnforcer must be run", true,  "false",ParameterType.BOOLEAN),
            new Parameter("itamarbackupprevious", "backup previous plugins", true,  "true",ParameterType.BOOLEAN)

    };

    public ItamarParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }


    public  Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter,this);
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
    public String getName() {
        return "Itamar installer parameters";
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
    public String getDescription() {
        return "This component is used to define the parameters for the Itamar software installation (configuration etc.)";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }



}
