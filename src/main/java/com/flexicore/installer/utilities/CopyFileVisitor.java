package com.flexicore.installer.utilities;

import com.flexicore.installer.model.InstallationContext;
import com.flexicore.installer.model.InstallationTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyFileVisitor extends SimpleFileVisitor<Path> {
    private final Path targetPath;
    private Path sourcePath = null;

    private InstallationTask installationTask;
    private Logger logger;
    private InstallationContext context;
    private boolean copyOver=true;
    private String ownerName;

    public String getOwnerName() {
        return ownerName;
    }

    public CopyFileVisitor setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public CopyFileVisitor setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public boolean isCopyOver() {
        return copyOver;
    }

    public CopyFileVisitor setCopyOver(boolean copyOver) {
        this.copyOver = copyOver;
        return this;
    }



    public InstallationTask getInstallationTask() {
        return installationTask;
    }

    public CopyFileVisitor setInstallationTask(InstallationTask installationTask) {
        this.installationTask = installationTask;
        return this;
    }

    public CopyFileVisitor(Path targetPath) {
        this.targetPath = targetPath;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
                                             final BasicFileAttributes attrs) throws IOException {

        if (sourcePath == null) {
            sourcePath = dir;
        } else {
            try {
                Files.createDirectories(targetPath.resolve(sourcePath
                        .relativize(dir)));

            } catch (IOException e) {
                installationTask.incrementFoldersFailures();
                throw e;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        FileVisitResult result = super.postVisitDirectory(dir, exc);
        installationTask.incrementFolders();
        context.getFilesProgress().filesProgress(context,installationTask);
        return result;
    }

    int count=0;
    int errors=0;

    public int getCount() {
        return count;
    }

    public int getErrors() {
        return errors;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        getInstallationTask().incrementFilesErrors();
        return super.visitFileFailed(file, exc);
    }

    public InstallationContext getContext() {
        return context;
    }

    public CopyFileVisitor setContext(InstallationContext context) {
        this.context = context;
        this.logger=context.getLogger();
        return this;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
                                     final BasicFileAttributes attrs) {

        Path path=null;
        try {
            path=targetPath.resolve(sourcePath.relativize(file));
            File fileToDelete=null;
            if ((fileToDelete=new File(path.toString())).exists()) {
                if (copyOver) {
                    fileToDelete.setWritable(true); //just in case
                    Files.copy(file,path, StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);
                    installationTask.incrementFiles();
                }


            }else {
                Files.copy(file,path);
                installationTask.incrementFiles();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,"Error while copying file "+path,e);
            installationTask.incrementFilesErrors();
            errors++;

        }
        return FileVisitResult.CONTINUE;
    }

    public void clear() {
        count=0;
    }
}
