package com.flexicore.installer.model;

import java.util.logging.Logger;

public class InstallationContext {

    private Logger logger;
    private Parameters parameters;

    public Logger getLogger() {
        return logger;
    }

    public Parameters getParamaters() {
        return parameters;
    }
    public <T extends InstallationContext> T setLogger(Logger logger) {
        this.logger = logger;
        return (T) this;
    }
    public <T extends InstallationContext> T setParameters(Parameters parameters) {
        this.parameters = parameters;
        return (T) this;
    }
}
