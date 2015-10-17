package com.om.snipit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.om.snipit.classes.Constants;

public class Launcher_Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean(Constants.USER_LOGGED_IN, false)) {
            startActivity(new Intent(Launcher_Activity.this, Books_Activity.class));
        } else {
            startActivity(new Intent(Launcher_Activity.this, Login_Activity.class));
        }
    }
}
