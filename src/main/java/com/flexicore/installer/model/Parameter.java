package com.flexicore.installer.model;
import java.util.ArrayList;
import java.util.List;

public class Parameter {
    private String name;
    private String description;
    private String defaultValue;
    private String value;
    private Integer minValue=0;
    private Integer maxValue=1000;
    private ParameterType type=ParameterType.STRING;

    private ParameterSource source=ParameterSource.CODE;

    private boolean hasValue;
    private boolean locked=false;
    private List<String> listOptions=new ArrayList<>();

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
    public Parameter(String name, String description, boolean hasValue, String defaultValue,ParameterType parameterType) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;

    }
    public Parameter(String name, String description, boolean hasValue, String defaultValue,ParameterType parameterType, ParameterSource parameterSource) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source=parameterSource;

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

    public ParameterType getType() {
        return type;
    }

    public Parameter setType(ParameterType type) {
        this.type = type;
        return this;
    }

    public List<String> getListOptions() {
        return listOptions;
    }

    public Parameter setListOptions(List<String> listOptions) {
        this.listOptions = listOptions;
        return this;
    }

    public boolean isLocked() {
        return locked;
    }

    public Parameter setLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public Parameter setMinValue(Integer minValue) {
        this.minValue = minValue;
        return this;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public Parameter setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
        return this;
    }
}
