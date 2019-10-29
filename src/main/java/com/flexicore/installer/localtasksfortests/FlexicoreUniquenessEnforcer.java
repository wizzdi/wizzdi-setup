package com.flexicore.installer.localtasksfortests;

import com.flexicore.installer.interfaces.IInstallationTask;
import com.flexicore.installer.model.*;
import org.pf4j.Extension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
make sure there are now double entries in entities and plugins, this is controllable by the two parameters-> ensureentities and ensureplugins

 */
@Extension
public class FlexicoreUniquenessEnforcer extends InstallationTask {
    static Logger logger;


    static Parameter[] preDefined = {
             new Parameter("ensureentities", "ensure now entities of the same type are installed", true,  "true",ParameterType.BOOLEAN),
            new Parameter("ensureplugins", "ensure no plugins of the same type are installed, rules out multiple versions support", true,  "true",ParameterType.BOOLEAN)

    };
    @Override
    public OperatingSystem[] getOperatingSystems() {
        return new OperatingSystem[]{OperatingSystem.Linux,OperatingSystem.Windows};
    }
    /**
     * set here for easier testing (shorter code)
     *
     * @param installationTasks
     */
    public FlexicoreUniquenessEnforcer(Map<String, IInstallationTask> installationTasks) {
        super(installationTasks);
    }



    /**
     * parameters are best provided by a different plugin
     *
     * @return
     */
    public  Parameters getPrivateParameters() {

        Parameters result = new Parameters();

        for (Parameter parameter : preDefined) {
            result.addParameter(parameter,this);
            logger.info("Got a default parameter: " + parameter.toString());
        }

        return result;

    }
    @Override
    public String getName() {
        return "Flexicore plugins/entities uniqueness enforcer";
    }
    @Override
    public Parameters getParameters(InstallationContext installationContext) {

        super.getParameters(installationContext);
        logger = installationContext.getLogger();
        logger.info("Getting parameters for " + this.toString());
        return getPrivateParameters();
    }

    @Override
    public InstallationResult install(InstallationContext installationContext) throws Throwable {

        super.install(installationContext);

        try {


            String flexicoreHome = getFlexicoreHome();

            if (!isDry()) {

               int total= ensureUnique(Paths.get(flexicoreHome+"/plugins"));
               info("Have removed "+total+" duplicate plugins");
               total= ensureUnique(Paths.get(flexicoreHome+"/entities"));
                info("Have removed "+total+" duplicate entities");

            }


        } catch (Exception e) {
            error("Error while configuring flexicore", e);
            return new InstallationResult().setInstallationStatus(InstallationStatus.FAILED);
        }
        return new InstallationResult().setInstallationStatus(InstallationStatus.COMPLETED);

    }

    private int ensureUnique(Path parentPath) throws IOException {
        List<Path> list=Files.walk(parentPath).collect(Collectors.toList());
        HashMap<String,List<Path>> all=new HashMap<>();
        for(Path path:list) {
           String base= removeVersion(path.toString());
            List<Path> pathList = all.get(base);
           if (pathList==null) {
                pathList=new ArrayList<>();
                all.put(base,pathList);
           }
           pathList.add(path);
        }
        for (List<Path> pathList:all.values()) {
            List<Path> toremove=new ArrayList<>();
            if (pathList.size()>1) {

                Collections.sort(pathList);

                for (Path path:pathList) {
                    if (!pathList.get(pathList.size()-1).equals(path)) {
                        //keep the latest version
                        toremove.add(path);
                    }
                }
            }
            for (Path path:toremove) {
                info("Have deleted version: "+path);
                Files.deleteIfExists(path);
            }
            return toremove.size();
        }
        return 0;
    }

    private String removeVersion(String name) {
        int index=name.lastIndexOf("-");
        if (isNumeric(name.substring(index+1,index+2))) {
            return name.substring(0, index);
        }
        return  name;
    }

    @Override
    public String getId() {
        return "flexicoreuniquenessenforcer";
    }

    @Override
    public Set<String> getPrerequisitesTask() {
        Set<String> result = new HashSet<>();
        result.add("flexicore-install");


        return result;
    }

    @Override
    public String getDescription() {
        return "Make sure that there are no double components by deleting previous once. using key names and versions, will not work with multiple versions environments ";
    }

    @Override
    public String toString() {
        return "Installation task: " + this.getId();
    }
    @Override
    public boolean cleanup() {
        return true;
    }
}
