package com.flexicore.installer.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PowerShellReturn {
    private int result;
    private List<String> error=new ArrayList<>();
    private List<String> output=new ArrayList<>();

    public int getResult() {
        return result;
    }

    public PowerShellReturn setResult(int result) {
        this.result = result;
        return this;
    }

    public List<String> getError() {
        return error;
    }

    public PowerShellReturn setError(List<String> error) {
        this.error = error;
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
        error.add(errorLine);
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
    public PowerShellResponse getResponse() {
        StringBuilder sbOutput=new StringBuilder();
        PowerShellResponse response = null;
        for (String s: output) {
            sbOutput.append(s);
            sbOutput.append("\n");
        }
        StringBuilder sbError=new StringBuilder();
        for (String s: error) {
            sbError.append(s);
            sbError.append("\n");
        }
        if (getResult()==0 && error.size()==0) {
            response=new PowerShellResponse(false,sbOutput.toString(),false);
            return response;
        }
        return new PowerShellResponse(true,sbError.toString(),false);
    }
}
