package com.flexicore.installer.model;

import java.util.concurrent.ConcurrentHashMap;

public class Parameters {
    ConcurrentHashMap<String,Parameter> map=new ConcurrentHashMap<>();
    public Parameter getParameter(String key) {
        return  map.get(key);
    }
    public Parameter addParameter(String name,String description,String defaultValue) {
        Parameter parameter=new Parameter(name,description,defaultValue);
        map.put(name,parameter);
        return parameter;

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
