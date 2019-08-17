package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationResult;
import com.flexicore.installer.model.Parameters;
import org.pf4j.ExtensionPoint;

import java.util.Set;


public interface IInstallationTask extends ExtensionPoint {
    /**
     *
     * @param installationContext
     * @return
     */
    InstallationResult install(InstallationContext installationContext);

    /**
     *  ID must be unique across all plugins
     * @return
     */
    String getId();

    /**
     * get a description of installer plug-in
     * @return
     */
    String getInstallerDescription();

    /**
     * get a list of plugins that must run before this one runs.
     * @return
     */
    Set<String> getPrerequisitesTask();

    /**
     *
     * @return Parmeters
     */
    Parameters getParameters();

}
