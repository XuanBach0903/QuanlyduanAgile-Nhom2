package com.example.appbanghe.network;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "app_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_IMG_URL = "user_img_url";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_EMAIL)
                .remove(KEY_IMG_URL)
                .apply();
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void setUserImgUrl(String imgUrl) {
        prefs.edit().putString(KEY_IMG_URL, imgUrl).apply();
    }

    public String getUserImgUrl() {
        return prefs.getString(KEY_IMG_URL, "");
    }
}
