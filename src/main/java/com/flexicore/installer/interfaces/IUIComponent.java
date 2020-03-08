package com.flexicore.installer.interfaces;

import com.flexicore.installer.model.*;
import org.pf4j.ExtensionPoint;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface IUIComponent extends ExtensionPoint {
    /**
     *
     * @param context
     */
    void setContext(InstallationContext context);

    /**
     *
     * @return
     */
    boolean isAutoStart();

    /**
     *
     * @return
     */
    boolean isShowing();

    /**
     *
     * @param context
     * @return
     */
    boolean startAsynch(InstallationContext context);
    boolean startBlocking(InstallationContext context);

    /**
     *
     * @param context
     * @param task
     * @return
     */
    boolean updateProgress(InstallationContext context,IInstallationTask task);
    boolean updateWidget(InstallationContext context,IInstallationTask task,Parameter parameter);

    boolean sendMessage(String message);
    boolean updateService(InstallationContext context, Service service, IInstallationTask task);
    String getVersion();
    boolean updateStatus(InstallationContext context, Map<String, Set<String>> additional, String message, InstallationState state);
    boolean refreshFilesCount(InstallationContext context,IInstallationTask task);

    /**
     *
     * @param context
     * @param userAction required User Action
     * @return
     */
    UserResponse askUser(InstallationContext context, UserAction userAction);

    /**
     * notify optional UI on Installation Tasks having some issues (already installed, different version installed).
     * @param context
     * @param inspections
     */
    void handleInspections(InstallationContext context, ArrayList<InspectionResult> inspections);



}
