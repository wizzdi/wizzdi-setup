package com.flexicore.installer.model;

public class Parameter {
    String name="";
    String description="";
    String defaultValue="";
    String value;
    boolean hasValue=false;

    public String getDescription() {
        return description;
    }

    public Parameter setDescription(String description) {
        this.description = description;
        return this;
    }

    public Parameter(String name, String description,boolean hasValue,String defaultValue) {
        this.hasValue=hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue=defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Parameter setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getName() {
        return name;
    }

    public Parameter setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value!=null ? value : (defaultValue!=null ?defaultValue : "");
    }

    public Parameter setValue(String value) {
        this.value = value;
        return this;
    }
    public Boolean getBoolean() {
        String value= this.value!=null ?this.value :(defaultValue!=null ? defaultValue: "false");
        return Boolean.parseBoolean(value);

    }

    public boolean isHasValue() {
        return hasValue;
    }

    public Parameter setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
        return this;
    }
}
