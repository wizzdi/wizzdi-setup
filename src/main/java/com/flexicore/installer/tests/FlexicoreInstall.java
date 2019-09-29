package com.flexicore.installer.tests;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public InstallationResult install(InstallationContext installationContext) {

        super.install(installationContext);

        try {
            String flexicoreHome = getFlexicoreHome();
            boolean isUpdate = new File(flexicoreHome).exists();
            String flexicoreSource = getServerPath() + "/flexicore";
            info("Flexicore folder exists :"+isUpdate +" at: "+flexicoreHome);

            if (!isDry()) {
                if (isUpdate) {

                    zip(flexicoreHome + "/entities", flexicoreHome + "/entities.zip", installationContext);
                    zip(flexicoreHome + "/plugins", flexicoreHome + "/plugins.zip", installationContext);
                    zip(flexicoreHome + "/flexicore.config", flexicoreHome + "/flexicore.config.zip", installationContext);
                    try {
                        deleteDirectoryStream(flexicoreHome + "/entities");
                        copy(flexicoreSource + "/entities", flexicoreHome + "/entities", installationContext);
                        info("Have deleted entities");
                    } catch (IOException e) {
                        severe("Error while deleting entities ", e);
                    }
                    try {
                        deleteDirectoryStream(flexicoreHome + "/plugins");
                        copy(flexicoreSource + "/plugins", flexicoreHome + "/plugins", installationContext);
                    } catch (IOException e) {
                        severe("Error while deleting entities ", e);
                    }
                    try {
                        File config=new File (flexicoreHome+"/flexicore.config");
                        if (config.exists()) {
                            Files.delete(new File(flexicoreHome + "/flexicore.config").toPath()); //todo: is it the best approach, should we leave the previous file?
                        }
                        Path path=Files.copy(new File(flexicoreSource+"/flexicore.config").toPath(),new File(flexicoreHome+"/flexicore.config").toPath());
                   } catch (IOException e) {
                        severe("Error while deleting flexicore config file ", e);
                    }
                } else {

                    if (copy(flexicoreSource, flexicoreHome, installationContext)) {
                        info("Have successfully copied  " + flexicoreSource + " to " + flexicoreHome);

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
        return " Installation for Flexicore files and plugins ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
