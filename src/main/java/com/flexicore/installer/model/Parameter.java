package com.flexicore.installer.model;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Parameter {
    private String name;
    private String description;
    private String defaultValue;
    private int defaultIndex;
    private String value;
    private String nonTranslatedValue;
    private String referencedParameter;
    private int ordinal=-1;


    private String originalValue =null;
    private ParameterSource originalSource=null;
    private Integer minValue=0;
    private Integer maxValue=1000;
    private ParameterType type=ParameterType.STRING;

    private ParameterSource source=ParameterSource.CODE;
    private boolean sandSymbolPresent=false;
    private boolean hasValue;
    private boolean locked=false;
    private ArrayList<String> listOptions=new ArrayList<>();
    private boolean autocreate=false;
    private boolean hidden=false;


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
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ArrayList<String> options, parameterValidator validator) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions=options;
        this.parameterValidator=validator;
    }
    public Parameter(String name, String description, boolean hasValue,
                     String defaultValue,ParameterType parameterType,ArrayList<String> options,ParameterSource parameterSource,Parameter.parameterValidator validator) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions = options;
        this.source=parameterSource;
        this.parameterValidator=validator;

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
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ParameterSource parameterSource, Parameter.parameterValidator validator,boolean autoCreate) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source=parameterSource;
        this.parameterValidator=validator;
        this.autocreate=autoCreate;

    }

    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ParameterSource parameterSource, Parameter.parameterValidator validator,boolean autoCreate,boolean hidden) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source=parameterSource;
        this.parameterValidator=validator;
        this.autocreate=autoCreate;
        this.setHidden(hidden);

    }
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType,
                     Parameter.parameterValidator validator, boolean autoCreate) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator=validator;
        this.autocreate=autoCreate;


    }
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType,
                     Parameter.parameterValidator validator, boolean autoCreate,boolean hidden) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator=validator;
        this.autocreate=autoCreate;
        this.hidden=hidden;

    }
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType, Parameter.parameterValidator validator, int ordinal, boolean autoCreate,boolean hidden) {
        this.type=parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator=validator;
        this.ordinal=ordinal;
        this.autocreate=autoCreate;
        this.hidden=hidden;

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

        if (type.equals(ParameterType.FOLDER) || type.equals(ParameterType.FILE)) {
            if (value.toLowerCase().startsWith("c:\\")) {
                value=value.replace("/","\\");
            }
        }
        this.value = value;
        return this;
    }

    public Boolean getBoolean() {
        String value = this.value != null ? this.value : (defaultValue != null ? defaultValue : "false");
        return Boolean.parseBoolean(value);

    }
    public String getSplitValue() {
        String v=getValue();
        String[] split=v.split(":");
        if (split.length>0) return split[1];
        return v;
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

    public ArrayList<String> getListOptions() {
        return listOptions;
    }

    public Parameter setListOptions(ArrayList<String> listOptions) {
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

    public int getDefaultIndex() {
        return defaultIndex;
    }

    public Parameter setDefaultIndex(int defaultIndex) {
        this.defaultIndex = defaultIndex;
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

    public static boolean validateEmail(InstallationContext context,Parameter parameter, Object newValue, ValidationMessage validationMessage) {
        String email=newValue.toString();
        email = email.trim();
        EmailValidator eValidator = EmailValidator.getInstance();
        if (eValidator.isValid(email)) return true;
        if (validationMessage!=null) validationMessage.setMessage(email+" is not a valid email");
        return false;
    }

    public boolean isAutocreate() {
        return autocreate;
    }

    public Parameter setAutocreate(boolean autocreate) {
        this.autocreate = autocreate;
        return this;
    }

    @FunctionalInterface
    public  static interface TransformParameter {
        String transform(Parameter effected,Parameter effecting);
    }
    @FunctionalInterface
    public  static interface parameterValidator {
        boolean validate(InstallationContext context,Parameter toValid,Object newValue,ValidationMessage message);
    }

    public Parameter.parameterValidator getParameterValidator() {
        return parameterValidator;
    }

    public Parameter setParameterValidator(Parameter.parameterValidator parameterValidator) {
        this.parameterValidator = parameterValidator;
        return this;
    }
    public boolean validate(InstallationContext context, Object newValue,ValidationMessage validationMessage) {
        if (parameterValidator!=null) {
           return parameterValidator.validate(context,this,newValue,validationMessage);
        }
        return true;
    }
    public static boolean validateURL(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        boolean result = urlValidator.isValid(newValue.toString());
        if (!result) validationMessage.setMessage("URL is not a valid URL");
        return result;
    }
    public static boolean validateList(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        if (parameter.getListOptions()!=null) {
            if (parameter.getListOptions().contains(newValue)) {
                return true;
            }
            validationMessage.setMessage("The value: <"+newValue +"> is not one of the possible values in the list");
            return false;
        }
        return true;
    }
    public static boolean validateExistingFolder(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        File file=new File(getReplaced(context,newValue.toString(),parameter));
        if (!file.exists()) {
            if (parameter.isAutocreate()) {
               if(file.mkdirs()) {
                   context.getLogger().info("have created the required folder: "+file.getAbsolutePath());

               }
            }else {
                validationMessage.setMessage("Cannot locate folder: " + newValue);
                return false;
            }
        }
        return true;
    }
    public static boolean validateExistingFile(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        File file=new File(getReplaced(context,newValue.toString(),parameter));
        if (!file.exists()) {
            validationMessage.setMessage("Cannot locate file: "+newValue);
            return false;
        }
        return true;
    }
    public static boolean validatePort(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        try {
            int p= Integer.parseInt(newValue.toString());
            if (p!=80 && ( p>8085 || p<8079 )) {
                validationMessage.setMessage("Port must be either 80 or between 8080 and 8085 \n, provided value is "+p);
                return false;
            }
        }catch ( Exception e) {
            validationMessage.setMessage("Port must be either 80 or between 8080 and 8085 \n, provided value is: "+newValue.toString());
            return false;
        }
        return true;
    }
    public static boolean validateHeap(InstallationContext context,Parameter parameter,Object newValue, ValidationMessage validationMessage) {
        try {
            int heap= Integer.parseInt(newValue.toString());
            if (heap%256!=0 || heap<768) {
                validationMessage.setMessage("Heap size must be divisible by 256 and equal or larger than 768 MBytes, value was:  "+newValue.toString());
                return false;
            }

        }catch ( Exception e) {
            validationMessage.setMessage("Heap size must be divisible by 256 and equal or larger than 768 MBytes , value was: "+newValue.toString());
            return false;
        }
        return true;
    }
    public int getOrdinal() {
        return ordinal;
    }

    public Parameter setOrdinal(int ordinal) {
        this.ordinal = ordinal;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Parameter setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public static String getReplaced(InstallationContext installationContext, String result, Parameter parameter) {
        Logger logger=installationContext.getLogger();
        logger.info("got to replace: "+result);
        int a = result.indexOf("&");
        if (a > -1) {
            int index = a + 2;
            int i = 1;
            while (index <= result.length()) {
                if (!result.substring(a + i++, index++).matches("[a-zA-Z0-9]+")) break;

            }
            if (index>result.length())index++; //special case

            String toReplace = result.substring(a+1, index - 2);
            logger.info("to replace is, this is the string we are looking for "+toReplace);
            String newString = installationContext.getParamaters().getValue(toReplace.substring(0));
            logger.info("this is the new replacement:  "+newString);
            if (newString != null && !newString.isEmpty()) {
                parameter.setReferencedParameter(toReplace.substring(1));
                result = result.replace(result.substring(a, index - 2), newString);

                logger.info("after replacement: "+result);
            }
        }else {
            logger.info("did not find & in the string "+result);
        }
        return result;
    }

}

