package com.springboot.android;

import android.app.Application;
import com.springboot.android.api.ApiClient;

public class MainApplication extends Application {

    private static MainApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize API Client
        ApiClient.initialize(this);
    }

    public static MainApplication getInstance() {
        return instance;
    }
}
