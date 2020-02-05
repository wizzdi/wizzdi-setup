package com.flexicore.installer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FolderCompression {

    private List <String> fileList;
    private static final String OUTPUT_ZIP_FILE = "/wizzdi/server/flexicore/usersdata/Folder1.zip";
    private static final String SOURCE= "/wizzdi/server/flexicore/users/"; // SourceFolder path

    public FolderCompression() {
        fileList = new ArrayList < String > ();
    }

    public static void main(String[] args) {
        zipFolder(SOURCE,OUTPUT_ZIP_FILE,Logger.getLogger("test"));
    }
    public static boolean zipFolder(String folder, String zipfile, Logger logger) {
        FolderCompression appZip = new FolderCompression();
        appZip.generateFileList(folder,new File(folder));
        return appZip.zipIt(folder,zipfile,logger);
    }
    public boolean zipIt(String folder,String zipFile,Logger logger) {
        boolean result=true;
        byte[] buffer = new byte[1024];
        String source = new File(folder).getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            logger.info("Output to Zip : " + zipFile);
            FileInputStream in = null;

            for (String file: this.fileList) {
                logger.info("File Added : " + file);
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(folder + File.separator + file);
                    int len;
                    while ((len = in .read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    in.close();
                }
            }

            zos.closeEntry();
            logger.info("Folder successfully compressed");

        } catch (IOException ex) {
            logger.log(Level.SEVERE,"error while compressing",ex);
            result=false;
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                result=false;
                logger.log(Level.SEVERE,"error while compressing",e);
            }
        }
        return result;
    }

    public void generateFileList(String folder,File node) {

        if (node.isFile()) {
            fileList.add(generateZipEntry(folder,node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNodes = node.list();
            if (subNodes.length==0) {
                try {
                    new FileOutputStream(node+"/placeholder").close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                generateZipEntry(folder, node+"/placeholder");
            }
            for (String filename: subNodes) {
                generateFileList(folder,new File(node, filename));
            }
        }
    }

    private String generateZipEntry(String folder,String file) {
        return file.substring(folder.length() , file.length());
    }
}
