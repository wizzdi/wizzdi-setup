package com.flexicore.installer.model;

import org.fusesource.jansi.Ansi;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * describes user required action
 * this should be supported on both UI and Console.
 */
public class UserAction {
    private boolean useAnsiColorsInConsole = true;
    private ResponseType responseType;
    private List<UserMessage> messages = new ArrayList<>();
    private UserResponse defaultResponse = UserResponse.CONTINUE;
    private String defaultResponseForString="";
    private UserResponse[] possibleAnswers;
    private String optionalPrompt="";

    public enum ResponseType {
        FROMELIST,BOOLEAN,STRING,FILE,EXISTINGFILE, EXISTINGFOLDER,LIST
    }
    public static UserAction getSample() {
        UserAction result=new UserAction();
        result.getMessages().add(new UserMessage().setColor(Ansi.Color.RED).setMessage("Hello there").setCrlf(true));
        result.getMessages().add(new UserMessage().setColor(Ansi.Color.GREEN).setMessage("Hello there 2").setCrlf(true));
        result.getMessages().add(new UserMessage().setColor(Ansi.Color.CYAN).setMessage("Hello there 2").setCrlf(false));
        result.setDefaultResponse(UserResponse.FORCESTOP);
        result.setResponseType(ResponseType.EXISTINGFILE);
        result.setDefaultResponseForString("/dev/mdbinstall.log");
        result.setOptionalPrompt("Please do that or that");


        result.setPossibleAnswers(new UserResponse[]{UserResponse.IGNORE,UserResponse.NO});
        return result;
    }


    public List<UserMessage> getMessages() {
        return messages;
    }

    public UserAction setMessages(List<UserMessage> messages) {
        this.messages = messages;
        return this;
    }

    public UserResponse getDefaultResponse() {
        return defaultResponse;
    }

    public UserAction setDefaultResponse(UserResponse defaultResponse) {
        this.defaultResponse = defaultResponse;
        return this;
    }

    public boolean isUseAnsiColorsInConsole() {
        return useAnsiColorsInConsole;
    }

    public UserAction setUseAnsiColorsInConsole(boolean useAnsiColorsInConsole) {
        this.useAnsiColorsInConsole = useAnsiColorsInConsole;
        return this;
    }

    public UserResponse[] getPossibleAnswers() {
        return possibleAnswers;
    }

    public UserAction setPossibleAnswers(UserResponse[] possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
        return this;
    }
    public String getAllAnswers() {
        String result="";
        if (possibleAnswers!=null) {
            for (UserResponse response:possibleAnswers) {
               if (result.equals(""))  {
                   result=response.toString();
               }else result+=","+response.toString();
            }
        }
        return result;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public UserAction setResponseType(ResponseType responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getDefaultResponseForString() {
        return defaultResponseForString;
    }

    public UserAction setDefaultResponseForString(String defaultResponseForString) {
        this.defaultResponseForString = defaultResponseForString;
        return this;
    }

    public String getOptionalPrompt() {
        return optionalPrompt;
    }

    public UserAction setOptionalPrompt(String optionalPrompt) {
        this.optionalPrompt = optionalPrompt;
        return this;
    }
}
