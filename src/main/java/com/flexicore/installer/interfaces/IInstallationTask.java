package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.*;
import org.pf4j.ExtensionPoint;

import java.time.LocalDateTime;
import java.util.Set;


public interface IInstallationTask extends ExtensionPoint {

    InstallationResult install(InstallationContext installationContext) throws Throwable;
    InstallationResult finalizeInstallation(InstallationContext installationContext) throws Throwable;
    int getOrder();
    IInstallationTask setOrder(int order);
    String getId();
    OperatingSystem[] getOperatingSystems();
    OperatingSystem getCurrentOperatingSystem();
    IInstallationTask setId(String id);
    boolean isAdmin();
    IInstallationTask setAdmin(boolean admin);
    String getName();

    String getDescription();

    IInstallationTask setDescription(String description);
    String getMessage();

    IInstallationTask setMessage(String message);

    String getVersion();

    IInstallationTask setVersion(String version);

    Set<String> getPrerequisitesTask();

    InstallationStatus getStatus();

    Parameters getParameters(InstallationContext installationContext);

    /**
     * once parameters are finally determined, before install, will check how parameter X of plugin Y may change the values
     * of parameter M in plugin N, for example change http to https.
     * @param installationContext
     * @return
     */
    int mergeParameters(InstallationContext installationContext);

    boolean isEnabled();

    IInstallationTask setEnabled(boolean value);
    boolean isStop();
    IInstallationTask setSTop(boolean value);
    boolean isSnooper();
    boolean cleanup();
    Service getService();
    IInstallationTask setService(Service service);
    LocalDateTime getStarted();

    IInstallationTask setStarted(LocalDateTime started);

    LocalDateTime getEnded();

    IInstallationTask setEnded(LocalDateTime ended);

    IInstallationTask setProgress(Integer progress);

    IInstallationTask setStatus(InstallationStatus status);

    IInstallationTask setName(String name);

    Integer getProgress();

    IInstallationTask setContext(InstallationContext context);

}
