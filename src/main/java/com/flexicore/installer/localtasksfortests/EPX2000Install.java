package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
//todo:fix
/**
 * Old Itamar software installation
 */
@Extension
public class EPX2000Install extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {


    };
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Windows};
    }
    /**
     * set here for easier testing (shorter code)
     *
     * @param installationTasks
     */
    public EPX2000Install(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }
    @Override
    public String getName() {
        return "EPX2000 Installation";
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
    public InstallationResult install (InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }
    @Override
    public String getId() {
        return "itamar-epx2000";
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("common-parameters");
       return result;
    }
    @Override
    public String getDescription() {
        return "This component is used to install old Itamar EPX2000 for RHI and other calculations)";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }

}
