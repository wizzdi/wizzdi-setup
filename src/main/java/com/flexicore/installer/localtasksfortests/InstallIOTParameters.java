package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.*;
import java.util.logging.Logger;

/**
 Parameters for IOT installation
 */
@Extension
public class InstallIOTParameters extends InstallationTask {
    static Logger logger;

    /**
     * configuration file for remoteServer should include the parametersnames as the current values
     */
    static ArrayList<String> sslOptions =new ArrayList<>();
    static ArrayList<String> servers=new ArrayList<>();
    static Parameter[] preDefined = {
            new Parameter("remote_server_name", "Name of the remote server", true, "",ParameterType.LIST,servers, Parameter::validateList),
            new Parameter("activate_IOT", "if true, IOT access to remote server will be activated", true, "false",ParameterType.BOOLEAN),
            new Parameter("remote_server_externalid", "The unique remote server external id", true, "",ParameterType.STRING),
            new Parameter("remote_server_url", "remote server URL, must be the URL of the remote server this unit is defined at", true,
                    "",ParameterType.URL,Parameter::validateURL, false),
            new Parameter("remote_server_web_service_url", "remote server web service URL, must be the URL of the remote server this unit is defined at", true, "default value",ParameterType.URL,Parameter::validateURL, false),
            new Parameter("remote_server_port", "remote server port, normally 80", true, "80",ParameterType.NUMBER),
            new Parameter("remote_server_ssl", "remote server SSL support", true, "SSL",ParameterType.LIST, sslOptions,Parameter::validateList),
            new Parameter("remote_server_username",
                    "remote server username used to access the cloud", true, "admin@flexicore.com",
                    ParameterType.EMAIL,Parameter::validateEmail, false),
            new Parameter("remote_server_password", "remote server password used to access the cloud", true, "",ParameterType.PASSWORD),
            new Parameter("remoteServer_configuration_file_source", "locate the remoteServer.json (name can be different) source file", true, "&serverpath/iot/remoteServer.json",ParameterType.FILE),


            // this part is inside flexicore.config
            new Parameter("CONCURRENT_SYNC_JOBS", "Concurrent Synchronization Jobs (incoming)", true, "1",ParameterType.NUMBER),
            new Parameter("MAX_JOBS_PER_SERVER", "Maximum synchronization jobs per remote end", true, "1",ParameterType.NUMBER),
            new Parameter("JOB_TIMEOUT", "Timeout in milliseconds till a job is pronounced failed", true, "120000",ParameterType.NUMBER),
            new Parameter("MAX_MESSAGE_SIZE", "Maximum Synchronization Message in kBytes", true, "50",ParameterType.NUMBER),
            new Parameter("CONCURRENT_SYNC_JOBS", "Concurrent Synchronization Jobs (incoming)", true, "1",ParameterType.NUMBER),
            new Parameter("iOTExternalId", "IOT external ID of this unit, must be unique and identical in remote server", true, "iOTExternalId",ParameterType.STRING),
    };

    public InstallIOTParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
        sslOptions.add("SSL");
        sslOptions.add("NO-SSL");
        servers.add("server 1");
        servers.add("server 2");
        servers.add("server 0");
        servers.add("server x");
        Collections.sort(servers);
        Collections.sort(sslOptions);

    }
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux,OperatingSystem.Windows};
    }
    @Override
    public String getName() {
        return "IOT installation parameters";
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
    public String getId() {
        return "installIOTParameters";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");
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
    public static boolean validateSSLOptions(Parameter parameter, Object newValue,ValidationMessage validationMessage) {
      return true;
    }
}
