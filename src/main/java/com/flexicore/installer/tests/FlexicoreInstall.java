package com.flexicore.installer.tests;

import com.flexicore.installer.model.*;
import org.pf4j.Extension;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * install the standard Flexicore system (plugins, configuration, entities etc)
 */
@Extension
public class FlexicoreInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
            //  new Parameter("example-key", "example description", true or false here (has value), "default value") //

    };
    @Override
    public boolean enabled() {
        return true;
    }

    /**
     * parameters are best provided by a different plugin
     * @return
     */
    public static Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
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

        super.install(installationContext);

        try {
            boolean isUpdate=new File(getFlexicoreHome()).exists();
            if (!isDry()) {
                if (isUpdate) {

                    ZipUtil.pack(new File(getFlexicoreHome()+"/entities"), new File(getFlexicoreHome()+"/entities.zip"));
                    if (!new File (getFlexicoreHome()+"/entities.zip").exists()) {
                        severe("Unable to create a zip file for entities");
                    }else {
                        info("have created a zip backup in "+getFlexicoreHome()+"/entities.zip");
                    }
                    zip(getFlexicoreHome()+"/entities",getFlexicoreHome()+"/entities.zip",installationContext);
                    zip(getFlexicoreHome()+"/plugins",getFlexicoreHome()+"/plugins.zip",installationContext);
                    zip(getFlexicoreHome()+"/flexicore.config",getFlexicoreHome()+"/flexicore.config.zip",installationContext);

                    try {
                        deleteDirectoryStream(getFlexicoreHome() + "/entities");
                        info("Have deleted entities");
                    } catch (IOException e) {
                        severe("Error while deleting entities ",e);
                    }
                    try {
                        deleteDirectoryStream(getFlexicoreHome() + "/plugins");
                    } catch (IOException e) {
                        severe("Error while deleting entities ",e);
                    }
                    try {
                        Files.delete(new File(getFlexicoreHome()+"/flexicore.config").toPath()); //todo: is it the best approach, should we leave the previous file?
                    } catch (IOException e) {
                        severe("Error while deleting flexicore config file ",e);
                    }
                }
            }




        } catch (Exception e) {
            error("Error while installing Wildfly", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }
    @Override
    public String getId() {
        return "flexicore-install";
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-parameters");


        return result;
    }
    @Override
    public String getInstallerDescription() {
        return "This component is used to define the parameters for the Itamar software installation (configuration etc.)";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }

}
