package com.flexicore.installer.interfaces;
import com.flexicore.installer.model.InstallationContext;
import org.pf4j.ExtensionPoint;

public interface IUIComponent extends ExtensionPoint {
    void setContext(InstallationContext context);
    boolean isAutoStart();
    void start();
}
