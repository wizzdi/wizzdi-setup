package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;
import java.util.Map;
import java.util.logging.Logger;

@Extension
public class CommonParameters extends InstallationTask {
    static Logger logger;
    static String currentFolder = System.getProperty("user.dir");
    static String parentFolder = "/temp";//new File(currentFolder).getParent();

    static Parameter[] preDefined = {
            new Parameter("targetpath", "the target path to install this installation into", true, "/temp/target",ParameterType.FOLDER),
            new Parameter("serverpath", "where to get this installation files from (not alien components)", true, parentFolder + "/resources/server",ParameterType.FOLDER),
            new Parameter("instllationspath", "where to find alien components installation files, for example Java installation. This is more relevant for Windows", true, parentFolder + "/resources/installations",ParameterType.FILE),
            new Parameter("dry", "If set (used) installation will run but nothing really installed", false, "false",ParameterType.BOOLEAN)


     };

    public CommonParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }



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
