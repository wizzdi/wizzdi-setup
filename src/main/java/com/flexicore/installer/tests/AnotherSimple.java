package com.flexicore.installer.tests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;

import java.util.HashSet;
import java.util.Set;

public class AnotherSimple extends InstallationTask  {
    /**
     * an
     */
    static Parameter[] preDefined = {
            new Parameter("test5", "description 1 ", true, "1"),
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
    public boolean enabled() {
        return true;
    }
    @Override
    public InstallationResult install(InstallationContext installationContext) {
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
    }

    @Override
    public String getId() {
        return "another-simple-demo";
    }

    @Override
    public String getInstallerDescription() {
        return "This task is only for testing another simple";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();

        result.add("prerequisite 1");
        result.add("prerequisite 2");
        return result;
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        return getPrivateParameters();
    }
}
