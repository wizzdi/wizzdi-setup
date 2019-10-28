package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install Auto-SSH , AUTO-SSH provides access through the cloud into units behind NAT
 */
@Extension
public class AutosshInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {


    };

    public AutosshInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
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
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux};
    }
    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }
    //TODO:make sure the correct script ConfigureAutoSSH is placed in scripts folder
    //QA:test that remote server can be url
    private final String CONFIGURATION_SCRIPT_FILE="AutoSSHConfigure.sh";
    private final String SERVICEFILE="auto-ssh.service";
    private final String INSTALLSCRIPTFILE ="installAutoSSH.sh";
    private final String MAKEPASSWORDSCRIPTFILE ="makesshpass";
    @Override
    public String getName() {
        return "Autossh";
    }
    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        InstallationResult result = null;
        if ((result = super.install(installationContext)).equals(InstallationStatus.DRY)) return result;

            try {
                boolean executescriptresult = executeBashScriptLocal(MAKEPASSWORDSCRIPTFILE, "", "make ssh password"); // return values seems wrong, ignore
                simpleMessage("auto ssh", "info", "&&&&&&&&& have created a password for AutoSSH ,success= " + executescriptresult);
                executescriptresult &= executeBashScriptLocal(INSTALLSCRIPTFILE, "", "Install AutoSSH");
                simpleMessage("auto ssh", "info", "&&&&&&&&& have installed  Auto SSH ,success= " + executescriptresult);
                //replace the ip inside configureAutoSSH.sh with the correct ip/url of the remote server

                editFile(getScriptsPath()+"/"+CONFIGURATION_SCRIPT_FILE,"",
                        "x.x.x.x",getContext().getParamaters().getParameter("autossh-server").getValue(),
                        false,false,true);
                //configure auto-ssh
                executescriptresult &= executeBashScriptLocal("newConfigure.sh", "", "configure AutoSSH");
                //edit the service file.

               String temp= editFile(getScriptsPath()+"/"+SERVICEFILE,"",
                        "x.x.x.x",getContext().getParamaters().getParameter("autossh-server").getValue(),
                        false,false,false);
                editFile(getScriptsPath()+"/"+SERVICEFILE,temp,
                        "yyyy",getContext().getParamaters().getParameter("autossh-port").getValue(),
                        false,false,true);
                Files.copy(Paths.get(getScriptsPath()+"/"+SERVICEFILE), Paths.get("/etc/systemd/system/"+SERVICEFILE));
                Files.createDirectory(Paths.get("/var/run/autossh"));
                setOwner("autossh","autossh",Paths.get("/var/run/autossh"));
                if (startEnableService("auto-ssh")) {
                    info ("Have installed and started auto-ssh");
                }else {
                    severe("Could not complete auto-ssh installation");
                }














            } catch (Exception e) {
                error("Error while configuring flexicore", e);
                return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
            }


        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);


    }

    @Override
    public String getId() {
        return "autossh";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("autossh-parameters");
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

}
