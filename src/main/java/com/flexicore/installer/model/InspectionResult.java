package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;

/**
 * a class for describing pre-installation phase of an installation task
 */
public class InspectionResult {
    boolean skip; // if true, no need to show
    String message;
    IInstallationTask task;
    InspectionState inspectionState;

    public boolean isSkip() {
        return skip;
    }

    public InspectionResult setSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public InspectionResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public InspectionState getInspectionState() {
        return inspectionState;
    }

    public InspectionResult setInspectionState(InspectionState inspectionState) {
        this.inspectionState = inspectionState;
        return this;
    }

    public IInstallationTask getTask() {
        return task;
    }

    public InspectionResult setTask(IInstallationTask task) {
        this.task = task;
        return this;
    }
}
