package com.flexicore.installer.model;

public class ProcessorType {
    int family;
    int model;
    int stepping;
    String caption;
    String deviceId;
    String manufacturer;
    String maxClockSpeed;
    String name;
    String socketDesignation;
//    Caption           : Intel64 Family 6 Model 142 Stepping 10
//    DeviceID          : CPU0
//    Manufacturer      : GenuineIntel
//    MaxClockSpeed     : 1992
//    Name              : Intel(R) Core(TM) i7-8550U CPU @ 1.80GHz
//    SocketDesignation : U3E1
    public void populate(String data) {
        String[] lines=data.split("\n");
        for (String line:lines) {
            String[] split;
            split=line.split(":");
            if (split.length>1) {
                switch (split[0].trim()) {
                    case "Caption":
                        caption=split[1].trim();
                        String[] more=split[1].split("\\s+");
                        if (more[2].contains("Family")) {
                            family = Integer.parseInt(more[3]);
                            model = Integer.parseInt(more[5]);
                            stepping = Integer.parseInt(more[7]);
                        }
                        break;
                    case "DeviceID":
                        deviceId=split[1].trim();
                        break;
                    case "Manufacturer":
                        manufacturer=split[1].trim();
                        break;
                    case "MaxClockSpeed":
                        maxClockSpeed=split[1].trim();
                        break;
                    case "Name":
                        name=split[1].trim();
                        break;
                    case "SocketDesignation":
                        socketDesignation=split[1].trim();
                        break;


                }
            }
        }

    }
    public int getFamily() {
        return family;
    }

    public ProcessorType setFamily(int family) {
        this.family = family;
        return this;
    }

    public int getModel() {
        return model;
    }

    public ProcessorType setModel(int model) {
        this.model = model;
        return this;
    }

    public int getStepping() {
        return stepping;
    }

    public ProcessorType setStepping(int stepping) {
        this.stepping = stepping;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public ProcessorType setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public ProcessorType setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public ProcessorType setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }

    public String getMaxClockSpeed() {
        return maxClockSpeed;
    }

    public ProcessorType setMaxClockSpeed(String maxClockSpeed) {
        this.maxClockSpeed = maxClockSpeed;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProcessorType setName(String name) {
        this.name = name;
        return this;
    }

    public String getSocketDesignation() {
        return socketDesignation;
    }

    public ProcessorType setSocketDesignation(String socketDesignation) {
        this.socketDesignation = socketDesignation;
        return this;
    }
}
