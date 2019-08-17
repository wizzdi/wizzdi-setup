package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;
import org.apache.commons.cli.Options;

public class TaskWrapper {
    IInstallationTask task;
    Options options;

    public TaskWrapper(IInstallationTask task, Options options) {
        this.task=task;
        this.options=options;
    }

    public IInstallationTask getTask() {
        return task;
    }

    public TaskWrapper setTask(IInstallationTask task) {
        this.task = task;
        return this;
    }

    public Options getOptions() {
        return options;
    }

    public TaskWrapper setOptions(Options options) {
        this.options = options;
        return this;
    }
}
