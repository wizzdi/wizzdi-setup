package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Extension
public class CommonParameters extends InstallationTask {
    static Logger logger;
    static String currentFolder = System.getProperty("user.dir");
    static String parentFolder = "/temp";//new File(currentFolder).getParent();
    static List<String> sslOptions =new ArrayList<>();

    static Parameter[] preDefined = {
            new Parameter("targetpath", "the target path to install this installation into", true, "/temp/target",ParameterType.FOLDER),
            new Parameter("serverpath", "where to get this installation files from (not alien components)",
            true, parentFolder + "/resources/server",ParameterType.FOLDER,Parameter::validateExistingFolder),
            new Parameter("testpath", "where to get this installation files from (not alien components)",
                    true,  "&serverpath",ParameterType.FOLDER,Parameter::validateExistingFolder,3),
            new Parameter("instllationspath", "where to find alien components installation files, for example Java installation. This is more relevant for Windows", true, parentFolder + "/resources/installations",ParameterType.FOLDER,Parameter::validateExistingFolder),
            new Parameter("scriptspath", "where to find operating system scripts", true, parentFolder + "/scripts",ParameterType.FOLDER,Parameter::validateExistingFolder),

            new Parameter("dry", "If set (used) installation will run but nothing really installed", false, "false",ParameterType.BOOLEAN),

            new Parameter("remote_server_name", "Name of the remote server", true, "",ParameterType.STRING),
            new Parameter("remote_server_description", "Description of the remote server", true, "",ParameterType.STRING),
            new Parameter("activate_IOT", "if true, IOT access to remote server will be activated", true, "false",ParameterType.BOOLEAN),
            new Parameter("remote_server_externalid", "The unique remote server external id", true, "",ParameterType.STRING),
            new Parameter("remote_server_url", "remote server URL, must be the URL of the remote server this unit is defined at", true,
                    "",ParameterType.URL,Parameter::validateURL),
            new Parameter("remote_server_web_service_url", "remote server web service URL, must be the URL of the remote server this unit is defined at", true, "default value",ParameterType.URL,Parameter::validateURL),
            new Parameter("remote_server_port", "remote server port, normally 80", true, "80",ParameterType.NUMBER,Parameter::validatePort),
            new Parameter("remote_server_ssl", "remote server SSL support", true, "SSL",ParameterType.LIST, sslOptions),
            new Parameter("remote_server_username",
                    "remote server username used to access the cloud", true, "admin@flexicore.com",
                    ParameterType.EMAIL,Parameter::validateEmail,2),
            new Parameter("remote_server_password", "remote server password used to access the cloud", true, "",ParameterType.PASSWORD,null,1),
            new Parameter("remoteServer_configuration_file_source", "locate the remoteServer.json (name can be different) source file", true, "&serverpath/iot/remoteServer.json",ParameterType.FILE,Parameter::validateExistingFile),



    };

    public CommonParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }



    public  Parameters getPrivateParameters() {
        initValues();
        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter,this);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }

    private void initValues() {
        String[] list={"ssl","nossl","test"};
        sslOptions.addAll(Arrays.asList(list));
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
        return "Common parameters";
    }
    @Override
    public InstallationResult install(InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public String getId() {
        return "common-parameters";
    }

    @Override
    public String getDescription() {
        return "This component is used to define preferred paths and other common parameters better globally defined. ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }


}
