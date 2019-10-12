package com.flexicore.installer.localtasksfortests;

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
    public String getId() {
        return "autossh-parameters";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("installIOT"); //we want to be able to use the URL of the main IOT server, however, this can be overridden by the user.
         return result;
    }

    @Override
    public String getDescription() {
        return "Parameters for IOT installation ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
