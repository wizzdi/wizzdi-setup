package com.flexicore.installer.model;

public class SystemData {
    private ProcessorData processorData;
    private double physicalMemory;
    private double freeDiskSpace;
    private WindowsVersion windowsVersion=WindowsVersion.Windows_10;
    public String toString() {
        StringBuilder b=new StringBuilder();
        if (processorData!=null) b.append("Processor Data: "+processorData);
        if (physicalMemory!=-1) b.append("\nPhysical Memory: "+(double)Math.round(physicalMemory));
        if (freeDiskSpace !=-1) b.append("\nFree disk space: "+(double)Math.round(freeDiskSpace));
        b.append("\nWindows version: "+windowsVersion);

        return b.toString();

    }
    public ProcessorData getProcessorData() {
        return processorData;
    }

    public SystemData setProcessorData(ProcessorData processorData) {
        this.processorData = processorData;
        return this;
    }

    public double getPhysicalMemory() {
        return physicalMemory;
    }

    public SystemData setPhysicalMemory(double physicalMemory) {
        this.physicalMemory = physicalMemory;
        return this;
    }

    public double getFreeDiskSpace() {
        return freeDiskSpace;
    }

    public SystemData setFreeDiskSpace(double freeDiskSpace) {
        this.freeDiskSpace = freeDiskSpace;
        return this;
    }

    public WindowsVersion getWindowsVersion() {
        return windowsVersion;
    }

    public SystemData setWindowsVersion(WindowsVersion windowsVersion) {
        this.windowsVersion = windowsVersion;
        return this;
    }


}
