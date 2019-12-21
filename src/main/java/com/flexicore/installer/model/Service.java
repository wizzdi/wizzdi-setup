package com.flexicore.installer.model;

import java.time.LocalDateTime;

public class Service {
    String serviceId;
    String name;
    String Description;
    LocalDateTime lastChecked;
    LocalDateTime runningFrom;
    boolean running;

    public String getServiceId() {
        return serviceId;
    }

    public Service setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Service setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return Description;
    }

    public Service setDescription(String description) {
        Description = description;
        return this;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked;
    }

    public Service setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
        return this;
    }

    public LocalDateTime getRunningFrom() {
        return runningFrom;
    }

    public Service setRunningFrom(LocalDateTime runningFrom) {
        this.runningFrom = runningFrom;
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    public Service setRunning(boolean running) {
        this.running = running;
        return this;
    }
}
