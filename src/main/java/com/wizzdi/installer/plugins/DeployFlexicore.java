package com.wizzdi.installer.plugins;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import com.flexicore.installer.runner.Start;
import com.flexicore.installer.utilities.Utilities;
import org.pf4j.Extension;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;


@Extension
/**
 * Deploys Flexicore.war into existing Wildfly Installation
 */
public class DeployFlexicore extends InstallationTask {
    private boolean copySucceeded;
    private boolean updateConfirmed;
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
                            "shouldn't exceed 60-85% of available memory",
                    true,
                    "768",
                    ParameterType.NUMBER,
                    null,
                    255,
                    false, false),
            new Parameter("jarlocationlinux",
                    "location for flexicore.jar link \n" +
                            "default is /home/flexicore/spring",
                    true,
                    "/opt/flexicore/",
                    ParameterType.FOLDER,
                    null,
                    260,
                    false, false),
            new Parameter("loglocationlinux",
                    "log folder for Spring on Linux systems",
                    true,
                    "/var/log/flexicore",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    265,
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
                    270,
                    null,
                    false,
                    false,
                    OperatingSystem.Windows
            ),
            new Parameter("freshinstallwithplugins",
                    "this is a hidden parameter to signal \n" +
                            "components installers that they do not need to run\n" +
                            "can be overridden by running update on the \n" +
                            "plugins installer",
                    true,
                    "true",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    275,
                    null,
                    false,
                    true,
                    OperatingSystem.All
            ),
            new Parameter("adminemail",
                    "the super administrator email",
                    true,
                    "admin@flexicore.com",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    280,
                    null,
                    false,
                    true,
                    OperatingSystem.All
            ),
            new Parameter("adminpassword",
                    "the super administrator password",
                    true,
                    "admin",
                    ParameterType.PASSWORD,
                    ParameterSource.CODE,
                    285,
                    null,
                    false,
                    true,
                    OperatingSystem.All
            ),
            new Parameter("freshinstallwithplugins",
                    "this is a hidden parameter to signal \n" +
                            "components installers that they do not need to run\n" +
                            "can be overridden by running update on the \n" +
                            "plugins installer",
                    true,
                    "true",
                    ParameterType.STRING,
                    ParameterSource.CODE,
                    290,
                    null,
                    false,
                    true,
                    OperatingSystem.All
            ),

    };

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
        installSpring = installSpring(context);
        return result;

    }


    @Override
    public Parameters getParameters(InstallationContext installationContext) {
        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters(installationContext);
    }

    File deployments;
    private boolean  confirmUpdate() {


            UserAction ua = new UserAction();
            ua.addMessage(new UserMessage().setMessage(" FlexiCore already installed, update?\n in case of doubt, confirm"));
            ua.setTitle("Warning");
            UserResponse[] userResponses = {UserResponse.OK, UserResponse.STOP};
            ua.setPossibleAnswers(userResponses);
            InstallationResult ir = new InstallationResult().setUserAction(ua);
            UserResponse response = Start.getUserResponse(getContext(), ua);
            return response.equals(UserResponse.OK);



    }

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
                    if (exists(flexicoreHome + (installSpring ? "config" : "entities"))) {
                        info("Found previous installation at:  " + flexicoreHome);
                        try {
                            updateConfirmed= confirmUpdate();
                        } catch (Exception e) {
                            severe("Error while confirming update",e);
                        }
                        info("Confirmation for update: "+updateConfirmed);
                        if (!is64) {
//                            if (updateToSpring()) {
//                                return succeeded();
//                            }else {
//
//                            }
                        }

                    } else {
                        info("It is assumed that is Flexicore first installation so included plugins need no\n" +
                                "additional installation from the same package");
                        Parameter parameter = installationContext.getParameter("freshinstallwithplugins");

                        if (parameter != null) {
                            parameter.setValue("true");
                        }
                        installationContext.setFreshInstall(true);
                    }

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

            if (!exists(flexicoreHome + (installSpring ? "config" : "entities")) || update || updateConfirmed) {
                if (!update && !updateConfirmed) {
                    if (copy(flexicoreSourceFolder, flexicoreHome, installationContext)) {
                        updateProgress(installationContext, "Have copied flexicore folder");
                        Credentials credentials = getCredentials(installationContext);
                        boolean fixed = fixConfigFile(installationContext, credentials);
                        if (fixed) {
                            info("Have fixed paths in application.properties file in: " + flexicoreHome + "config/ folder");
                        }
                        if (!credentials.getPassword().equals("")) {
                            boolean result = fixFirstRun(credentials);
                            if (result) {
                                info("Have fixed credentials " + flexicoreHome + "config/ folder");
                            }
                        }
                        return true;
                    } else {
                        updateProgress(installationContext, "failed to copy flexicore from: " + flexicoreSourceFolder + " to: " + flexicoreHome);
                    }
                } else {
                    if (copy(flexicoreSourceFolder + "/spring", flexicoreHome + "spring", installationContext)) {
                        updateProgress(installationContext, "Have copied Spring folder");

                        return true;
                    } else {
                        updateProgress(installationContext, "failed to copy flexicore from: " + flexicoreSourceFolder + " to: " + flexicoreHome);
                    }
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

    private boolean fixFirstRun(Credentials credentials) {
        String filePath = "";
        String config = flexicoreHome + "config/application.properties";
        try {
            if (credentials != null && credentials.getPassword() != null) {
                Properties properties = new Properties();
                FileInputStream is = new FileInputStream(config);
                properties.load(is);
                filePath = properties.getProperty("flexicore.users.firstRunPath");
                if (filePath!=null) {
                    FileWriter fileWriter = new FileWriter(filePath);
                    fileWriter.write(credentials.getPassword());
                    fileWriter.close();
                    info("First set password of " + credentials.getEmail() + " can be found in: " + filePath);
                    return true;
                }

            }
        } catch (IOException e) {
            severe("Error while writing to firstRun.txt at: " + filePath,e);
        }
        return false;
    }

    private Credentials getCredentials(InstallationContext context) {

        Credentials credentials = new Credentials();
        Parameter parameter = context.getParameter("adminemail");
        if (parameter != null) {
            if (!parameter.getValue().equals("admin@flexicore.com")) {
                credentials.setEmail(parameter.getValue());
            } else {
                credentials.setEmail("admin@flexicore.com");
            }

        }
        parameter = context.getParameter("adminpassword");
        if (parameter != null) {
            if (!parameter.getValue().isEmpty()) {
                credentials.setPassword(parameter.getValue());
            } else {
                credentials.setPassword("");
            }

        }
        return credentials;
    }


    /**
     * change the path of properties in application.properties file in flexicoreHome+"/config"
     *
     * @param installationContext
     * @param credentials
     * @return
     * @throws IOException
     */
    private boolean fixConfigFile(InstallationContext installationContext, Credentials credentials) throws IOException {

        File file = new File(flexicoreHome + "config/application.properties");
        if (file.exists()) {
            String intermediate = "";
            if (!credentials.getEmail().isEmpty()) {
                intermediate = editFile(file.getAbsolutePath(), "", "admin@flexicore.com", credentials.getEmail(), false, false, true, true);
                if (intermediate.equals("")) return false;
            }
            if (isLinux) {
                if (!flexicoreHome.equals("/home/flexicore/") && !flexicoreHome.equals("/home/flexicore//")) {
                    intermediate = editFile(file.getAbsolutePath(), intermediate, "/home/flexicore/", flexicoreHome, false, false, true, true);
                    return intermediate.equals("") ? false : true;
                }
            } else {
                if (!flexicoreHome.equals("/wizzdi/server/flexicore/") && !flexicoreHome.equals("/wizzdi/server/flexicore//")) {
                    intermediate = editFile(file.getAbsolutePath(), intermediate, "/wizzdi/server/flexicore/", flexicoreHome, false, false, true, true);
                    return intermediate.equals("") ? false : true;
                }
            }
            return true;
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
                        severe("error while deleting folder: " + deployments.getAbsolutePath() + "/FlexiCore.war",e);
                        return false;
                    }
                    File[] files = deployments.listFiles();
                    for (File file : files) {
                        String fileis = file.getAbsolutePath();
                        try {
                            Files.deleteIfExists(Paths.get(fileis));
                            info("Deleted " + file.getAbsolutePath());
                        } catch (Exception e) {
                            severe("error while deleting file: " + file.getAbsolutePath(),e);
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
     *
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
            severe("Stopped while waiting",e);
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
                    info("Deploy flexicore found service " + serviceName + " running");
                    InstallationResult result = waitForDeployment(deployments);
                    Parameter p = installationContext.getParameter("flexicore_running");
                    //required so other plugins can test if flexicore is running without knowing the details.
                    if (p != null && result.getInstallationStatus().equals(InstallationStatus.COMPLETED))
                        p.setValue("true");
                    return result;
                } else {
                    info("Deploy flexicore found service " + serviceName + " NOT running");
                    updateProgress(installationContext, "Deployed, but wildfly service not running");
                    return failed();
                }
            } else {
                //Spring installation here.
                if (copySucceeded) {
                    if (!update && !isUpdateThis()) {
                        if (installSpringAsService(installationContext)) {
                            if (isLinux) {
                                if (executeCommand("chown -R flexicore.flexicore " + flexicoreHome, "", ownerName)) {
                                    setServiceToStart(serviceName, ownerName);
                                    info("Have started service: " + serviceName);
                                    return succeeded();
                                } else {
                                    info("Cannot set " + flexicoreHome + " to flexicore owner");
                                    return failed();
                                }
                            } else {
                                if (setServiceToStart(serviceName, ownerName)) {
                                    updateProgress(getContext(), "have started " + serviceName);
                                    return succeeded();
                                } else {
                                    updateProgress(getContext(), "have failed to start " + serviceName);
                                    return failed();
                                }
                            }
                        } else return failed();
                    } else {
                        deleteDirectoryStream(flexicoreHome + "logs");
                        if (setServiceToStart(serviceName, ownerName)) {
                            updateProgress(installationContext, "Started FlexiCore as a service (Spring)");
                            return succeeded();
                        } else {
                            updateProgress(installationContext, "was not able to start FlexiCore as a service (Spring)");
                            return failed();

                        }
                    }
                } else {
                    updateProgress(installationContext, "copy of files failed in installation phase so Spring cannot be started as a service");
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
        boolean updateHeap = heapMemory != null && !heapMemory.isEmpty() && !heapMemory.equals("2048");
        if (!dry) {

            String tempDir = System.getProperty("java.io.tmpdir");
            try {
                boolean usersCopied = false;
                boolean keepOld = false;
                springSourceFolder = fixWindows(getServerPath() + "flexicore/spring");
                springConfigFolder = fixWindows(getServerPath() + "flexicore/config");
                springTargetFolder = fixWindows("/opt/flexicore/");
                springConfigTargetFolder = fixWindows(getFlexicoreHome() + "config");
                if (isWindows) {
                    springXML = getFlexicoreHome() + "/spring/flexicore.xml";
                    if (!exists(springXML)) {
                        info("cannot find the spring XML in: " + springXML);
                        return false;
                    }
                } else {
                    Parameter springTarget = getContext().getParameter("jarlocationlinux");
                    if (springTarget == null) {
                        updateProgress(installationContext, "no target for flexicore.jar has been specified");
                        return false;
                    }
                    springTargetFolder = springTarget.getValue();
                }


                //the files in flexicoreHome were copied in install
                if (isLinux) {
                    if (exists(springSourceFolder)) {
                        String serviceFile = getUbuntuServicesLocation() + serviceName + ".service";
                        if (exists(serviceFile)) {
                            boolean result = setServiceToStop(serviceName, ownerName);

                        }
                        boolean result = executeCommand("adduser flexicore --shell=/bin/false --no-create-home", "", ownerName);
                        if (!result) {
                            updateProgress(installationContext, "failed to create user flexicore, service will not run  or user already exists");
                        }
                        //create a link to the latest version of Flexicore...
                        if (!fixLinks(installationContext)) {
                            info("failed to fix links on latest flexicore.jar");
                            return false;

                        }

                        result = executeCommand("chown -R flexicore.flexicore " + springTargetFolder, "", ownerName);
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
                        String targetServiceLocation;
                        Files.copy(Paths.get(serviceLocation), Paths.get(targetServiceLocation = getUbuntuServicesLocation() + serviceName + ".service"), StandardCopyOption.REPLACE_EXISTING);


                        if (!springTargetFolder.equals("/home/flexicore/spring/")) {
                            String after = editFile(serviceFile, "", "/home/flexicore/spring/flexicore.jar", springTargetFolder + "flexicore.jar"
                                    , false, false, false, false);
                            after = editFile(serviceFile, after, "/home/flexicore/spring", springTargetFolder, false, false, !updateHeap, false);
                            if (after.isEmpty()) {
                                severe("Error while editing service file");
                            }
                            if (updateHeap) {
                                after = Utilities.editFile(serviceFile, after, "2048m", heapMemory + "m", true, false, true);
                                executeCommand("systemctl daemon-reload", "", ownerName);


                            } else {
                                severe("Heap memory was not set to a legal value");
                            }
                        }

                        if (installService(null, serviceName, ownerName, false)) {
                            info("Have successfully installed: " + serviceName);
                            return true;
                        }
                    } else {
                        updateProgress(getContext(), "Cannot find Spring source folder: " + springSourceFolder);
                        return false;
                    }
                } else { //windows here
                    if (updateHeap) {
                        String result = editFile(springXML, "", "2048", heapMemory, false, false, true, false);
                        if (result == null) {
                            severe("Could not update heap memory in: " + springXML);
                            return false;
                        } else {
                            info("Updated FlexiCore heap memory to: " + heapMemory);
                        }
                    }
                    String target = flexicoreHome + "spring/flexicore.exe";
                    if (exists(target)) {
                        String args[] = {target, "install"};
                        if (executeCommandByBuilder(args, "", false, "Deploy flexicore finalizer", new File(target).getParent())) {
                            updateProgress(getContext(), "have successfully installed Flexicore as a service on Windows");
                            return true;
                        } else {
                            updateProgress(getContext(), "have failed to install Flexicore as a service on Windows");
                        }
                    } else {
                        updateProgress(getContext(), "have failed to find target for service install at: " + target);
                    }
                }

                updateProgress(getContext(), "copying components, may take few minutes");


            } catch (Exception e) {
                severe("Error while installing "+getId(), e);

            }
            setProgress(100);
            return false;

        } else {
            info("Dry run selected nothing will be installed");
        }
        return true;
    }

    /**
     * find latest Spring Boot Flexicore file and create a standard link for the service.
     *
     * @param installationContext
     * @return
     * @throws IOException
     */
    private boolean fixLinks(InstallationContext installationContext) {
        try {
            String candidate = getLatestVersion(springSourceFolder, "FlexiCore-", ".jar");

            if (!candidate.isEmpty()) {

                info("Found Flexicore version: " + candidate);

                if (!exists(springTargetFolder)) {
                    boolean result = (new File(springTargetFolder)).mkdirs();
                }

                //this is just for cases where /home/flexicore is on mounted device
                Path path = Files.copy(Paths.get(candidate), Paths.get(springTargetFolder + "/" + new File(candidate).getName()), StandardCopyOption.REPLACE_EXISTING);
                if (path != null) {
                    String[] args = {"ln", "-fs", path.toString(), new File(springTargetFolder) + "/flexicore.jar"};
                    boolean result = executeCommandByBuilder(args, "", false, ownerName, false);
                    return result;
                }
            } else {
                info("couldn't find a candidate for flexicore.jar");
            }
            return false;
        } catch (IOException e) {
            severe("Error while fixing links in " + ownerName, e);
        }
        return false;
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

        result.add("PostgresSQL-installer");
        return result;
    }

    @Override
    public Set<String> getSoftPrerequisitesTask() {

        Set<String> result = new HashSet<>();
        result.add("java-installer"); //soft as it may be a 32 bit system with Java installed in an earlier stage
        result.add("mongodb-installer");
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
