package com.flexicore.installer.model;

public class Parameter {
    private String name;
    private String description;
    private String defaultValue;
    private String value;
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
        return "Parameter: "+ "name: "+(name==null ? "no name is defined " :name )+" description: "+
                (description==null ? "no descriptions is defined " :description) + " hasvalue: "+hasValue+
                (hasValue ? " default value: "+ (defaultValue==null ? "no default value is defined" : defaultValue)+" value: "+ (value==null ?"no value is defined" :value): " This is a parameter without value" );
    }
}
