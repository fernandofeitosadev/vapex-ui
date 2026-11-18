package com.limelight.register.model;

public class SignUpRequest {
    private String username;
    private String email;
    private String password;
    private String googleAuthCode;

    public void setUsername(String username) {
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getGoogleAuthCode() {
        return googleAuthCode;
    }
}
