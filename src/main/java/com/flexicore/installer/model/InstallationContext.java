package com.flexicore.installer.model;
import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.interfaces.IUIComponent;
import com.flexicore.installer.interfaces.ProgressConsumer;
import com.flexicore.installer.runner.Start;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
public class InstallationContext {

    private Logger logger;
    private Parameters parameters;
    private Properties properties;
    private LinkedHashMap<String,IInstallationTask> iInstallationTasks =new LinkedHashMap<>();
    private LinkedHashMap<String,IInstallationTask> cleanupTasks=new LinkedHashMap<>();
    private LinkedHashMap<String,InstallationResult> results=new LinkedHashMap<>();
    private List<IUIComponent> iuiComponents=new ArrayList<>();
    private Start.UIAccessInterfaceQuit uiQuit;
    private Start.UIAccessInterfaceInstallDry uiInstallDry;
    private Start.UIAccessInterfaceInstall uiInstall;
    private Start.UIAccessInterfacePause uiPause;
    private Start.UIAccessInterfaceResume uiResume;
    private Start.UIAccessInterfaceShowLogs uiShowLogs;
    private Start.UIAccessInterfaceStop uiStopInstall;
    private Start.UIAccessAbout uiAbout;
    private ProgressConsumer consumer;
    private int successFullyInstalled=0;
    private int failedToInstall=0;
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

    public Parameters getParamaters() {
        return parameters;
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

    public ProgressConsumer getConsumer() {
        return consumer;
    }

    public InstallationContext setConsumer(ProgressConsumer consumer) {
        this.consumer = consumer;
        return this;
    }
}


