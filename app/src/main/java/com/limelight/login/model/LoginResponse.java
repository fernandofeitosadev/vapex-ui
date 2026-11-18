package com.limelight.login.model;

public class LoginResponse {


    private int status;
    private String message;
    private String token;

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }
}
