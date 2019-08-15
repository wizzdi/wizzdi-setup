package com.flexicore.installer.model;

public class InstallationResult {

    private InstallationStatus installationStatus;

    public InstallationStatus getInstallationStatus() {
        return installationStatus;
    }

    public <T extends InstallationResult> T setInstallationStatus(InstallationStatus installationStatus) {
        this.installationStatus = installationStatus;
        return (T) this;
    }

    @Override
    public String toString() {
        return "InstallationResult{" +
                "installationStatus=" + installationStatus +
                '}';
    }
}
