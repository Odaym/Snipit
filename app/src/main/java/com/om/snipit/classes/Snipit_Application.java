package com.om.snipit.classes;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.om.snipit.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class Snipit_Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            //Initialize Crashlytics
            Fabric.with(this, new Crashlytics());
        }
    }
}
