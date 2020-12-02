package com.flexicore.installer.model;

import java.util.ArrayList;
import java.util.List;

public class ScriptResult {
    private int result;
    private List<String> output=new ArrayList<>();
    private List<String> errors=new ArrayList<>();

    public List<String> getOutput() {
        return output;
    }

    public List<String> getErrors() {
        return errors;
    }

    public int getResult() {
        return result;
    }

    public ScriptResult setResult(int result) {
        this.result = result;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (output.size()!=0) {

            for (String line : output) {
                sb.append(line + "\n");
            }
        }
        StringBuilder errorSB = new StringBuilder();
        if (errors.size()!=0) {

            for (String line : errors) {
                errorSB.append(line + "\n");
            }
        }
       return  "ScriptResult{" +
                "result=" + String.valueOf(result)+
                ", output=" + (output.size()==0 ?"": sb.toString())+
                ", errors=" + (errors.size()==0 ?"" :errorSB.toString())+
                '}';
    }
}
