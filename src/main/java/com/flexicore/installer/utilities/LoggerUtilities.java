package com.flexicore.installer.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtilities {
    public static String logsFolder = null;
    private static Queue<Logger> loggers = new LinkedBlockingQueue<>();

    public static Logger initLogger(String name, String folder) {

        Logger logger = Logger.getLogger(name);
        FileHandler
                fh;
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            handler.close();
            logger.removeHandler(handler);
        }

        try {

            // String time = LocalDateTime.now().toString().replace(":", "-");
            File file;
            if (!(file = new File(folder)).exists()) {
                Files.createDirectories(file.toPath());
            }
            fh = new FileHandler(folder.isEmpty() ? name + ".log" : folder + "/" + name + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            loggers.add(logger);


        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logger;

    }

    public static void closeLogger(Logger logger) {
        if (logger == null) {
            return;
        }
        logger.info("closing logger " + logger.getName());
        for (Handler handler : logger.getHandlers()) {
            handler.close();
        }

    }

    public static void closeAllLoggers() {
        for (Logger logger : loggers) {
            closeLogger(logger);
        }
    }


}
