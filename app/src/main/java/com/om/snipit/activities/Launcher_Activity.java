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

        if (getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE).getBoolean(Constants.EXTRAS_USER_LOGGED_IN, false))
            startActivity(new Intent(Launcher_Activity.this, Books_Activity.class));
        else
            startActivity(new Intent(Launcher_Activity.this, Login_Activity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }
}
