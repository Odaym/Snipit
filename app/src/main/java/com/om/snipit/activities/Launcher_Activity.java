package com.om.snipit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.om.snipit.classes.Constants;

public class Launcher_Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean(Constants.SEEN_APP_INTRO_PREF, false)) {
            startActivity(new Intent(Launcher_Activity.this, Books_Activity.class));
        } else {
            startActivity(new Intent(Launcher_Activity.this, IntroSlider_Activity.class));
        }
    }
}
