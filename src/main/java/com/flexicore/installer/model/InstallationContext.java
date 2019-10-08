package com.flexicore.installer.model;
import com.flexicore.installer.runner.Start;
import java.util.Properties;
import java.util.logging.Logger;
public class InstallationContext {

    private Logger logger;
    private Parameters parameters;
    private Properties properties;
    private Start.UIAccessInterfaceClose uiClose;
    private Start.UIAccessInterfaceApply uiApply;
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

    public Properties getProperties() {
        return properties;
    }

    public InstallationContext setProperties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public Start.UIAccessInterfaceClose getUiClose() {
        return uiClose;
    }

    public InstallationContext setUiClose(Start.UIAccessInterfaceClose uiClose) {
        this.uiClose = uiClose;
        return this;
    }

    public Start.UIAccessInterfaceApply getUiApply() {
        return uiApply;
    }

    public InstallationContext setUiApply(Start.UIAccessInterfaceApply uiApply) {
        this.uiApply = uiApply;
        return this;
    }
}


