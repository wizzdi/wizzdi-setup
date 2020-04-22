package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.*;
import org.pf4j.ExtensionPoint;

import java.time.LocalDateTime;
import java.util.Set;


public interface IInstallationTask extends ExtensionPoint {
    InspectionResult inspect(InstallationContext context);
    InstallationResult install(InstallationContext installationContext) throws Throwable;
    InstallationResult unInstall(InstallationContext installationContext) throws Throwable;
    InstallationResult update(InstallationContext installationContext) throws Throwable;
    InstallationResult finalizeInstallation(InstallationContext installationContext) throws Throwable;
    InstallationResult restartService(InstallationContext installationContext) throws Throwable;
    int getOrder();
    IInstallationTask setOrder(int order);
    String getId();
    OperatingSystem[] getOperatingSystems();
    OperatingSystem getCurrentOperatingSystem();
    IInstallationTask setId(String id);
    boolean isAdmin();
    boolean isFinalizerOnly();
    IInstallationTask setAdmin(boolean admin);
    String getName();


    String getDescription();

    IInstallationTask setDescription(String description);
    String getMessage();

    IInstallationTask setMessage(String message);

    String getVersion();

    IInstallationTask setVersion(String version);

    Set<String> getPrerequisitesTask();
    Set<String> getNeedRestartTasks();
    Set<String> getSoftPrerequisitesTask(); //added to support forced order between two tasks if both exist
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

    /**
     * get welcome message, usually only one plugin provides this.
     * @return array of UserMessage or null
     */
    UserMessage[] getWelcomeMessage();
    /**
     * get final message, usually only one plugin provides this.
     * @return array of UserMessage or null
     */
    UserMessage[] getFinalMessage();
    boolean cleanup();
    Service getService();
    IInstallationTask setService(Service service);
    LocalDateTime getStarted();
    boolean initialize(InstallationContext context);

    /**
     * allows one task to force refresh of data on another task
     * @param parameter
     * @return
     */
    boolean refreshData(InstallationContext installationContext,Parameter parameter);

    /**
     * connect this task parameters to changes in other parameters
     */
    void setSubscribers(InstallationContext installationContext);
    void parameterChanged(Parameter parameter);
    IInstallationTask setStarted(LocalDateTime started);
    boolean isWrongOS();
    LocalDateTime getEnded();

    IInstallationTask setEnded(LocalDateTime ended);

    IInstallationTask setProgress(Integer progress);

    IInstallationTask setStatus(InstallationStatus status);

    IInstallationTask setName(String name);

    Integer getProgress();

    IInstallationTask setContext(InstallationContext context);

}
