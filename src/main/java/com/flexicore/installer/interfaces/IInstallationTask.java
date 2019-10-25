package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.*;
import org.pf4j.ExtensionPoint;

import java.time.LocalDateTime;
import java.util.Set;


public interface IInstallationTask extends ExtensionPoint {

    InstallationResult install(InstallationContext installationContext) throws Throwable;
    int getOrder();
    IInstallationTask setOrder(int order);
    String getId();
    OperatingSystem[] getOperatingSystems();
    IInstallationTask setId(String id);

    String getName();

    String getDescription();

    IInstallationTask setDescription(String description);

    String getVersion();

    IInstallationTask setVersion(String version);

    Set<String> getPrerequisitesTask();

    InstallationStatus getStatus();

    Parameters getParameters(InstallationContext installationContext);

    boolean getEnabled();

    IInstallationTask setEnabled(boolean value);

    boolean cleanup();

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
