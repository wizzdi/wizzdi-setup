package com.flexicore.installer.model;

import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Describes a UserMessage that has no JavaFX dependency and can be used n Console and in JavaFX based UI
 */
public class UserMessage {
    public enum InputType {
        none,string,bool,list
    }
    boolean crlf=true;
    private String message;
    private int emphasize = 1; // the higher the number larger font will be used if UI present, console will use colors if possible
    /**
     * will be translated to JavaFX if UI is used.
     */
    private Ansi.Color color = BLACK; // this can be used to translate in JavaFX UI without dependency on JavaFX
    private Side side = Side.left;
    private String fontName="calibri";
    private int fontSize=10;
    private boolean useFont=false;

    /**
     * from here added support for returned input
     */
    private InputType inputType=InputType.none;

    private int inputWidth=70;
    private String prompt;
    private Side textFieldSide=Side.left;
    private List<String> options=new ArrayList<>();
    private String defaultValue;
    private Object value;
    /**
     * will be used as left margin, -1==ignore
     */
    private int leftMargin =-1;

    public enum Side {
        left, center, right // this is bit UI thing....
    }

    public String getMessage() {
        return message;
    }

    public UserMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public int getEmphasize() {
        return emphasize;
    }

    public UserMessage setEmphasize(int emphasize) {
        this.emphasize = emphasize;
        return this;
    }

    public Ansi.Color getColor() {
        return color;
    }

    public UserMessage setColor(Ansi.Color color) {
        this.color = color;
        return this;
    }

    public Side getSide() {
        return side;
    }

    public UserMessage setSide(Side side) {
        this.side = side;
        return this;
    }

    public boolean isCrlf() {
        return crlf;
    }

    public UserMessage setCrlf(boolean crlf) {
        this.crlf = crlf;
        return this;
    }

    public String getFontName() {
        return fontName;
    }

    public UserMessage setFontName(String fontName) {
        this.fontName = fontName;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public UserMessage setFontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public boolean isUseFont() {
        return useFont;
    }

    public UserMessage setUseFont(boolean useFont) {
        this.useFont = useFont;
        return this;
    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public UserMessage setLeftMargin(int leftMargin) {
        this.leftMargin = leftMargin;
        return this;
    }

    public InputType getInputType() {
        return inputType;
    }

    public UserMessage setInputType(InputType inputType) {
        this.inputType = inputType;
        return this;
    }

    public int getInputWidth() {
        return inputWidth;
    }

    public UserMessage setInputWidth(int inputWidth) {
        this.inputWidth = inputWidth;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public UserMessage setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public Side getTextFieldSide() {
        return textFieldSide;
    }

    public UserMessage setTextFieldSide(Side textFieldSide) {
        this.textFieldSide = textFieldSide;
        return this;
    }

    public List<String> getOptions() {
        return options;
    }

    public UserMessage setOptions(List<String> options) {
        this.options = options;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public UserMessage setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public UserMessage setValue(Object value) {
        this.value = value;
        return this;
    }
}
