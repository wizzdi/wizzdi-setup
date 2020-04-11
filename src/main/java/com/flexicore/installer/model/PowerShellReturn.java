package com.flexicore.installer.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PowerShellReturn {
    private int result;
    boolean error=false;
    private List<String> errorList =new ArrayList<>();
    private List<String> output=new ArrayList<>();

    public PowerShellReturn(int result) {
        this.result=result;
    }

    public int getResult() {
        return result;
    }

    public PowerShellReturn setResult(int result) {
        this.result = result;
        return this;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public PowerShellReturn setErrorList(List<String> errorList) {
        this.errorList = errorList;
        return this;
    }

    public List<String> getOutput() {
        return output;
    }

    public PowerShellReturn setOutput(List<String> output) {
        this.output = output;
        return this;
    }
    public void addError(String errorLine) {
        errorList.add(errorLine);
    }
    public void addOutput(String outputLine) {
        output.add(outputLine);
    }

    public void fillInput(Process proc,Logger logger) {
        InputStream is = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                addOutput(line);
            }
            reader.close();
        }catch (Exception e) {

        }
    }

    public void fillError(Process proc, Logger logger) {
        InputStream is = proc.getErrorStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                addError(line);
            }
            reader.close();
        }catch (Exception e) {

        }
    }

    public boolean isTimeout() {
        return false;
    }
    public boolean isError() {
        return errorList.size()!=0 || result!=0;
    }

    public String getCommandOutput() {
        StringBuilder sbOutput=new StringBuilder();

        for (String s: output) {
            sbOutput.append(s);
            sbOutput.append("\n");
        }
        return  sbOutput.toString();
    }
}
