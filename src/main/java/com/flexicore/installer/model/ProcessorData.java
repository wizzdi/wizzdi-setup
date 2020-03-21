package com.flexicore.installer.model;

public class ProcessorData {
    private String name;
    private ProcessorType processorType;
    private double processorFrequency;
    private int numberOfCores = -1;
    private int logicalCores = -1;

    public String getName() {
        return name;
    }

    public ProcessorData setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (name != null) b.append("Name: " + name);
        if (processorType != null) b.append(",ProcessorType: " + processorType);
        if (numberOfCores != -1) b.append(",Number of cores: " + numberOfCores);
        if (logicalCores != -1) b.append(",Logical cores: " + logicalCores);
        return b.toString();

    }

    public double getProcessorFrequency() {
        return processorFrequency;
    }

    public ProcessorData setProcessorFrequency(double processorFrequency) {
        this.processorFrequency = processorFrequency;
        return this;
    }

    public int getNumberOfCores() {
        return numberOfCores;
    }

    public ProcessorData setNumberOfCores(int numberOfCores) {
        this.numberOfCores = numberOfCores;
        return this;
    }

    public int getLogicalCores() {
        return logicalCores;
    }

    public ProcessorData setLogicalCores(int logicalCores) {
        this.logicalCores = logicalCores;
        return this;
    }

    public ProcessorType getProcessorType() {
        return processorType;
    }

    public ProcessorData setProcessorType(ProcessorType processorType) {
        this.processorType = processorType;
        return this;
    }
}
