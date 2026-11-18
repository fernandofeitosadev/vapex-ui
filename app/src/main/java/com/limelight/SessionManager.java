package com.limelight;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "auth";
    private static final String KEY_TOKEN = "jwt_token";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        String token = prefs.getString(KEY_TOKEN, null);
        return token != null && !token.trim().isEmpty();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}