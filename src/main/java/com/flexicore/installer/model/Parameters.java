package com.flexicore.installer.model;

import com.flexicore.installer.interfaces.IInstallationTask;

import java.util.*;

public class Parameters {
    private TreeMap<String, Parameter> map = new TreeMap<>();

    public Parameter getParameter(String key) {
        return map.get(key);
    }

    public Collection<Parameter> getValues() {
        return map.values();
    }

    private HashMap<IInstallationTask, List<Parameter>> bytasks;

    public Set<String> getKeys() {
        return map.keySet();
    }

    public Parameters addParameter(String name, String description, Boolean hasValue, String defaultValue) {
        Parameter parameter = new Parameter(name, description, hasValue, defaultValue);
        map.put(name, parameter);
        return this;

    }

    public List<Parameter> byTask(IInstallationTask task) {
        if (bytasks != null) {

            return bytasks.get(task);
        }
        return null;
    }

    public void sort() {
        if (bytasks==null) return;
        Comparator<Parameter> parameterComparator = new Comparator<Parameter>() {
            @Override
            public int compare(Parameter p1, Parameter p2) {
                if (p1.getOrdinal() == p2.getOrdinal()) {
                    return p1.getName().compareTo(p2.getName());
                } else {
                    if (p1.getOrdinal() > p2.getOrdinal()) return -1;
                    return 1;
                }
            }
        };
        for (List<Parameter> list : bytasks.values()) {
            Collections.sort(list, parameterComparator);
        }
    }

    public Parameters addParameter(Parameter parameter, IInstallationTask task) {
        map.put(parameter.getName(), parameter);
        if (task != null) {
            if (bytasks == null) bytasks = new HashMap<>();
            List<Parameter> list = null;
            if ((list = bytasks.get(task)) == null) bytasks.put(task, list = new ArrayList<Parameter>());
            list.add(parameter);
        }
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
    public String getValueFromSplit(String key) {
        if (map.containsKey(key)) {
            return map.get(key).getSplitValue();
        }
        return "";
    }

}
