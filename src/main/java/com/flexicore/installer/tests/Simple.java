package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;

import java.util.HashSet;
import java.util.Set;

public class Simple extends InstallationTask  {
    /**
     * this is just an example ho parameters should be defined.
     */
    static Parameter[] preDefined = {
            new Parameter("targetpath", "description 1 ", true, "1"),
            new Parameter("sourcepath", "description 2 ", true, "2"),
            new Parameter("remoteip", "description 3 ", true, "3"),
            new Parameter("localip", "description 3 ", true, "4"),
            new Parameter("dry", " this is a simple switch ", false, "false")

    };

    public static Parameters getPrivateParameters() {
        Parameters result = new Parameters();
        for (Parameter parameter : preDefined) {
            result.addParameter(parameter);
        }
        return result;

    }
    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        return getPrivateParameters();
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
    public boolean enabled() {
        return true;
    }
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("prerequisite 1");
        result.add("prerequisite 2");
        return result;
    }


}
