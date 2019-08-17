package com.flexicore.installer.utilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class StreamGobbler extends Thread {
    InputStream is;
    String type;
    Logger logger;
    private boolean show=false;
    Queue<String> lines = new ConcurrentLinkedQueue<String>();

   public StreamGobbler(InputStream is, String type, Logger logger, boolean show) {
        this.is = is;
        this.type = type;
        this.logger = logger;
        this.show=show;
    }

    public boolean findString(String toFind) {

        for (String line : lines) {
            //System.out.println(line);
            if (line.toLowerCase().contains(toFind.toLowerCase())) {
                return true;
            }
        }
        return false;

    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                if (show) {
                    logger.info(line);
                }

            }
        } catch (IOException ioe) {
            lines.clear();
        }
    }

    public Queue<String> getLines() {
        return lines;
    }

}