package com.om.snipit.classes;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.flurry.android.FlurryAgent;

import io.fabric.sdk.android.Fabric;

public class Snipit_Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize Parse
//        Parse.enableLocalDatastore(this);
//        Parse.initialize(this, Constants.PARSE_APPLICATION_ID, Constants.PARSE_CLIENT_ID);

        if (Constants.APPLICATION_CODE_STATE.equals("PRODUCTION")) {
            //Initialize Flurry Analytics
            FlurryAgent.setLogEnabled(false);
            FlurryAgent.init(this, Constants.FLURRY_API_KEY);

            //Initialize Crashlytics
            Fabric.with(this, new Crashlytics());
        }
    }
}
