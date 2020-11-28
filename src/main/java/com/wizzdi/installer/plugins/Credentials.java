package com.wizzdi.installer.plugins;

public class Credentials {
    String email="";
    String Password="";

    public String getEmail() {
        return email;
    }

    public Credentials setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return Password;
    }

    public Credentials setPassword(String password) {
        Password = password;
        return this;
    }
}
