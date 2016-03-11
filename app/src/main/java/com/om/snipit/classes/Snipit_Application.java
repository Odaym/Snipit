package com.om.snipit.classes;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.om.snipit.BuildConfig;
import com.parse.Parse;

import io.fabric.sdk.android.Fabric;

public class Snipit_Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize Parse
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, Constants.PARSE_APPLICATION_ID, Constants.PARSE_CLIENT_ID);

        if (!BuildConfig.DEBUG) {
            //Initialize Crashlytics
            Fabric.with(this, new Crashlytics());
        }
    }
}
