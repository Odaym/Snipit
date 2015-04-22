package com.om.atomic.classes;

import android.app.Application;

import com.parse.Parse;

public class Atomic_Application extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        Parse.initialize(this, "0DrXCFgecw0uwqBxryaFSBVWEVeoqH0OFCN6KWnT", "Ur0tf0ORJ4pzAwvsHijIPLACCool19b38p8C4iQk");

//        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
//                .setDefaultFontPath("fonts/HelveticaNeue-Thin.otf")
//                .setFontAttrId(R.attr.fontPath)
//                .build());
    }
}
