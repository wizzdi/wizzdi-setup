package com.flexicore.installer.model;

public class WindowsInstalledService {
    String name;
    String status;
    String displayName;

    public String getName() {
        return name;
    }

    public WindowsInstalledService setName(String name) {
        this.name = name;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WindowsInstalledService setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WindowsInstalledService setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
}
