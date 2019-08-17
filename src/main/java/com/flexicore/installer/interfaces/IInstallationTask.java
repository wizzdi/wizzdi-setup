package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationResult;
import org.pf4j.ExtensionPoint;

import java.util.Set;


public interface IInstallationTask extends ExtensionPoint {

    InstallationResult install(InstallationContext installationContext);
    String getId();
    Set<String> getPrerequisitesTask();

}
