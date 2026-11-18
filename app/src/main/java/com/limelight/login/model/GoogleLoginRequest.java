package com.limelight.login.model;

public class GoogleLoginRequest {
    private String email;
    private String username;
    private String googleAuthCode;
    private String googleRedirectUri;
    private String avatarUrl;

    public GoogleLoginRequest(String email, String username, String googleAuthCode, String googleRedirectUri, String avatarUrl) {
        this.email = email;
        this.username = username;
        this.googleAuthCode = googleAuthCode;
        this.googleRedirectUri = googleRedirectUri;
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getGoogleAuthCode() {
        return googleAuthCode;
    }

    public String getGoogleRedirectUri() {
        return googleRedirectUri;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
