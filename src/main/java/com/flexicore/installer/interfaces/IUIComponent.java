package com.flexicore.installer.interfaces;
import com.flexicore.installer.model.InstallationContext;
import org.pf4j.ExtensionPoint;

public interface IUIComponent extends ExtensionPoint {
    public void setContext(InstallationContext context);
    public boolean isAutoStart();
}
