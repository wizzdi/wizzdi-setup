package com.flexicore.installer.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

public class Parameters {
    private TreeMap<String,Parameter> map=new TreeMap<>();
    public Parameter getParameter(String key) {
        return  map.get(key);
    }
    public Collection<Parameter> getValues() {
        return map.values();
    }
    public Set<String> getKeys() {
        return map.keySet();
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
        if (map.containsKey(key)) {
            return map.get(key).getBoolean();
        }
        return false;
    }
    public String getValue(String key) {

        if (map.containsKey(key)) {
            return map.get(key).getValue();
        }
        return "";
    }

}
