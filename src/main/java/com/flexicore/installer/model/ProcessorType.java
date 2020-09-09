package com.flexicore.installer.model;

import java.util.jar.Manifest;

public class ProcessorType {
    int family = -1;
    int model = -1;
    int stepping = -1;
    String caption;
    String deviceId;
    String manufacturer;
    String maxClockSpeed;
    String name;
    String socketDesignation;
    String architecture="arm64";
    boolean bits64 = true;

    public static class ByteOrder {
        public enum Option {
            BigEndian,
            LittleEndian

        }
    }
    ByteOrder.Option byteOrder=ByteOrder.Option.LittleEndian;
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (caption != null) b.append("Caption: " + caption);
        if (deviceId != null) b.append("\nDeviceID: " + deviceId);
        if (manufacturer != null) b.append("\nManufacturer: " + manufacturer);
        if (maxClockSpeed != null) b.append("\nMaxclockSpeed: " + maxClockSpeed);
        if (name != null) b.append("\nName: " + name);
        if (socketDesignation != null) b.append(",\nSocketDesignation: " + socketDesignation);
        if (family != -1) b.append("\nFamily: " + family);
        if (model != -1) b.append(",Model: " + model);
        if (stepping != -1) b.append(",stepping: " + stepping);
        return b.toString();

    }

    public void populate(String data) {
        String[] lines = data.split("\n");
        for (String line : lines) {
            String[] split;
            split = line.split(":");
            if (split.length > 1) {
                switch (split[0].trim()) {
                    case "Caption":
                        caption = split[1].trim();
                        String[] more = split[1].split("\\s+");
                        int i = 0;
                        for (String m : more) {
                            if (m.toLowerCase().contains("family")) family = Integer.parseInt(more[i + 1]);
                            if (m.toLowerCase().contains("model")) model = Integer.parseInt(more[i + 1]);
                            if (m.toLowerCase().contains("stepping")) stepping = Integer.parseInt(more[i + 1]);
                            if (m.toLowerCase().contains("@")) stepping = Integer.parseInt(more[i + 1]);
                            i++;
                        }

                        break;
                    case "DeviceID":
                        deviceId = split[1].trim();
                        break;
                    case "Manufacturer":
                        manufacturer = split[1].trim();
                        break;
                    case "MaxClockSpeed":
                        maxClockSpeed = split[1].trim();
                        break;
                    case "Name":
                        name = split[1].trim();
                        break;
                    case "SocketDesignation":
                        socketDesignation = split[1].trim();
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

    public boolean isBits64() {
        return bits64;
    }

    public ProcessorType setBits64(boolean bits64) {
        this.bits64 = bits64;
        return this;
    }

    public ByteOrder.Option getByteOrder() {
        return byteOrder;
    }

    public ProcessorType setByteOrder(ByteOrder.Option byteOrder) {
        this.byteOrder = byteOrder;
        return this;
    }

    public String getArchitecture() {
        return architecture;
    }

    public ProcessorType setArchitecture(String architecture) {
        this.architecture = architecture;
        return this;
    }
}
