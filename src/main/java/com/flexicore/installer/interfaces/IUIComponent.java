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
     *should the component started automatically
     * @return
     */
    boolean isAutoStart();
    boolean isWizard();
    /**
     * is the component currently displayed
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

    /**
     * update a visual widget when a parameter changes
     * @param context
     * @param task
     * @param parameter
     * @return
     */
    boolean updateWidget(InstallationContext context,IInstallationTask task,Parameter parameter);

    boolean sendMessage(String message);

    /**
     * allow wizards to display a welcome message todo: allow plugins to change this message....
     * @param context
     * @param messages
     * @return
     */
    boolean welcomeMessage(InstallationContext context,UserMessage[] messages);

    /**
     * allow wizards to display a completion message todo: allow plugins to change this message....
     * @param context
     * @param messages
     * @return
     */
    boolean completionMessage(InstallationContext context, UserMessage[] messages);
    boolean updateService(InstallationContext context, Service service, IInstallationTask task);
    String getVersion();

    /**
     * Update the status of the installation
     * @param context
     * @param additional
     * @param message
     * @param state
     * @return
     */
    boolean updateStatus(InstallationContext context, Map<String, Set<String>> additional, String message, InstallationState state);
    boolean refreshFilesCount(InstallationContext context,IInstallationTask task);

    /**
     *Ask user (using UI, or command line) for some response , see UserAction
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

    /**
     * Show current System data (normally in a dialog)
     * @param context
     * @param systemData
     * @return
     */

    UserResponse showSystemData(InstallationContext context,SystemData systemData);

    /**
     * Show Paged list of data, usually a file like log
     * @param context
     * @param pagedList
     * @return
     */
    UserResponse showPagedList(InstallationContext context, PagedList pagedList);
}
