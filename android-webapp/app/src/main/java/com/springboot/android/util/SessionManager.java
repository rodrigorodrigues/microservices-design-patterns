package com.springboot.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static final String PREF_NAME = "SpringBootSession";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CSRF_TOKEN = "csrf_token";
    private static final String KEY_CSRF_HEADER = "csrf_header";
    private static final String KEY_AUTHORITIES = "authorities";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getAuthToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUser(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void saveCsrfToken(String token, String headerName) {
        editor.putString(KEY_CSRF_TOKEN, token);
        editor.putString(KEY_CSRF_HEADER, headerName);
        editor.apply();
    }

    public String getCsrfToken() {
        return prefs.getString(KEY_CSRF_TOKEN, null);
    }

    public String getCsrfHeader() {
        return prefs.getString(KEY_CSRF_HEADER, "X-CSRF-TOKEN");
    }

    public void saveAuthorities(List<String> authorities) {
        Gson gson = new Gson();
        String json = gson.toJson(authorities);
        editor.putString(KEY_AUTHORITIES, json);
        editor.apply();
    }

    public List<String> getAuthorities() {
        String json = prefs.getString(KEY_AUTHORITIES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void clearAuthToken() {
        editor.remove(KEY_TOKEN);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
