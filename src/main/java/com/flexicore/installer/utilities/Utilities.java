package com.flexicore.installer.utilities;

import com.flexicore.installer.runner.Start;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utilities {
    /**
     *
     * @param path the path to the file needing editing
     * @param existingString if not null it contains the the same file from a previous edit, saves disk IO
     * @param toFind the string to look for
     * @param toReplace the string to replace
     * @param warning send warnings on missed todry string
     * @param reverseSlash  if true will replace all / with \
     * @param close if true will close the file, leave false only when a subsequent editing on the same file is to be esecuted
     * @return
     */
    public static String editFile(String path, String existingString, String toFind, String toReplace, boolean warning, boolean reverseSlash, boolean close) {

        if (!new File(path).exists()) {
            if (warning) {
                Start.severe( "Cannot dry the file: " + path + " for editing");
            }
            return null;
        }
       Start.info("editing file:  " + path + " exists");
        toReplace = toReplace.replace("\\", "/");
        toFind = toFind.replace("\\", "/");
        String fileAsString = existingString;
        if (existingString == null || (existingString!=null && existingString.isEmpty())) {
            InputStream is;

            try {
                is = new FileInputStream(path);

                BufferedReader buf = new BufferedReader(new InputStreamReader(is));
                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();
                while (line != null) {
                    sb.append(line).append("\n");
                    line = buf.readLine();
                }
                fileAsString = sb.toString();
                if (fileAsString.length() == 0) {
                    is.close();
                    Start.severe("editing file: file was empty");
                    return null;
                }

                if (reverseSlash) {
                    fileAsString = fileAsString.replaceAll("/", "\\");
                }
                is.close();
            } catch (IOException e) {
                Start.severe("Error while reading file", e);
            }
        }
        if (fileAsString != null) {
            Start.info("Editing file:  file as string is not null");
            if (!fileAsString.contains(toFind)) {
                Start.info("Editing file:  file as string doesn't contain: " + toFind);
                return null;
            }
            fileAsString = fileAsString.replaceAll(toFind, toReplace);
        }
        if (close) {
            try {
                Files.write(Paths.get(path), fileAsString.getBytes());
            } catch (IOException e) {
                Start.severe("Error while writing file", e);
            }
        }
        Start.info("Editing file: ->" + fileAsString);
        return fileAsString;
    }
    public static boolean isNumeric(String strNum) {
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }
    public static boolean isInteger(String strNum) {
        try {
            Integer d = Integer.parseInt(strNum);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }
    public static boolean getBoolean(String strBoolean) {
        try {
            Boolean d = Boolean.parseBoolean(strBoolean.toLowerCase());
            return d;
        }catch (Exception e) {
            return false;
        }

    }
    public static Integer getInteger(String strInteger) {
        try {
            Integer d = Integer.parseInt(strInteger);
            return d;
        }catch (Exception e) {
            return 0;
        }

    }
}
