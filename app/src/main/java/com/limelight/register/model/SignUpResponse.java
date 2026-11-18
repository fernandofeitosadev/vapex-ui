package com.limelight.register.model;

public class SignUpResponse {

    private int status;
    private boolean success;
    private String message;
    private String action;

    public int getStatus() {
        return status;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess(){
        return success;
    }
}
