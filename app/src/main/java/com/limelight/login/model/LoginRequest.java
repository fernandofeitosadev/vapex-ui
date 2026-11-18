package com.limelight.login.model;

public class LoginRequest {

    private String email;
    private String password;
    private String googleAuthCode;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getGoogleAuthCode() {
        return googleAuthCode;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setGoogleAuthCode(String googleAuthCode) {
        this.googleAuthCode = googleAuthCode;
    }
}
