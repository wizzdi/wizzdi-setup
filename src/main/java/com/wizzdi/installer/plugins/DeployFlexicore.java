package com.wizzdi.installer.plugins;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import com.flexicore.installer.utilities.Utilities;
import org.pf4j.Extension;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@Extension
/**
 * Deploys Flexicore.war into existing Wildfly Installation
 */
public class DeployFlexicore extends InstallationTask {
    private boolean copySucceeded;

    @Override
    public String getVersion() {
        return "Deploy Flexicore 1.0.1";
    }

    static boolean installSpring = true; //with Spring, deploy flexicore is installing Spring service too.
    static Logger logger;
    static String ownerName = "Deploy flexicore";
    static String serviceName = installSpring ? "flexicore" : "wildfly";
    private String springSourceFolder;
    private String springConfigFolder;
    private String springTargetFolder;
    private String springConfigTargetFolder;
    String springXML;
    static String currentFolder = System.getProperty("user.dir");
    static String parentFolder = new File(currentFolder).getParent();
    static Parameter[] preDefined = {
            new Parameter("flexicore_running",
                    "When checked, it means that flexicore is running\n" +
                            "not intended for user changes",
                    true,
                    "false",
                    ParameterType.BOOLEAN,
                    null,
                    250,
                    false, false),
            new Parameter("heapsize",
                    "Java heap size for Spring \n" +
                            "shouldn't exceed 60-85% of availble memory",
                    true,
                    "612",
                    ParameterType.NUMBER,
                    null,
                    255,
                    false, false),
            new Parameter("loglocationlinux",
                    "log folder for Spring on Linux systems",
                    true,
                    "/var/log/flexicore",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    251,
                    null,
                    false,
                    false,
                    OperatingSystem.Linux
            ),
            new Parameter("loglocationWindows",
                    "log folder for Spring on Windows systems",
                    true,
                    "&flexicorehome/logs",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    251,
                    null,
                    false,
                    false,
                    OperatingSystem.Windows
            ),

    };
//    public Parameter(String name, String description, boolean hasValue, String defaultValue,
//                     ParameterType parameterType, ParameterSource parameterSource, int ordinal, Parameter.parameterValidator validator, boolean autoCreate, boolean hidden, OperatingSystem os) {

    private boolean serviceRunning;

    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    @Override
    public Parameters getPrivateParameters(InstallationContext context) {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            if (parameter.getName().equals("flexicore_running")) {
                parameter.setEditable(false);
            }
            result.addParameter(parameter, this);
            if (context.isExtraLogs()) logger.info("Got a default parameter: " + parameter.toString());
        }
        setContainer(context);
        return result;

    }

    private static void setContainer(InstallationContext context) {
        Parameter container = context.getParameter("container");
        if (container != null) installSpring = container.getValue().toLowerCase().equals("spring");
        serviceName = installSpring ? "flexicore" : "wildfly";
    }

    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters(installationContext);
    }

    File deployments;

    /**
     * this is the main installation entry point. Install starts here
     */
    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {
        super.install(installationContext);
        try {
            if (!dry) {
                serviceRunning = testServiceRunning(serviceName, "flexicore deploy", false);
                verifyStop();
                if (!installSpring) {
                    //wildfly deployment, wildfly should be already available (files tree)
                    if (!deployInWildfly(installationContext)) {
                        return failed();
                    }
                    return succeeded();
                } else {
                    if (copyFlexicoreFilesForSpring(installationContext)) {
                        copySucceeded = true;
                        return succeeded();
                    }
                    return failed();
                }


            } else {
                info("This is a dry run");
                return succeeded();
            }
        } catch (Exception e) {
            error("Error while installing flexicore deployment ", e);

        }
        return failed();

    }

    /**
     * creates the flexicore folder with the required files.
     * applications specific plugins are normally copied by a separate installer plugin
     * This is called with Spring only installation
     *
     * @param installationContext
     * @return
     * @throws InterruptedException
     */
    private boolean copyFlexicoreFilesForSpring(InstallationContext installationContext) throws InterruptedException, IOException {
        String flexicoreSourceFolder = getServerPath() + "flexicore";
        if (exists(flexicoreSourceFolder)) { //todo: fix files to correct flexicoreHome location
            if (!exists(flexicoreHome) || update) {
                if (copy(flexicoreSourceFolder, flexicoreHome, installationContext)) {
                    updateProgress(installationContext, "Have copied flexicore folder");
                    boolean fixed = fixFlexicoreHome(installationContext);
                    if (fixed) {
                        info("Have fixed paths in application.properties file in: "+flexicoreHome+"config/ folder");
                    }
                    return true;
                } else {
                    updateProgress(installationContext, "failed to copy flexicore from: " + flexicoreSourceFolder + " to: " + flexicoreHome);
                }
            } else {
                updateProgress(installationContext, "Flexicore home folder exists and update was not specified, skipping flexicore home update");
                return true;
            }
        } else {
            updateProgress(installationContext, "Have not found Flexicore Source folder at: " + flexicoreSourceFolder);
        }
        return false;
    }

    /**
     * change the path of properties in application.properties file in flexicoreHome+"/config"
     *
     * @param installationContext
     * @return
     * @throws IOException
     */
    private boolean fixFlexicoreHome(InstallationContext installationContext) throws IOException {
        if (isLinux) {
            if (!flexicoreHome.equals("/home/flexicore/")) {
                File file = new File(flexicoreHome + "config/application.properties");
                if (file.exists()) {
                  String intermediate = editFile(file.getAbsolutePath(), "", "/home/flexicore/", flexicoreHome, false, false, true, true);
                    return true;

                } else {

                }
            }
        }
        return false;
    }

    /**
     * Deploy Flexicore inside Wildfly. Not used for Spring
     *
     * @param installationContext
     * @return
     * @throws IOException
     */
    private boolean deployInWildfly(InstallationContext installationContext) throws IOException {
        String wildflyhome = isWindows ? installationContext.getParamaters().getValue("wildflyhome") : "/opt/wildfly/";
        File flexicore = new File(getServerPath() + "/flexicore/FlexiCore.war.zip");
        deployments = new File(wildflyhome + "/standalone/deployments");
        if (deployments.exists()) {
            if (exists(flexicore.getAbsolutePath())) {
                if (update || flexicoreMissing(deployments)) {

                    Path result = Files.copy(Paths.get(flexicore.getAbsolutePath())
                            , Paths.get(wildflyhome + "/standalone/FlexiCore.war.zip")
                            , StandardCopyOption.REPLACE_EXISTING);
                    setProgress(50);
                    try {
                        deleteDirectoryStream(deployments.getAbsolutePath() + "/FlexiCore.war");
                    } catch (Exception e) {
                        severe("error while deleting folder: " + deployments.getAbsolutePath() + "/FlexiCore.war");
                        return false;
                    }
                    File[] files = deployments.listFiles();
                    for (File file : files) {
                        String fileis = file.getAbsolutePath();
                        try {
                            Files.deleteIfExists(Paths.get(fileis));
                            info("Deleted " + file.getAbsolutePath());
                        } catch (Exception e) {
                            severe("error while deleting file: " + file.getAbsolutePath());
                        }
                    }

                    ZipUtil.unpack(flexicore, deployments);
                    info("Unpacked Flexicore.war.zip");
                    setProgress(70);
                    if (!isWindows) {
                        setOwnerFolder(Paths.get(deployments.getAbsolutePath()), serviceName, serviceName);
                    }
                    touch(new File(deployments.getAbsolutePath() + "/FlexiCore.war.dodeploy")); //this should start deployment once service runs

                } else {
                    updateProgress(installationContext, "WIll not update Flexicore as update option was not selected");
                }
                return true;
            } else {
                updateProgress(getContext(), "Cannot find Flexicore.war.zip at: " + flexicore.getAbsolutePath());

            }
        } else {
            updateProgress(installationContext, "Wildfly deployments was not located on: " + deployments.getAbsolutePath());

        }
        return false;
    }

    private void verifyStop() {
        if (serviceRunning) {
            if (installSpring) {
                stopWildfly(ownerName, 20000);
            } else {
                setServiceToStop(serviceName, ownerName);
            }
        }
    }

    private boolean flexicoreMissing(File deployments) {
        if (!deployments.exists()) return true;
        File[] files = deployments.listFiles();
        if (files == null || files.length != 2) return true;
        boolean folderFound = false;
        boolean controlFile = false;
        for (File file : files) {
            if (file.getName().equals("FlexiCore.war") && file.isDirectory()) {
                folderFound = true;
                continue;
            }
            if (file.getName().startsWith("FlexiCore.war")) {
                controlFile = true;
                continue;
            }
        }
        return !controlFile || !folderFound;
    }

    /**
     * For Wildfly only, wait on the file in /opt/wildfly/standalone/deployments
     * @param deployments
     * @return
     */
    private InstallationResult waitForDeployment(File deployments) {
        //was an update
        updateProgress(getContext(), "Waiting up to 200 seconds till Flexicore starts");
        DeployState state = waitForServertoDeploy(deployments.getAbsolutePath(), 200000);
        setProgress(97);
        switch (state) {
            case deployed:
                Service service = getNewService(true).setName("Flexicore").setDescription("Flexicore Framework");
                updateService(getContext(), service, this);
                updateProgress(getContext(), "Deployment succeeded");

                return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
            case failed:
                updateProgress(getContext(), "Deployment failed");
                break;
            case undeployed:
                updateProgress(getContext(), "Deployment undeployed, requires repeating update");
                break;

            case dodeploy:

                updateProgress(getContext(), "Deployment has not started");
                break;

            case undefined:
                updateProgress(getContext(), "Deployment undeployed");
                break;
            case isdeploying:
                updateProgress(getContext(), "Deployment is still deploying after 200 seconds");
                break;
            case nodeployfile:
                updateProgress(getContext(), "did not find a deploy file in the deployment folder!!");
                break;
            default:

        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
    }

    private DeployState waitForServertoDeploy(String path, long timeout) {
        long start = System.currentTimeMillis();

        try {
            File file = new File(path);
            do {
                Thread.sleep(20);
                File[] files = file.listFiles();
                if (files.length == 1) {
                    info("Was waiting for deploy but there is only one file in deployemts: " + files[0]);
                    return DeployState.nodeployfile;
                }
                if (exists(path + "/flexicore.war.isdeploying")) continue;
                if (exists(path + "/flexicore.war.deployed")) return DeployState.deployed;
                if (exists(path + "/flexicore.war.undeployed")) return DeployState.undeployed;
                if (exists(path + "/flexicore.war.failed")) return DeployState.failed;

            } while ((System.currentTimeMillis() - start) < timeout);
            if (exists(path + "/flexicore.war.isdeploying")) return DeployState.isdeploying;
            if (exists(path + "/flexicore.war.dodeploy")) return DeployState.dodeploy;
        } catch (InterruptedException e) {
            info("Stopped while waiting");
        }
        return DeployState.undefined;
    }

    public enum DeployState {
        isdeploying, deployed, dodeploy, undeployed, failed, undefined, nodeployfile
    }

    /**
     * defines here what are the compatible operating systems for this installer plugin
     */
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux, OperatingSystem.Windows};
    }

    @Override
    public InstallationResult finalizeInstallation(InstallationContext installationContext) throws Throwable {
        if (!dry) {
            info("Finalizer on deploy flexicore called");
            if (!installSpring) {
                Thread.sleep(300); //allow for Wildfly to start
                if (testServiceRunning(serviceName, "Flexicore deploy", false)) {
                    info("Deploy flexicore found service wildfly running");
                    InstallationResult result = waitForDeployment(deployments);
                    Parameter p = installationContext.getParameter("flexicore_running");
                    //required so other plugins can test if flexicore is running without knowing the details.
                    if (p != null && result.getInstallationStatus().equals(InstallationStatus.COMPLETED))
                        p.setValue("true");
                    return result;
                } else {
                    info("Deploy flexicore found service wildfly NOT running");
                    updateProgress(installationContext, "Deployed, but wildfly service not running");
                    return failed();
                }
            } else {
                //Spring installation here.
                if (copySucceeded) {
                    if (installSpringAsService(installationContext)) {
                        return succeeded();
                    } else return failed();
                } else {
                    updateProgress(installationContext, "copy of files failed so Spring cannot be started as a service");
                    return failed();
                }
            }

        }
        info("This is a dry run");
        return succeeded();
    }

    private boolean installSpringAsService(InstallationContext installationContext) {

        String heapMemory = getContext().getParamaters().getValue("heapsize");
        String serviceLocation = getServicesPath() + serviceName + ".service";
        if (!dry) {

            String tempDir = System.getProperty("java.io.tmpdir");
            try {
                boolean usersCopied = false;
                boolean keepOld = false;
                springSourceFolder = fixWindows(getServerPath() + "flexicore/spring");
                springConfigFolder = fixWindows(getServerPath() + "flexicore/config");
                springTargetFolder = fixWindows(getFlexicoreHome() + "spring");
                springConfigTargetFolder = fixWindows(getFlexicoreHome() + "config");
                if (isWindows) {
                    springXML = springSourceFolder + "/flexicore.xml";
                    if (!exists(springXML)) return false;
                }

                if (exists(springSourceFolder)) {

                    //the files in flexicoreHome were copied in install
                    if (isLinux) {
                        String serviceFile = getUbuntuServicesLocation() + serviceName + ".service";
                        if (exists(serviceFile)) {
                            boolean result = setServiceToStop(serviceName, ownerName);

                        }
                        boolean result = executeCommand("id -u flexicore &>/dev/null || useradd flexicore ", "", ownerName);
                        result = executeCommand("chmod 500 " + springTargetFolder + "/flexicore.jar", "", ownerName);
                        if (!result) {
                            info("failed to chmod 500 on flexicore.jar");
                            return false;
                        }
                        result = executeCommand("chown flexicore.flexicore " + springTargetFolder + "/flexicore.jar", "", ownerName);
                        if (!result) {
                            if (!result) {
                                info("failed to change owner of flexicore.jar");
                            }
                            return false;
                        }
                        Parameter loglocationParameter = installationContext.getParameter("loglocationlinux");
                        String loglocation = loglocationParameter != null ? loglocationParameter.getValue() : "/var/log/flexicore";
                        File logs = new File(loglocation);
                        if (!logs.exists()) {
                            if (!logs.mkdirs()) {
                                info("Failed to create logs folder: " + loglocation);
                                return false;
                            }
                        }
                        result = executeCommand("chown -R flexicore.flexicore " + loglocation, "", ownerName);
                        if (!result) {
                            if (!result) {
                                info("failed to change owner on logs folder at: " + logs.getAbsolutePath());
                            }
                            return false;
                        }
                        //must copy the service before fixing links
                        Files.copy(Paths.get(serviceLocation), Paths.get(getUbuntuServicesLocation() + serviceName + ".service"), StandardCopyOption.REPLACE_EXISTING);
                        fixServiceFile(serviceLocation,installationContext);
                        if (installService(serviceLocation, serviceName, ownerName)) {
                            info("Have successfully installed: " + serviceName);
                            if (heapMemory != null & !heapMemory.isEmpty()) {
                                String intermediate = "";
                                intermediate = Utilities.editFile(serviceFile, intermediate, "2048m", heapMemory + "m", true, false, true);
                                setServiceToStop(serviceName, ownerName);
                                executeCommand("systemctl daemon-reload", "", ownerName);
                                if (setServiceToStart(serviceName, ownerName)) {
                                    return true;
                                }
                            } else {
                                severe("Heap memory was not set to a legal value");
                            }
                        }


                    } else {

                    }

                    updateProgress(getContext(), "copying components, may take few minutes");

                } else {
                    updateProgress(getContext(), "Cannot find Spring Source Folder at: " + springSourceFolder);
                }
            } catch (Exception e) {
                error("Error while installing ", e);

            }
            setProgress(100);
            return false;

        } else {
            info("Dry run selected nothing will be installed");
        }
        return true;
    }

    /**
     * change paths in service file, points to the correct flexicore home
     * @param installationContext
     * @return
     */
    private boolean fixServiceFile(String serviceLocation,InstallationContext installationContext) throws IOException {
        if (flexicoreHome.equals("/home/flexicore")) return false;

            String result=editFile(serviceLocation,"","/home/flexicore/",flexicoreHome,false,false,true,true);


            return !result.isEmpty();


    }



    @Override
    public InstallationResult unInstall(InstallationContext installationContext) throws Throwable {
        if (!dry) {
            stopWildfly("FlexiCore deploy", 3000);
            int result = deleteDirectoryContent(getDeployments());
            if (result >= 0) {
                updateProgress(installationContext, "Have deleted :" + result);
                return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
            } else {
                updateProgress(installationContext, "No need to delete deployments, already deleted");
                return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
            }
        } else {
            info("This is a dry run");
            return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);
        }
    }

    @Override
    public Integer averageDuration() {
        return 1;
    }

    @Override
    public Integer averageFinalizerDuration() {
        return dry ? 0 : 60;
    }

    @Override
    public String getName() {
        return "deploy-flexicore";
    }

    /**
     * this must be unique, we use here the artifact-id
     */
    @Override
    public String getId() {
        return "deploy-flexicore";
    }

    /**
     * get the ids of the prerequistes tasks
     */
    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        if (!installSpring) {
            result.add("Wildfly-installer"); //otherwise we cannot install deployment
        }
        return result;
    }

    @Override
    public String getDescription() {
        if (installSpring) {
            return "Copy default flexicore folder, Install Spring Boot Flexicore jar as a service";
        } else {
            return "Deploys Flexicore into existing Wildfly Installation";
        }
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }


    @Override
    public IInstallationTask setId(String s) {
        return this;
    }

    @Override
    public Set<String> getNeedRestartTasks() {
        Set<String> result = new HashSet<>();
        if (!installSpring) {

            result.add("wildfly");
        }
        return result;
    }

    @Override
    public IInstallationTask setDescription(String s) {
        return this;
    }


    @Override
    public int mergeParameters(InstallationContext installationContext) {
        return 0;
    }


}
