package com.flexicore.installer.model;

import java.util.logging.Logger;

public class InstallationContext {

    private Logger logger;

    public Logger getLogger() {
        return logger;
    }

    public <T extends InstallationContext> T setLogger(Logger logger) {
        this.logger = logger;
        return (T) this;
    }
}
