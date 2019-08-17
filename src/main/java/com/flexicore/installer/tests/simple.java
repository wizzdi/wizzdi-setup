package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;

import java.util.Set;

public class simple extends InstallationTask implements IInstallationTask {

   static Parameter[] preDefined={
            new Parameter("targetpath","description",true,"c:\\test5"),
            new Parameter("sourcepath","description",true,"c:\\test5"),
            new Parameter("remoteip","description",true,"c:\\test5"),
            new Parameter("localip","description",true,"c:\\test5")
     };

    public static Parameters getPrivateParameters() {
        Parameters result=new Parameters();
        for (Parameter parameter:preDefined) {
            result.addParameter(parameter);
        }
        return result;

    }
    @Override
    public InstallationResult install(InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public String getId() {
        return "simple-demo";
    }

    @Override
    public String getInstallerDescription() {
        return "This task is only for testing";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        return null;
    }

    @Override
    public Parameters getParameters() {
        return getPrivateParameters();
    }
}
