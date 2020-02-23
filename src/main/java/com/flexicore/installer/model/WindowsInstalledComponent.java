package com.flexicore.installer.model;

public class WindowsInstalledComponent {
    private String IdentifyingNumber;
    private String name;
    private String vendor;
    private String version;
    private String caption;

    public String getIdentifyingNumber() {
        return IdentifyingNumber;
    }

    public WindowsInstalledComponent setIdentifyingNumber(String identifyingNumber) {
        IdentifyingNumber = identifyingNumber;
        return this;
    }

    public String getName() {
        return name;
    }

    public WindowsInstalledComponent setName(String name) {
        this.name = name;
        return this;
    }

    public String getVendor() {
        return vendor;
    }

    public WindowsInstalledComponent setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public WindowsInstalledComponent setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public WindowsInstalledComponent setCaption(String caption) {
        this.caption = caption;
        return this;
    }
}
