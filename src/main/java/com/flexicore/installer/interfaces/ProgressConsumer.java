package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationStatus;

public interface ProgressConsumer {
    boolean updateProgress(IInstallationTask task, InstallationContext context);
    boolean inform(IInstallationTask task, InstallationContext context, String message);
}
