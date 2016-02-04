package com.om.snipit.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.om.snipit.R;
import com.om.snipit.classes.Constants;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences prefs;
    private SharedPreferences.Editor prefsEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();

        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.darker_red));
            toolbar.setElevation(25f);
        }

        toolbar.setBackgroundColor(getResources().getColor(R.color.red));
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);

        prefs = getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);

        root.addView(toolbar, 0);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        Preference aboutOpenSourceLibs = findPreference("pref_key_open_source_libs");

        aboutOpenSourceLibs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent startAboutOpenSourceLibs_Activity = new Intent(Settings_Activity.this, Open_Source_Libs_Activity.class);
                startActivity(startAboutOpenSourceLibs_Activity);
                return false;
            }
        });

        final CheckBoxPreference keepIntroSlider = (CheckBoxPreference) findPreference("pref_key_configuration_intro_slider");

        if (prefs.getBoolean(Constants.APP_INTRO_ENABLED, false))
            keepIntroSlider.setChecked(true);
        else
            keepIntroSlider.setChecked(false);

        keepIntroSlider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                prefsEditor = prefs.edit();
                prefsEditor.putBoolean(Constants.APP_INTRO_ENABLED, !prefs.getBoolean(Constants.APP_INTRO_ENABLED, false));
                prefsEditor.apply();

                keepIntroSlider.setChecked(!keepIntroSlider.isChecked());

                return false;
            }
        });

//        Preference signOut = findPreference("pref_key_sign_out");
//
//        signOut.setSummary(prefs.getString(Constants.USER_EMAIL_ADDRESS, ""));
//        signOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                prefsEditor = prefs.edit();
//                prefsEditor.putBoolean(Constants.USER_LOGGED_IN, false);
//                prefsEditor.apply();
//
//                startActivity(new Intent(Settings_Activity.this, Login_Activity.class));
//
//                return false;
//            }
//        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
