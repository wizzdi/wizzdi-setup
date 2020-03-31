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
    private ResponseType responseType=ResponseType.BOOLEAN;
    private String title;
    private List<UserMessage> messages = new ArrayList<>();
    private UserResponse defaultResponse = UserResponse.CONTINUE;
    private String defaultResponseForString="";
    private UserResponse[] possibleAnswers;
    private String optionalPrompt="";
    private int defaultLeftMargin=-1;

    public enum ResponseType {
        FROMLIST,BOOLEAN,STRING,FILE,EXISTINGFILE, EXISTINGFOLDER,LIST
    }
    public void addMessage(UserMessage userMessage) {
        messages.add(userMessage);
    }
    public void clearMessages() {
        messages.clear();
    }


    /**
     * list of {@link UserMessage} to be displayed on console
     * @return
     */
    public List<UserMessage> getMessages() {
        return messages;
    }

    public UserAction setMessages(List<UserMessage> messages) {
        this.messages = messages;
        return this;
    }

    /**
     * The default response for FROMLIST
     * @return
     */
    public UserResponse getDefaultResponse() {
        return defaultResponse;
    }

    public UserAction setDefaultResponse(UserResponse defaultResponse) {
        this.defaultResponse = defaultResponse;
        return this;
    }

    /**
     * if true, will use console colors as defined in {@link UserMessage} type
     * @return
     */
    public boolean isUseAnsiColorsInConsole() {
        return useAnsiColorsInConsole;
    }

    public UserAction setUseAnsiColorsInConsole(boolean useAnsiColorsInConsole) {
        this.useAnsiColorsInConsole = useAnsiColorsInConsole;
        return this;
    }

    /**
     * get all possible responses as an array
     * @return
     */
    public UserResponse[] getPossibleAnswers() {
        return possibleAnswers;
    }

    public UserAction setPossibleAnswers(UserResponse[] possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
        return this;
    }

    /**
     * get a string with possible responses for  FROMLIST
     * @return
     */
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

    /**
     * get the response type , can be FROMELIST,BOOLEAN,STRING,FILE,EXISTINGFILE, EXISTINGFOLDER,LIST
     * @return
     */
    public ResponseType getResponseType() {
        return responseType;
    }

    public UserAction setResponseType(ResponseType responseType) {
        this.responseType = responseType;
        return this;
    }

    /**
     * what is the default response for a String type
     * @return
     */
    public String getDefaultResponseForString() {
        return defaultResponseForString;
    }

    public UserAction setDefaultResponseForString(String defaultResponseForString) {
        this.defaultResponseForString = defaultResponseForString;
        return this;
    }

    /**
     * Add custom prompt message , will be displayed on console only
     * @return
     */
    public String getOptionalPrompt() {
        return optionalPrompt;
    }

    public UserAction setOptionalPrompt(String optionalPrompt) {
        this.optionalPrompt = optionalPrompt;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public UserAction setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getDefaultLeftMargin() {
        return defaultLeftMargin;
    }

    public UserAction setDefaultLeftMargin(int defaultLeftMargin) {
        this.defaultLeftMargin = defaultLeftMargin;
        return this;
    }
}
