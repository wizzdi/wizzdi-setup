package com.flexicore.installer.model;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.interfaces.IUIComponent;
import com.flexicore.installer.interfaces.ProgressConsumer;
import com.flexicore.installer.runner.Start;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
public class InstallationContext {

    private Logger logger;
    private boolean extraLogs=false;
    private boolean helpRunning=false;
    private Parameters parameters;
    private Properties properties;
    private LinkedHashMap<String,IInstallationTask> iInstallationTasks =new LinkedHashMap<>();
    private HashMap<String,Service> services=new HashMap<>();
    private LinkedHashMap<String,IInstallationTask> cleanupTasks=new LinkedHashMap<>();
    private LinkedHashMap<String,InstallationResult> results=new LinkedHashMap<>();
    private List<IUIComponent> iuiComponents=new ArrayList<>();
    //the following are functional interfaces defined in Start class, can be considered as a modern form of a jump tables into functions
    private Start.UIAccessInterfaceQuit uiQuit;
    private Start.UIAccessInterfaceInstallDry uiInstallDry;
    private Start.UIAccessInterfaceInstall uiInstall;
    private Start.UIAccessInterfaceUnInstall uiUninstall;
    private Start.UIAccessInterfaceUpdate uiUpdate;
    private Start.UIAccessInterfacePause uiPause;
    private Start.UIAccessInterfaceResume uiResume;
    private Start.UIAccessInterfaceToggle uiToggle;
    private Start.UIAccessInterfaceShowLogs uiShowLogs;
    private Start.UIAccessInterfaceStop uiStopInstall;
    private Start.UIAccessAbout uiAbout;

    private Start.InstallerProgress installerProgress;
    private Start.UpdateService updateService;
    private Start.installerFilesProgress filesProgress;
    private Start.UpdateSingleComponent updateSingleComponent;
    private Start.ShowDialog uishowDialog;
    private ProgressConsumer consumer;
    private int successFullyInstalled=0;
    private int failedToInstall=0;
    private String mainTitle;
    private File iconFile;
    private File imageFile;
    public Logger getLogger() {
        return logger;
    }
    public void incSuccess() {
        successFullyInstalled++;
    }
    public void incFailures() {
        failedToInstall++;
    }
    public void addResult(IInstallationTask task,InstallationResult result) {
        results.put(task.getId(),result);
    }
    public void addUIComponents(List<IUIComponent> iuiComponents) {
       this.iuiComponents.addAll(iuiComponents);
    }
    public void addUIComponent(IUIComponent iuiComponent) {
        this.iuiComponents.add(iuiComponent);
    }
    public HashMap<String,InstallationResult> getResults() {
        return results;
    }

    public int getSuccessFullyInstalled() {
        return successFullyInstalled;
    }

    public int getFailedToInstall() {
        return failedToInstall;
    }
    private OperatingSystem operatingSystem;
    public Parameters getParamaters() {
        return parameters;
    }
    public Parameter getParameter(String key) {

        return parameters.getParameter(key);
    }
    public <T extends InstallationContext> T setLogger(Logger logger) {
        this.logger = logger;
        return (T) this;
    }

    public <T extends InstallationContext> T setParameters(Parameters parameters) {
        this.parameters = parameters;
        return (T) this;
    }

    public Properties getProperties() {
        return properties;
    }

    public InstallationContext setProperties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public Start.UIAccessInterfaceQuit getUiQuit() {
        return uiQuit;
    }

    public InstallationContext setUiQuit(Start.UIAccessInterfaceQuit uiQuit) {
        this.uiQuit = uiQuit;
        return this;
    }

    public Start.UIAccessInterfaceInstallDry getUiInstallDry() {
        return uiInstallDry;
    }

    public InstallationContext setUiInstallDry(Start.UIAccessInterfaceInstallDry uiInstallDry) {
        this.uiInstallDry = uiInstallDry;
        return this;
    }

    public Start.UIAccessInterfaceInstall getUiInstall() {
        return uiInstall;
    }

    public InstallationContext setUiInstall(Start.UIAccessInterfaceInstall uiInstall) {
        this.uiInstall = uiInstall;
        return this;
    }

    public Start.UIAccessInterfaceUnInstall getUiUninstall() {
        return uiUninstall;
    }

    public InstallationContext setUiUninstall(Start.UIAccessInterfaceUnInstall uiUninstall) {
        this.uiUninstall = uiUninstall;
        return this;
    }

    public Start.UIAccessInterfaceUpdate getUiUpdate() {
        return uiUpdate;
    }

    public InstallationContext setUiUpdate(Start.UIAccessInterfaceUpdate uiUpdate) {
        this.uiUpdate = uiUpdate;
        return this;
    }

    public Start.UIAccessInterfacePause getUiPause() {
        return uiPause;
    }

    public InstallationContext setUiPause(Start.UIAccessInterfacePause uiPause) {
        this.uiPause = uiPause;
        return this;
    }

    public Start.UIAccessInterfaceResume getUiResume() {
        return uiResume;
    }

    public InstallationContext setUiResume(Start.UIAccessInterfaceResume uiResume) {
        this.uiResume = uiResume;
        return this;
    }

    public Start.UIAccessInterfaceShowLogs getUiShowLogs() {
        return uiShowLogs;
    }

    public InstallationContext setUiShowLogs(Start.UIAccessInterfaceShowLogs uiShowLogs) {
        this.uiShowLogs = uiShowLogs;
        return this;
    }

    public void addTask(IInstallationTask task) {
        if (cleanupTasks.containsKey(task.getId()) || iInstallationTasks.containsKey(task.getId())) {
            logger.log(Level.SEVERE,"****************** Multiple task keys found for task: "+task.getId()+ "********************");
        }
        if (task.cleanup()) {
            cleanupTasks.put(task.getId(),task);
        }else {
            iInstallationTasks.put(task.getId(), task);
        }
    }
    public IInstallationTask getTask(String id) {
        IInstallationTask task=iInstallationTasks.get(id);
        if (task!=null) return task;
        return cleanupTasks.get(id);
    }

    public HashMap<String, IInstallationTask> getCleanupTasks() {
        return cleanupTasks;

    }
    public void clear () {
        iInstallationTasks.clear();
        cleanupTasks.clear();
    }
    public HashMap<String,IInstallationTask> getiInstallationTasks() {
        return iInstallationTasks;
    }

    public Start.UIAccessAbout getUiAbout() {
        return uiAbout;
    }

    public InstallationContext setUiAbout(Start.UIAccessAbout uiAbout) {
        this.uiAbout = uiAbout;
        return this;
    }

    public Start.UIAccessInterfaceStop getUiStopInstall() {
        return uiStopInstall;
    }

    public InstallationContext setUiStopInstall(Start.UIAccessInterfaceStop uiStopInstall) {
        this.uiStopInstall = uiStopInstall;
        return this;
    }

    public Start.InstallerProgress getInstallerProgress() {
        return installerProgress;
    }

    public InstallationContext setInstallerProgress(Start.InstallerProgress installerProgress) {
        this.installerProgress = installerProgress;
        return this;
    }

    public ProgressConsumer getConsumer() {
        return consumer;
    }

    public InstallationContext setConsumer(ProgressConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public InstallationContext setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public File getIconFile() {
        return iconFile;
    }

    public InstallationContext setIconFile(File iconFile) {
        this.iconFile = iconFile;
        return this;
    }

    public File getImageFile() {
        return imageFile;
    }

    public InstallationContext setImageFile(File imageFile) {
        this.imageFile = imageFile;
        return this;
    }

    /**
     * list of running/failed services following the installation.
     * The list can be amended by one of the installers or more likely, by a snooper.
     * @return
     */
    public HashMap<String, Service> getServices() {
        return services;
    }

    public InstallationContext setServices(HashMap<String, Service> services) {
        this.services = services;
        return this;
    }

    public Start.UpdateService getUpdateService() {
        return updateService;
    }

    public InstallationContext setUpdateService(Start.UpdateService uiUpdateService) {
        updateService=uiUpdateService;
        return this;
    }

    public Start.UIAccessInterfaceToggle getUiToggle() {
        return uiToggle;
    }

    public InstallationContext setUiToggle(Start.UIAccessInterfaceToggle uiToggle) {
        this.uiToggle = uiToggle;
        return this;
    }

    public Start.installerFilesProgress getFilesProgress() {
        return filesProgress;
    }

    public InstallationContext setFilesProgress(Start.installerFilesProgress filesProgress) {
        this.filesProgress = filesProgress;
        return this;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public InstallationContext setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        return this;
    }

    public boolean isExtraLogs() {
        return extraLogs;
    }

    public InstallationContext setExtraLogs(boolean extraLogs) {
        this.extraLogs = extraLogs;
        return this;
    }

    public boolean isHelpRunning() {
        return helpRunning;
    }

    public InstallationContext setHelpRunning(boolean helpRunning) {
        this.helpRunning = helpRunning;
        return this;
    }

    public Start.UpdateSingleComponent getUpdateSingleComponent() {
        return updateSingleComponent;
    }

    public InstallationContext setUpdateSingleComponent(Start.UpdateSingleComponent updateSingleComponent) {
        this.updateSingleComponent = updateSingleComponent;
        return this;
    }

    public Start.ShowDialog getUishowDialog() {
        return uishowDialog;
    }

    public InstallationContext setUishowDialog(Start.ShowDialog uishowDialog) {
        this.uishowDialog = uishowDialog;
        return this;
    }
}


