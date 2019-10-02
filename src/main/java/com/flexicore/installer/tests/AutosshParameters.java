package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 Parameters for AUTO SSH installation
 */
@Extension
public class AutosshParameters extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            new Parameter("autossh-server", "Auto SSH remote server, if not specified, defaults to IOT server remote URL, the server CANNOT be changed after installation", true, "&remote-server-url"),
            new Parameter("autossh-port", "should be a unique port on this server, the port can be changed after installation", true, "4444")

    };

    public AutosshParameters(Map<String, IInstallationTask> installationTasks) {
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
        return "autossh-parameters";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("installIOT");
         return result;
    }

    @Override
    public String getInstallerDescription() {
        return "Parameters for IOT installation ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
