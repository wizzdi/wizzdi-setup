package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.*;
import org.pf4j.ExtensionPoint;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface IUIComponent extends ExtensionPoint {
    void setContext(InstallationContext context);
    boolean isAutoStart();
    boolean isShowing();
    boolean startAsynch(InstallationContext context);
    boolean startBlocking(InstallationContext context);
    boolean updateProgress(InstallationContext context,IInstallationTask task);
    boolean sendMessage(String message);
    boolean updateService(InstallationContext context, Service service, IInstallationTask task);
    String getVersion();
    boolean updateStatus(InstallationContext context, Map<String, Set<String>> additional, String message, InstallationState state);

    /**
     * notify optional UI on Installation Tasks having some issues (already installed, different version installed).
     * @param context
     * @param inspections
     */
    void handleInspections(InstallationContext context, ArrayList<InspectionResult> inspections);

    /**
     * returned by potential UI interface as a response to handleInspections, this is asynchronous
     * @param inspections
     * @return
     */

}
