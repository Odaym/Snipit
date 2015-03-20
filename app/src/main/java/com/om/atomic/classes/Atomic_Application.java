package com.om.atomic.classes;

import android.app.Application;

public class Atomic_Application extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
//                .setDefaultFontPath("fonts/HelveticaNeue-Thin.otf")
//                .setFontAttrId(R.attr.fontPath)
//                .build());
    }
}
