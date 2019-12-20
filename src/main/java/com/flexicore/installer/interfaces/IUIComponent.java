package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.InstallationContext;
import org.pf4j.ExtensionPoint;

import java.util.Map;
import java.util.Set;

public interface IUIComponent extends ExtensionPoint {
    void setContext(InstallationContext context);
    boolean isAutoStart();
    boolean isShowing();
    boolean startAsynch();
    boolean startBlocking();
    boolean updateProgress(InstallationContext context,IInstallationTask task);
    boolean sendMessage(String message);
    String getVersion();
    boolean updateStatus(InstallationContext context, Map<String, Set<String>> additional, String message);

}
