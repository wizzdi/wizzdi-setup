package com.flexicore.installer.model;

public class InstallationResult {

    private InstallationStatus installationStatus;
    private String message;
    private String taskid;
    private UserAction userAction;

    public InstallationStatus getInstallationStatus() {
        return installationStatus;
    }

    public <T extends InstallationResult> T setInstallationStatus(InstallationStatus installationStatus) {
        this.installationStatus = installationStatus;
        return (T) this;
    }

    public String getMessage() {
        return message;
    }

    public InstallationResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getTaskid() {
        return taskid;
    }

    public InstallationResult setTaskid(String taskid) {
        this.taskid = taskid;
        return this;
    }

    @Override
    public String toString() {
        return "InstallationResult{" +
                "installationStatus=" + installationStatus +
                '}';
    }

    public UserAction getUserAction() {
        return userAction;
    }

    public InstallationResult setUserAction(UserAction userAction) {
        this.userAction = userAction;
        return this;
    }
}
