package com.om.snipit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.om.snipit.classes.Constants;

public class Launcher_Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //If prefs do not yet contain the APP_INTRO_ENABLED preference or if prefs does contain it and its value is true
        if (!getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE).contains(Constants.APP_INTRO_ENABLED) || getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE).getBoolean(Constants.APP_INTRO_ENABLED, false))
            startActivity(new Intent(Launcher_Activity.this, IntroSlider_Activity.class));
        else
            startActivity(new Intent(Launcher_Activity.this, Books_Activity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }
}
