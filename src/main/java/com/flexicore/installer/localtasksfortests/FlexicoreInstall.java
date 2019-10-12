package com.flexicore.installer.localtasksfortests;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.Extension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public FlexicoreInstall(Map<String, IInstallationTask> installationTasks) {
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
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        super.install(installationContext);

        try {
            String flexicoreHome = getFlexicoreHome();
            boolean isUpdate = new File(flexicoreHome).exists();
            boolean backupprevious = getContext().getParamaters().getBooleanValue("backupprevious");
            boolean deleteplugins = getContext().getParamaters().getBooleanValue("deleteplugins");
            String flexicoreSource = getServerPath() + "/flexicore";
            info("Flexicore folder exists :" + isUpdate + " at: " + flexicoreHome);
            if (!isDry()) {
                if (isUpdate) {
                    if (backupprevious) {
                        zipAll(flexicoreHome,flexicoreHome+"/backup.zip",getContext());
                    }
                    try {
                        Pair<List<String>, List<String>> existingFiles = getComponents(flexicoreSource, null);

                        for (String dir : existingFiles.getLeft()) {
                            if (deleteplugins) {
                                deleteDirectoryStream(flexicoreHome + "/" + dir);
                            }
                            copy(flexicoreSource + "/" + dir, flexicoreHome + "/" + dir, installationContext);

                        }
                        for (String file : existingFiles.getRight()) {
                            if (deleteplugins) {
                                Files.deleteIfExists(Paths.get(flexicoreHome + "/" + file));
                            }
                            Files.copy(Paths.get(flexicoreSource + "/" + file),Paths.get(flexicoreHome + "/" + file), StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);

                        }

                    } catch (IOException e) {
                        severe("Error while deleting entities ", e);
                    }

                } else {

                    if (copy(flexicoreSource, flexicoreHome, installationContext)) {
                        info("Have successfully copied  " + flexicoreSource + " to " + flexicoreHome);

                    }
                }
            }


        } catch (Exception e) {
            error("Error while installing Flexicore", e);
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
    public String getDescription() {
        return " Installation for Flexicore files and plugins ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }

}
