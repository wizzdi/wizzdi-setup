package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.apache.commons.validator.routines.UrlValidator;
import org.pf4j.Extension;

import java.util.*;
import java.util.logging.Logger;

/**
 Parameters for IOT installation
 */
@Extension
public class InstallIOTParameters extends InstallationTask {
    static Logger logger;

   /* {
        "name":"cloud_server",
            "description":"cloud_server_description",
            "enabled":true,
            "externalId":"cloud_server_external_id",
            "basePathApi":"api_url",
            "webSocketPath":"web_socket_url", //ssl too
            "username":"doralon@shekelonline.com",
            "password":"H5%Wz"
    }*/



    /*
    #Linux Config
        JWTSecondsValid=2592000
        PluginPath=/home/flexicore/plugins/
        EntitiesPath=/home/flexicore/entities/
        UploadPath=/home/flexicore/upload/
        UsersRootDirectory=/home/flexicore/users/
        OutputPath=/home/flexicore/upload/
        XmlPath=/home/flexicore/xml/
        SwaggerClientGenCLIPath=/home/flexicore/swaggerclientgenpath/
        SwaggerClientGenResourceFolder=/home/flexicore/swaggerresources/
        HttpActive=false
        firstRunFile=/home/flexicore/firstRun.txt
        iOTExternalId=TnuvAsk3-3521
        CONCURRENT_SYNC_JOBS=1
        MAX_MESSAGE_SIZE=50
        JOB_TIMEOUT=120000
        MAX_JOBS_PER_SERVER=1
    */
    /*
    {
        "name":"shekel-cloud",  //cloud_server
            "description":"shekel-cloud",
            "enabled":true,    -> activate-IOT
            "externalId":"shekel-cloud",
            "basePathApi":"https://shekelbrainweighmc.com/FlexiCore/rest", ->remote-server-url , remote-server-port
            "webSocketPath":"wss://shekelbrainweighmc.com/FlexiCore/iotWSGZIP",
            "username":"bays@smartsell.com",
            "password":"TgsDvwdZZ39tUC4FU6HJ"
    }
*/
    /**
     * configuration file for remoteServer should include the parametersnames as the current values
     */
    static List<String> sslOptions =new ArrayList<>();
    static Parameter[] preDefined = {
            new Parameter("remote-server-name", "Name of the remote server", true, "",ParameterType.STRING),
            new Parameter("remote-server-description", "Description of the remote server", true, "",ParameterType.STRING),
            new Parameter("activate-IOT", "if true, IOT access to remote server will be activated", true, "false",ParameterType.BOOLEAN),
            new Parameter("remote-server-externalid", "The unique remote server external id", true, "",ParameterType.STRING),
            new Parameter("remote-server-url", "remote server URL, must be the URL of the remote server this unit is defined at", true,
                    "",ParameterType.URL,Parameter::validateURL),
            new Parameter("remote-server-web-service-url", "remote server web service URL, must be the URL of the remote server this unit is defined at", true, "default value",ParameterType.URL,Parameter::validateURL),
            new Parameter("remote-server-port", "remote server port, normally 80", true, "80",ParameterType.NUMBER),
            new Parameter("remote-server-ssl", "remote server SSL support", true, "SSL",ParameterType.LIST, sslOptions),
            new Parameter("remote-server-username", "remote server username used to access the cloud", true, "admin@flexicore.com",ParameterType.EMAIL,Parameter::validateEmail),
            new Parameter("remote-server-password", "remote server password used to access the cloud", true, "",ParameterType.PASSWORD),
            new Parameter("remoteServer-configuration-file-source", "locate the remoteServer.json (name can be different) source file", true, "&serverpath/iot/remoteServer.json",ParameterType.FILE),


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
    public static boolean validateSSLOptions(Parameter parameter, ValidationMessage validationMessage) {
      return true;
    }
}
