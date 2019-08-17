package com.flexicore.installer.model;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Parameters {
    ConcurrentHashMap<String,Parameter> map=new ConcurrentHashMap<>();
    public Parameter getParameter(String key) {
        return  map.get(key);
    }
    public Collection<Parameter> getValues() {
        return map.values();
    }
    public Parameters addParameter(String name,String description,Boolean hasValue,String defaultValue) {
        Parameter parameter=new Parameter(name,description,hasValue,defaultValue);
        map.put(name,parameter);
        return this;

    }
    public Parameters addParameter(Parameter parameter) {
        map.put(parameter.getName(),parameter);
        return this;
    }
    public boolean getBooleanValue(String key) {
        if (map.contains(key)) {
            return map.get(key).getBoolean();
        }
        return false;
    }
    public String getValue(String key) {
        if (map.contains(key)) {
            return map.get(key).getValue();
        }
        return "";
    }

}
