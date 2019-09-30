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
    InstallationResult install(InstallationContext installationContext) throws Throwable;


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
     * @return Parameters
     */
    Parameters getParameters(InstallationContext installationContext);

    boolean enabled ();

    /**
     * if set true, will be moved to the end of the installations list, order between plugins having cleanup=true is maintained.
     * @return
     */
    boolean cleanup ();

}
