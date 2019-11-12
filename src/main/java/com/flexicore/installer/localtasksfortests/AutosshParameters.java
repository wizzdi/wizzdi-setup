package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.io.File;
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
            new Parameter("autossh-port", "should be a unique port on this server, the port can be changed after installation", true, "4444",ParameterType.NUMBER,AutosshParameters::validateSSHPort)

    };

    public AutosshParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux};
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
    public String getName() {
        return "Autossh-parameters";
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

    /**
     * an example for special validation, can check against some server if the port is free, suggest another one if not.
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateSSHPort(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {


        return true;
    }

    /**
     * Validate if the remote server is  one of the available servers. we can ping the server from here.
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateSSHServer(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {

        return true;
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
