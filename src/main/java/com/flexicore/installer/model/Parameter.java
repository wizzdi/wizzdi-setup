package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;

public class Parameter {
    private String name;
    private String description;
    private String defaultValue;
    private String value;

    private ParameterSource source=ParameterSource.CODE;
    private ParameterType parameterType=ParameterType.StringType;
    private boolean hasValue;

    public String getDescription() {
        return description;
    }

    public Parameter setDescription(String description) {
        this.description = description;
        return this;
    }

    public Parameter(String name, String description, boolean hasValue, String defaultValue) {
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * by presence only, acting as a switch
     * @param name
     * @param description
     */
    public Parameter(String name, String description) {
        this.hasValue = false;
        this.name = name;
        this.description = description;
    }
    public Parameter() {
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
        return value != null ? value : (defaultValue != null ? defaultValue : "");
    }

    public Parameter setValue(String value) {
        this.value = value;
        return this;
    }

    public Boolean getBoolean() {
        String value = this.value != null ? this.value : (defaultValue != null ? defaultValue : "false");
        return Boolean.parseBoolean(value);

    }

    public boolean isHasValue() {
        return hasValue;
    }

    public Parameter setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
        return this;
    }
    @Override
    public String toString() {
        return "***** Parameter:\n "+ "name: "+(name==null ? "no name is defined \n" :name +"\n")+" description: "+
                (description==null ? "no descriptions is defined \n " :description+"\n") + " hasvalue: "+hasValue+"\n"+
                (hasValue ? " default value: "+ (defaultValue==null ? "no default value is defined \n" : defaultValue+"\n")+" current value: "+ (value==null ?"no value is defined\n" :value+"\n"): " This is a parameter without value\n" );
    }



    public ParameterSource getSource() {
        return source;
    }

    public Parameter setSource(ParameterSource source) {
        this.source = source;
        return this;
    }
}
