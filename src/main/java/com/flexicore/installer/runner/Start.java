package com.flexicore.installer.runner;

import com.flexicore.installer.exceptions.MissingInstallationTaskDependency;
import com.flexicore.installer.interfaces.InstallationTask;
import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationResult;
import org.apache.commons.cli.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Start {

    private static final String HELP = "h";
    private static final String LOG_PATH_OPT = "l";
    private static final String INSTALLATION_TASKS_FOLDER = "tasks";

    public static void main(String[] args) throws MissingInstallationTaskDependency, ParseException {

        Options options=initOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        Logger logger = Logger.getLogger("Installation");
        File pluginRoot=new File(cmd.getOptionValue(INSTALLATION_TASKS_FOLDER,"tasks"));
        logger.info("Will load tasks from "+pluginRoot.getAbsolutePath());
        PluginManager pluginManager = new DefaultPluginManager(pluginRoot.toPath());

        pluginManager.loadPlugins();
        pluginManager.startPlugins();

        InstallationContext installationContext = new InstallationContext()
                .setLogger(logger);
        Map<String,InstallationTask> installationTasks = pluginManager.getExtensions(InstallationTask.class).parallelStream().collect(Collectors.toMap(f->f.getId(), f->f));
        TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = getInstallationTaskIterator(installationTasks);

        while(topologicalOrderIterator.hasNext()){
           String installationTaskUniqueId=topologicalOrderIterator.next();
            InstallationTask installationTask=installationTasks.get(installationTaskUniqueId);
            logger.info("Starting " + installationTask.getId());
            InstallationResult installationResult = installationTask.install(installationContext);
            logger.info("Completed " + installationTask.getId() + " with " + installationResult);
       }


        // stop and unload all plugins
        pluginManager.stopPlugins();
    }

    private static TopologicalOrderIterator<String, DefaultEdge> getInstallationTaskIterator(Map<String, InstallationTask> installationTasks) throws MissingInstallationTaskDependency {
        Map<String, Set<String>> missingDependencies=new HashMap<>();
        Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (InstallationTask installationTask : installationTasks.values()) {
            String uniqueId = installationTask.getId();
            g.addVertex(uniqueId);
            for (String req : installationTask.getDependentTasks()) {
                if(installationTasks.containsKey(req)){
                    g.addVertex(req);
                    g.addEdge(req, uniqueId);
                }
                else{
                    missingDependencies.computeIfAbsent(uniqueId,f->new HashSet<>()).add(req);
                }
            }
        }
        if(!missingDependencies.isEmpty()){
            String s="Missing Dependencies:"+System.lineSeparator()+missingDependencies.entrySet().parallelStream().map(f->f.getKey() +" : "+f.getValue()).collect(Collectors.joining(System.lineSeparator()));
            throw new MissingInstallationTaskDependency(s);
        }
        return new TopologicalOrderIterator<>(g);
    }

    private static Options initOptions() {

        // create Options object
        return new Options()
                .addOption(INSTALLATION_TASKS_FOLDER,true,"(optional - default currentDir/tasks) folder for installation tasks")
                .addOption(LOG_PATH_OPT,true,"log folder")

                .addOption(HELP ,false,"shows this message");

    }
}
