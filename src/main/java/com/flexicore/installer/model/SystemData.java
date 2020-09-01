package com.flexicore.installer.model;

import java.util.ArrayList;
import java.util.List;

public class SystemData {
    private ProcessorData processorData;
    private double physicalMemory;
    private double freeDiskSpace;
    private boolean done;
    private long start;
    private long total;
    private List<String> dotNetVersions =new ArrayList<>();
    private WindowsVersion windowsVersion=WindowsVersion.Windows_10;

    private String installDotNetVersion="0.0";

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

    public boolean isDone() {
        return done;
    }

    public SystemData setDone(boolean done) {
        this.done = done;
        return this;
    }

    public long getStart() {
        return start;
    }

    public SystemData setStart(long start) {
        this.start = start;
        return this;
    }

    public long getTotal() {
        return total;
    }

    public SystemData setTotal(long total) {
        this.total = total;
        return this;
    }
    public SystemData addDotNet(String toAdd) {
        dotNetVersions.add(toAdd);
        return this;
    }
    public List<String> getDotNetVersions() {
        return dotNetVersions;
    }

    public SystemData setDotNetVersions(List<String> dotNetVersions) {
        this.dotNetVersions = dotNetVersions;
        if (dotNetVersions!=null) {
            for (String version:dotNetVersions) {
                if (version.compareTo(installDotNetVersion)>0) {
                    installDotNetVersion=version;
                }
            }
        }
        return this;
    }

    public String getInstallDotNetVersion() {
        return installDotNetVersion;
    }
}
