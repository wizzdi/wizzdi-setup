package com.flexicore.installer.model;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.Color.*;

public class UserMessage {
    boolean crlf=true;
    private String message;
    private int emphasize = 1; // the higher the number larger font will be used if UI present, console will use colors if possible

    private Ansi.Color color = BLACK; // this can be used to translate in JavaFX UI without dependency on JavaFX
    private Side side = Side.left;
    private String fontName="calibri";
    private int fontSize=10;
    private boolean useFont=false;

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
}
