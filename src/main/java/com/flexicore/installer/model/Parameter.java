package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Parameter {
    private  boolean donotSave=false;
    private String name;
    private String description;
    private String defaultValue;
    private int defaultIndex;
    private String value;
    private String nonTranslatedValue;
    private String referencedParameter;
    private int ordinal = -1;
    private OperatingSystem os = OperatingSystem.All;
    private String originalValue = null;
    private ParameterSource originalSource = null;
    private Integer minValue = Integer.MIN_VALUE;
    private Integer maxValue = Integer.MAX_VALUE;
    private Double minDoubleValue = Double.MIN_VALUE;
    private Double maxDoubleValue = Double.MAX_VALUE;
    private ParameterType type = ParameterType.STRING;

    private ParameterSource source = ParameterSource.CODE;
    private boolean sandSymbolPresent = false;
    private boolean hasValue;
    private boolean locked = false;
    private ArrayList<String> listOptions = new ArrayList<>();
    private boolean autocreate = false;
    private boolean hidden = false;
    private ArrayList<Parameter> subscribers = new ArrayList<>();
    private IInstallationTask iInstallationTask;
    private InstallationContext installationContext;
    private boolean editable = true;


    /**
     * list of methods to process when using one parameter to change another one
     */
    private List<Parameter.TransformParameter> transformParameters = new ArrayList<>();
    private Parameter.parameterValidator parameterValidator;

    public String getDescription() {
        return description;
    }

    public Parameter setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     */
    public Parameter(String name, String description, boolean hasValue, String defaultValue) {
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     */
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;


    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param options
     * @param validator
     */
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ArrayList<String> options, parameterValidator validator) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions = options;
        this.parameterValidator = validator;
    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param options
     * @param parameterSource
     * @param validator
     */
    public Parameter(String name, String description, boolean hasValue,
                     String defaultValue, ParameterType parameterType, ArrayList<String> options, ParameterSource parameterSource, Parameter.parameterValidator validator) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions = options;
        this.source = parameterSource;
        this.parameterValidator = validator;

    }

    public Parameter(String name, String description, boolean hasValue,
                     String defaultValue, ParameterType parameterType, ArrayList<String> options,
                     ParameterSource parameterSource, Parameter.parameterValidator validator, int ordinal, boolean autoCreate, boolean hidden) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.listOptions = options;
        this.source = parameterSource;
        this.parameterValidator = validator;
        this.ordinal = ordinal;
        this.autocreate = autoCreate;
        this.hidden = hidden;

    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param parameterSource
     * @param validator
     */
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ParameterSource parameterSource, Parameter.parameterValidator validator) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source = parameterSource;
        this.parameterValidator = validator;

    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param parameterSource
     * @param validator
     * @param autoCreate
     */
    public Parameter(String name, String description, boolean hasValue, String defaultValue, ParameterType parameterType, ParameterSource parameterSource, Parameter.parameterValidator validator, boolean autoCreate) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source = parameterSource;
        this.parameterValidator = validator;
        this.autocreate = autoCreate;

    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param parameterSource
     * @param validator
     * @param autoCreate
     * @param hidden
     * @param donotSave if true will not be saved in a properties file
     */
    public Parameter(String name,
                     String description,
                     boolean hasValue,
                     String defaultValue,
                     ParameterType parameterType,
                     ParameterSource parameterSource,
                     Parameter.parameterValidator validator,
                     boolean autoCreate,
                     boolean hidden,
                     boolean donotSave) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source = parameterSource;
        this.parameterValidator = validator;
        this.autocreate = autoCreate;
        this.setHidden(hidden);
        this.donotSave=donotSave;

    }

    public Parameter(String name,
                     String description,
                     boolean hasValue,
                     String defaultValue,
                     ParameterType parameterType,
                     Parameter.parameterValidator validator,
                     double maxDoubleValue,
                     double minDoubleValue,
                     int ordinal,
                     boolean autoCreate,
                     boolean hidden) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator = validator;
        this.maxDoubleValue = maxDoubleValue;
        this.minDoubleValue = minDoubleValue;
        this.ordinal = ordinal;
        this.autocreate = autoCreate;
        this.setHidden(hidden);

    }

    public Parameter(String name,
                     String description,
                     boolean hasValue,
                     String defaultValue,
                     ParameterType parameterType,
                     Parameter.parameterValidator validator,
                     int maxDoubleValue,
                     int minDoubleValue,
                     int ordinal,
                     boolean autoCreate,
                     boolean hidden) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator = validator;
        this.maxValue = maxDoubleValue;
        this.minValue = minDoubleValue;
        this.ordinal = ordinal;
        this.autocreate = autoCreate;
        this.setHidden(hidden);

    }

    public Parameter(String name, String description, boolean hasValue, String defaultValue,
                     ParameterType parameterType, ParameterSource parameterSource, int ordinal, Parameter.parameterValidator validator, boolean autoCreate, boolean hidden, OperatingSystem os) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.source = parameterSource;
        this.setOrdinal(ordinal);
        this.parameterValidator = validator;
        this.autocreate = autoCreate;
        this.setHidden(hidden);
        this.os = os;

    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param validator
     * @param autoCreate
     */
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType,
                     Parameter.parameterValidator validator, boolean autoCreate) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator = validator;
        this.autocreate = autoCreate;


    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param validator
     * @param autoCreate
     * @param hidden
     */
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType,
                     Parameter.parameterValidator validator, boolean autoCreate, boolean hidden) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator = validator;
        this.autocreate = autoCreate;
        this.hidden = hidden;

    }

    /**
     * @param name
     * @param description
     * @param hasValue
     * @param defaultValue
     * @param parameterType
     * @param validator
     * @param ordinal
     * @param autoCreate
     * @param hidden
     */
    public Parameter(String name, String description,
                     boolean hasValue, String defaultValue, ParameterType parameterType, Parameter.parameterValidator validator, int ordinal, boolean autoCreate, boolean hidden) {
        this.type = parameterType;
        this.hasValue = hasValue;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.parameterValidator = validator;
        this.ordinal = ordinal;
        this.autocreate = autoCreate;
        this.hidden = hidden;

    }

    /**
     * by presence only, acting as a switch
     *
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

    /**
     * name of the parameter
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public Parameter setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * current value of this parameter.
     *
     * @return
     */
    public String getValue() {
        return value != null ? value : (defaultValue != null ? defaultValue : "");
    }

    /**
     * creates a correct value for List type.
     * @return
     */
    public String getValueForProperties() {
        if (type.equals(ParameterType.LIST)) {
            if (listOptions != null && listOptions.size() != 0) {
                String result = value + ":";
                boolean first = true;
                for (String v : listOptions) {
                    if (v.isEmpty()) continue;
                    if (first) {
                        first = false;
                        result = result + v;
                    } else {
                        result = result+"|" + v;
                    }
                }
                return result;
            }
        }
        return value;
    }

    /**
     * split options from properties file
     * @param value
     * @return
     */
    public Parameter setValueFromProperties(String value) {
        if (type.equals(ParameterType.LIST)) {
            if (value.contains(":")) {
                String[] split = value.split(":");
                setValue(split[0]);
                split = split[1].split("|");
                listOptions.clear();
                listOptions.addAll(Arrays.asList(split));
                return this;
            }

        }
        return setValue(value);
    }

    public Parameter setValue(String value) {

        if (type.equals(ParameterType.FOLDER) || type.equals(ParameterType.FILE)) {
            if (value.toLowerCase().startsWith("c:\\")) {
                value = value.replace("/", "\\");
            }
        }
        if (value != this.value) {
            this.value = value;
            informSubscribers();


        }
        return this;
    }

    private void informSubscribers() {
        if (!preventCircular) {
            preventCircular = true;
            for (Parameter parameter : subscribers) {
                parameter.refreshData();
            }
            preventCircular = false;
        } else {
            // TODO: 02-Feb-20 add logging for circular dependency cases.
        }
    }

    /**
     * trigger datarefresh in a task parameter. task may inform UI (if there is any) of the change.
     */
    private void refreshData() {
        iInstallationTask.refreshData(installationContext, this);
    }

    boolean preventCircular = false;

    public Boolean getBoolean() {
        String value = this.value != null ? this.value : (defaultValue != null ? defaultValue : "false");
        return Boolean.parseBoolean(value);

    }

    public String getSplitValue() {

        String v = getValue();
        String[] split = v.split(":");

        if (split.length > 0) return split[1];
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
        return "***** Parameter:\n " + "name: " + (name == null ? "no name is defined \n" : name + "\n") + " description: " +
                (description == null ? "no descriptions is defined \n " : description + "\n") + " hasvalue: " + hasValue + "\n" +
                (hasValue ? " default value: " + (defaultValue == null ? "no default value is defined \n" : defaultValue + "\n") + " current value: " + (value == null ? "no value is defined\n" : value + "\n") : " This is a parameter without value\n");
    }


    public ParameterSource getSource() {
        return source;
    }

    public Parameter setSource(ParameterSource source) {
        this.source = source;
        return this;
    }

    /**
     * get Parameter type.
     *
     * @return
     */
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

    public Double getMinDoubleValue() {
        return minDoubleValue;
    }

    public Parameter setMinDoubleValue(Double minDoubleValue) {
        this.minDoubleValue = minDoubleValue;
        return this;
    }

    public Double getMaxDoubleValue() {
        return maxDoubleValue;
    }

    public Parameter setMaxDoubleValue(Double maxDoubleValue) {
        this.maxDoubleValue = maxDoubleValue;
        return this;
    }

    public boolean storeValue() {
        if (originalValue == null) {
            originalValue = value;
            originalSource = source;
            return true;
        }
        return false;
    }

    public boolean restoreValue() {
        if (originalValue != null) {
            value = originalValue;
            source = originalSource;
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

    /**
     * validate if email is in the correct format
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateEmail(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        String email = newValue.toString();
        email = email.trim();
        EmailValidator eValidator = EmailValidator.getInstance();
        if (eValidator.isValid(email)) return true;
        if (validationMessage != null) validationMessage.setMessage(email + " is not a valid email");
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
    public static interface TransformParameter {
        String transform(Parameter effected, Parameter effecting);
    }

    @FunctionalInterface
    public static interface parameterValidator {
        boolean validate(InstallationContext context, Parameter toValid, Object newValue, ValidationMessage message);
    }

    public Parameter.parameterValidator getParameterValidator() {
        return parameterValidator;
    }

    public Parameter setParameterValidator(Parameter.parameterValidator parameterValidator) {
        this.parameterValidator = parameterValidator;
        return this;
    }

    public boolean validate(InstallationContext context, Object newValue, ValidationMessage validationMessage) {
        if (parameterValidator != null) {
            return parameterValidator.validate(context, this, newValue, validationMessage);
        }
        return true;
    }

    public static boolean validateURL(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        boolean result = urlValidator.isValid(newValue.toString());
        if (!result) validationMessage.setMessage("URL is not a valid URL");
        return result;
    }

    public static boolean validateDouble(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        try {
            double value = Double.parseDouble(newValue.toString());
            if (parameter.getMaxDoubleValue() != Double.MAX_VALUE) {
                if (value > parameter.getMaxDoubleValue()) {
                    validationMessage.setMessage("Value is too big, max value is: " + parameter.getMaxDoubleValue());
                    return false;
                }
            }
            if (parameter.getMinDoubleValue() != Double.MIN_VALUE) {
                if (value < parameter.getMinDoubleValue()) {
                    validationMessage.setMessage("Value is too small, min value is: " + parameter.getMinDoubleValue());
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            validationMessage.setMessage("Value is not a double");
            return false;
        }
    }

    public static boolean validateInteger(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        try {
            Integer value = Integer.parseInt(newValue.toString());
            if (parameter.getMaxValue() != Integer.MAX_VALUE) {
                if (value > parameter.getMaxValue()) {
                    validationMessage.setMessage("Value is too big, max value is: " + parameter.getMaxValue());
                    return false;
                }
            }
            if (parameter.getMinValue() != Integer.MIN_VALUE) {
                if (value < parameter.getMinValue()) {
                    validationMessage.setMessage("Value is too small, min value is: " + parameter.getMinValue());
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            validationMessage.setMessage("Value is not an Integer");
            return false;
        }
    }

    /**
     * validate if new value is in a list
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateList(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        if (parameter.getListOptions() != null) {
            if (parameter.getListOptions().contains(newValue)) {
                return true;
            }
            validationMessage.setMessage("The value: <" + newValue + "> is not one of the possible values in the list");
            return false;
        }
        return true;
    }

    /**
     * check if the new value is left or right side of a pair
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateDualList(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        if (parameter.getListOptions() != null) {
            for (String option : parameter.getListOptions()) {
                if (option.contains(":")) {
                    String[] split = option.split(":");
                    if (split[0].equals(newValue) || split[1].equals(newValue)) return true;
                }
            }
            if (parameter.getListOptions().contains(newValue)) {
                return true;
            }
            validationMessage.setMessage("The value: <" + newValue + "> is not one of the possible values in the list");
            return false;
        }
        return true;
    }

    /**
     * Validate if folder exists.
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateExistingFolder(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        File file = new File(getReplaced(context, newValue.toString(), parameter, null));
        if (!file.exists()) {
            if (parameter.isAutocreate()) {
                if (file.mkdirs()) {
                    context.getLogger().info("have created the required folder: " + file.getAbsolutePath());

                }
            } else {
                validationMessage.setMessage("Cannot locate folder: " + newValue);
                return false;
            }
        }
        return true;
    }

    /**
     * validate of file exists
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateExistingFile(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        File file = new File(getReplaced(context, newValue.toString(), parameter, null));
        if (!file.exists()) {
            validationMessage.setMessage("Cannot locate file: " + newValue);
            return false;
        }
        return true;
    }

    public static boolean validateHttpPort(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        try {
            int p = Integer.parseInt(newValue.toString());
            if (p != 80 && (p > 8085 || p < 8079)) {
                validationMessage.setMessage("Port must be either 80 or between 8080 and 8085 \n, provided value is " + p);
                return false;
            }
        } catch (Exception e) {
            validationMessage.setMessage("Port must be either 80 or between 8080 and 8085 \n, provided value is: " + newValue.toString());
            return false;
        }
        return true;
    }

    public static boolean validatePort(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        try {
            int p = Integer.parseInt(newValue.toString());
            if (p < 1000 || p > 65535) {
                validationMessage.setMessage("Port must be greater than 1000 and lower than 65535, value is: " + p);
                return false;
            }
        } catch (Exception e) {
            validationMessage.setMessage("Port must be either 80 or between 8080 and 8085 \n, provided value is: " + newValue.toString());
            return false;
        }
        return true;
    }

    /**
     * simple validator for heap size todo:check available memory
     *
     * @param context
     * @param parameter
     * @param newValue
     * @param validationMessage
     * @return
     */
    public static boolean validateHeap(InstallationContext context, Parameter parameter, Object
            newValue, ValidationMessage validationMessage) {
        try {
            int heap = Integer.parseInt(newValue.toString());
            if (heap % 256 != 0 || heap < 768) {
                validationMessage.setMessage("Heap size must be divisible by 256 and equal or larger than 768 MBytes, value was:  " + newValue.toString());
                return false;
            }

        } catch (Exception e) {
            validationMessage.setMessage("Heap size must be divisible by 256 and equal or larger than 768 MBytes , value was: " + newValue.toString());
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

    public OperatingSystem getOs() {
        return os;
    }

    /**
     * add subscription using parameter names of subscriber and subscribe to.
     *
     * @param context
     * @param subscriber
     * @param subscribeTo
     * @return
     */
    public static boolean subscribe(InstallationContext context, String subscriber, String subscribeTo) {
        if (context != null) {
            Parameter parameterSubscriber = context.getParameter(subscriber);
            if (parameterSubscriber != null) {
                Parameter parameterSubscribeto = context.getParameter(subscribeTo);
                if (parameterSubscribeto != null) {
                    parameterSubscribeto.addSubscriber(parameterSubscriber);
                    return true;
                }
            }


        }
        return false;
    }

    public Parameter setOs(OperatingSystem os) {
        this.os = os;
        return this;
    }

    public static String getReplaced(InstallationContext installationContext, String result, Parameter
            parameter, Parameter onlyParameter) {
        Logger logger = installationContext.getLogger();
        if (installationContext.isExtraLogs()) logger.info("got to replace: " + result);
        int a = result.indexOf("&");
        if (a > -1) {
            int index = a + 2;
            int i = 1;
            while (index <= result.length()) {
                if (!result.substring(a + i++, index++).matches("[a-zA-Z0-9]+")) break;

            }
            if (index > result.length()) index++; //special case

            String toReplace = result.substring(a + 1, index - 2);
            if (installationContext.isExtraLogs())
                logger.info("to replace is, this is the string we are looking for " + toReplace);
            boolean doReplace = onlyParameter != null ? toReplace.equals(onlyParameter.getName()) : true; //if onlyParameter was provided, then use it only as key

            String newString = installationContext.getParamaters().getValue(toReplace.substring(0));
            if (installationContext.isExtraLogs()) logger.info("this is the new replacement:  " + newString);
            if (newString != null && !newString.isEmpty() && doReplace) {
                parameter.setReferencedParameter(toReplace);
                result = result.replace(result.substring(a, index - 2), newString);

                if (installationContext.isExtraLogs()) logger.info("after replacement: " + result);
            }
        } else {
            if (installationContext.isExtraLogs()) logger.info("did not find & in the string " + result);
        }
        return result;
    }

    /**
     * A Parameter that should be refreshed when there is a change in value.
     *
     * @return
     */

    public void addSubscriber(Parameter parameter) {
        if (!subscribers.contains(parameter)) subscribers.add(parameter);
    }

    public void removeSubscriber(Parameter parameter) {
        if (subscribers.contains(parameter)) subscribers.remove(parameter);
    }

    public void testSubscribers() {
        informSubscribers();
    }

    public IInstallationTask getiInstallationTask() {
        return iInstallationTask;
    }

    public Parameter setiInstallationTask(IInstallationTask iInstallationTask) {
        this.iInstallationTask = iInstallationTask;
        return this;
    }

    public InstallationContext getInstallationContext() {
        return installationContext;
    }

    public Parameter setInstallationContext(InstallationContext installationContext) {
        this.installationContext = installationContext;
        return this;
    }

    public boolean isEditable() {
        return editable;
    }

    public Parameter setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }
}

