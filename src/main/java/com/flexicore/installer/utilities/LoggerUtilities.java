package com.flexicore.installer.utilities;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerUtilities {
    public static String logsFolder = null;
    private static Queue<Logger> loggers = new LinkedBlockingQueue<>();

    public static Logger initLogger(String name) {
        Logger logger = Logger.getLogger(name);
        if (logsFolder != null) {
            name = logsFolder + "/" + name;
        }

        FileHandler fh;

        try {

            //changed to support
            fh = new FileHandler(name + ".log",false);


            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);


            logger.info("****************************Have started new logger********************************");
            logger.setUseParentHandlers(true); //will show on terminal too.


        } catch (SecurityException e) {
            logger.info("Permission issue when creating log file");

        } catch (IOException e) {
            logger.info("IO issue when creating log file");
        }
        loggers.add(logger);
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
