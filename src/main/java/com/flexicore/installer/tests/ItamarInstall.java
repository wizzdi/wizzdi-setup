package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Install Itamar specific plugins and configuration files.
 */
@Extension
public class ItamarInstall extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {


    };

    public ItamarInstall(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    @Override
    public boolean enabled() {
        return true;
    }
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
    public InstallationResult install (InstallationContext installationContext) throws Throwable{
        boolean itamarbackupprevious= getContext().getParamaters().getBooleanValue("itamarbackupprevious");
        boolean itamardeleteplugins=getContext().getParamaters().getBooleanValue("itamardeleteplugins");
        String flexicoreHome = getFlexicoreHome();
        boolean isUpdate = new File(flexicoreHome).exists();
        String itamarsource = getContext().getParamaters().getValue("itamarsource");
        if (!isDry()) {
            if (isUpdate) {

                if(itamarbackupprevious) {
                    zip(flexicoreHome + "/entities", flexicoreHome + "/entities.zip", installationContext);
                    zip(flexicoreHome + "/plugins", flexicoreHome + "/plugins.zip", installationContext);
                    zip(flexicoreHome + "/flexicore.config", flexicoreHome + "/flexicore.config.zip", installationContext);
                }
                try {
                    if (itamardeleteplugins) {
                        deleteDirectoryStream(flexicoreHome + "/entities");
                    }
                    copy(itamarsource + "/entities", flexicoreHome + "/entities", installationContext);
                    info("Have deleted entities");
                } catch (IOException e) {
                    severe("Error while deleting entities ", e);
                }
                try {
                    if (itamardeleteplugins) {
                        deleteDirectoryStream(flexicoreHome + "/plugins");
                    }
                    copy(itamarsource + "/plugins", flexicoreHome + "/plugins", installationContext);
                } catch (IOException e) {
                    severe("Error while deleting entities ", e);
                }
                try {
                    File config=new File (flexicoreHome+"/flexicore.config");
                    if (config.exists()) {
                        Files.delete(new File(flexicoreHome + "/flexicore.config").toPath()); //todo: is it the best approach, should we leave the previous file?
                    }
                    Path path=Files.copy(new File(itamarsource+"/flexicore.config").toPath(),new File(flexicoreHome+"/flexicore.config").toPath());
                } catch (IOException e) {
                    severe("Error while deleting flexicore config file ", e);
                }
            } else {

                if (copy(itamarsource, flexicoreHome, installationContext)) {
                    info("Have successfully copied  " + itamarsource + " to " + flexicoreHome);

                }
            }
        }

        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }
    @Override
    public String getId() {
        return "itamar-install";
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("itamar-parameters");
        return result;
    }
    @Override
    public String getInstallerDescription() {
        return "This component is used to to install Itamar files";
    }
    @Override
    public String toString() {
        return "Installation task: "+this.getId();
    }
}
