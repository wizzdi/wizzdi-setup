package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install IOT files and configuration
 */
@Extension
public class InstallIOT extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };

    public InstallIOT(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }


    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    public Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter, this);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }

    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public String getName() {
        return "IOT installer";
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }


    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);

        try {

            String flexicoreSource = getServerPath() + "/flexicore";
            String flexicoreHome = getFlexicoreHome();
            if (!dry) {
                String remoteServerSource = getContext().getParamaters().getValue("remoteServer-configuration-file-source");
                if (new File(remoteServerSource).exists()) {
                    Path remote = Files.copy(Paths.get(remoteServerSource), Paths.get(flexicoreHome + "/remoteServer.json"));
                    if (editFile(flexicoreHome + "/remoteServer.json", "installIOTParameters")) {
                        if (editFile(flexicoreHome + "/flexicore.config", "installIOTParameters")) {
                            info("Have successfully replaced parameters names with parameters values");
                        } else {
                            severe("");
                        }
                    } else {
                        severe("");
                    }
                } else {

                }
            }


        } catch (Exception e) {
            error("Error while configuring flexicore", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }



    @Override
    public String getId() {
        return "installIOT";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");
        return result;
    }


    @Override
    public String getDescription() {
        return "IOT installation, adding the required files  ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

    private static String setCorrectHttpPort(Parameter effected, Parameter effecting) {
        return "";
    }


    private static String fixhttp(Parameter effected, Parameter effecting) {
        return "";
    }

    /**
     * this is called by the installation runner.Start to fix parameters after a change. (UI or properties read)
     * @param installationContext
     * @return
     */
    @Override
    public int mergeParameters(InstallationContext installationContext) {
        Parameter useSSlParameter=installationContext.getParameter("remote-server-ssl");
        Parameter portParameter=installationContext.getParameter("remote-server-port");
        Parameter url=installationContext.getParameter("remote-server-url");
        Parameter wsurl=installationContext.getParameter( "remote-server-web-service-url");
        if (useSSlParameter!=null && portParameter !=null && url!=null && wsurl!=null) {
            int port=Integer.valueOf(portParameter.getValue());
            Boolean useSSl= Boolean.valueOf(useSSlParameter.getValue());
            if (useSSl) {
                if (!url.getValue().contains("https:")) {
                   url.setValue(url.getValue().replace("http:","https:"));
                }
                if (!wsurl.getValue().contains("wss:")) {
                    wsurl.setValue(wsurl.getValue().replace("ws:","wss:"));
                }
            }else {
                if (url.getValue().contains("https")) {
                    url.setValue(url.getValue().replace("https","http"));
                }
                if (url.getValue().contains("wss:")) {
                    wsurl.setValue(wsurl.getValue().replace("wss:","ws:"));
                }

            }
            int urlColonLoc=url.getValue().indexOf(":");
            int wsUrlColonLoc=url.getValue().indexOf(":");
            int urlColonLastLoc=url.getValue().lastIndexOf(":");
            int wsUrlColonLastLoc=wsurl.getValue().lastIndexOf(":");
            if (urlColonLoc==urlColonLastLoc) {
                //means that there is no port definition in the URL
                port=useSSl & port==80 ? 443: port==8080 & useSSl ? 8443 : port;
                if (port!=80 && port!=443) { //then we need to specify port
                    url.setValue(url.getValue().replace("/FlexiCore", ":" + String.valueOf(port) + "/FlexiCore"));
                }
            }else { //port is specified in the URL
                int slashLoc=url.getValue().substring(urlColonLastLoc).indexOf("/");
               String newUrl= url.getValue().substring(0,urlColonLastLoc)+String.valueOf(port)+url.getValue().substring(slashLoc);
            }
        }


        return 1;
    }
}
