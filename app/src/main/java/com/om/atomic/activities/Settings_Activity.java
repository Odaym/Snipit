package com.om.atomic.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.om.atomic.R;
import com.om.atomic.classes.DatabaseHelper;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("pref_key_tutorial_mode")) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            if (sharedPreferences.getBoolean("pref_key_tutorial_mode", true)) {
                //Set all coachmarks to Unseen
                dbHelper.switchCoachmarksSeenParam(1, "False");
                dbHelper.switchCoachmarksSeenParam(2, "False");
                dbHelper.switchCoachmarksSeenParam(3, "False");
            } else {
                //Set all coachmarks to Seen
                dbHelper.switchCoachmarksSeenParam(1, "True");
                dbHelper.switchCoachmarksSeenParam(2, "True");
                dbHelper.switchCoachmarksSeenParam(3, "True");
            }
        } else if (key.equals("pref_key_about")) {

        }
    }
}
