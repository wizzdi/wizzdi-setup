package com.flexicore.installer.model;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.ArrayList;
import java.util.List;

public class Parameter {
    private String name;
    private String description;
    private String defaultValue;
    private String value;
    private String nonTranslatedValue;
    private String referencedParameter;


    private String originalValue =null;
    private ParameterSource originalSource=null;
    private Integer minValue=0;
    private Integer maxValue=1000;
    private ParameterType type=ParameterType.STRING;

    private ParameterSource source=ParameterSource.CODE;
    private boolean sandSymbolPresent=false;
    private boolean hasValue;
    private boolean locked=false;
    private List<String> listOptions=new ArrayList<>();


    /**
     * list of methods to process when using one parameter to change another one
     */
    private List<Parameter.TransformParameter> transformParameters=new ArrayList<>();
    private Parameter.parameterValidator parameterValidator;
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
    public Parameter(String name, String description, boolean hasValue, String defaultValue,ParameterType parameterType,List<String> options) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions=options;

    }
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ParameterSource parameterSource, Parameter.parameterValidator validator) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source=parameterSource;
        this.parameterValidator=validator;

    }
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType,Parameter.parameterValidator validator) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator=validator;

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
    public boolean storeValue() {
        if (originalValue ==null) {
            originalValue =value;
            originalSource=source;
            return true;
        }
        return false;
    }
    public boolean restoreValue() {
        if (originalValue !=null) {
            value= originalValue;
            source=originalSource;
            return true;
        }
        return false;
    }
    public String getOriginalValue() {
        return originalValue;
    }

    public Parameter setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
        return this;
    }
    public ParameterSource getOriginalSource() {
        return originalSource;
    }

    public Parameter setOriginalSource(ParameterSource originalSource) {
        this.originalSource = originalSource;
        return this;
    }

    public String getNonTranslatedValue() {
        return nonTranslatedValue;
    }

    public Parameter setNonTranslatedValue(String nonTranslatedValue) {
        this.nonTranslatedValue = nonTranslatedValue;
        return this;
    }

    public boolean isSandSymbolPresent() {
        return sandSymbolPresent;
    }

    public Parameter setSandSymbolPresent(boolean sandSymbolPresent) {
        this.sandSymbolPresent = sandSymbolPresent;
        return this;
    }

    public String getReferencedParameter() {
        return referencedParameter;
    }

    public Parameter setReferencedParameter(String referencedParameter) {
        this.referencedParameter = referencedParameter;
        return this;
    }
    public void addTransform(Parameter.TransformParameter transform) {
        transformParameters.add(transform);
    }
    public void clearTransforms() {
        transformParameters.clear();
    }

    public static boolean validateEmail(Parameter parameter, ValidationMessage validationMessage) {
        return true;
    }

    @FunctionalInterface
    public  static interface TransformParameter {
        String transform(Parameter effected,Parameter effecting);
    }
    @FunctionalInterface
    public  static interface parameterValidator {
        boolean validate(Parameter toValid,ValidationMessage message);
    }

    public Parameter.parameterValidator getParameterValidator() {
        return parameterValidator;
    }

    public Parameter setParameterValidator(Parameter.parameterValidator parameterValidator) {
        this.parameterValidator = parameterValidator;
        return this;
    }
    public boolean validate(ValidationMessage validationMessage) {
        if (parameterValidator!=null) {
           return parameterValidator.validate(this,validationMessage);
        }
        return true;
    }
    public static boolean validateURL(Parameter parameter, ValidationMessage validationMessage) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        boolean result = urlValidator.isValid(parameter.getValue());
        if (!result) validationMessage.setMessage("URL is not a valid URL");
        return result;
    }
}

