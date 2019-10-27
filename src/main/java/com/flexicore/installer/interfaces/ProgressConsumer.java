package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationStatus;

public interface ProgressConsumer {
    void updateProgress(IInstallationTask task, InstallationContext context, double progress, InstallationStatus status);

}
