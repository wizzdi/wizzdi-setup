package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Extension
public class WildflyParameters extends InstallationTask {
    static Logger logger;
    static String currentFolder = System.getProperty("user.dir");
    static String parentFolder = new File(currentFolder).getParent();
    static Parameter[] preDefined = {
            new Parameter("heapsize", "Heap size for Wildfly application server", true, "768"),
            new Parameter("wildflysourcepath", "where to get wildfly files", true, "&serverpath"+"/wildfly"),
            new Parameter("wildflyhome", "where to put wildfly files", true, "&targetpath"+"/wildfly"),
            new Parameter("wildflymove", "if present will move wildfly folder from source and not copy, additional installations may fail",true,"false" )
    };

    public WildflyParameters(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }

    public static Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }
    @Override
    public InstallationResult install (InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }
    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String > result=new HashSet<>();
        result.add("common-parameters");
        result.add("flexicore-install"); //this s because we need to update the standalone.xml and standalone.bat.conf standalone.conf to point here
        return result;
    }
    @Override
    public boolean enabled() {
        return true;
    }
    @Override
    public String getId() {
        return "wildfly-parameters";
    }

    @Override
    public String getInstallerDescription() {
        return "This component is used to define preferred paths and other common parameters better globally defined. ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }


}
