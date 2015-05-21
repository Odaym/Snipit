package com.om.atomic.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.om.atomic.R;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.Helper_Methods;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Helper_Methods helperMethods = new Helper_Methods(this);

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();

        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.darker_red));
            toolbar.setElevation(25f);
        }
        toolbar.setBackgroundDrawable(getResources().getDrawable(R.color.red));

        root.addView(toolbar, 0);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        Preference populateSampleData = findPreference("pref_key_populate_sample_data");

        populateSampleData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Helper_Methods.isInternetAvailable(Settings_Activity.this)) {
                    finish();
                    EventBus_Singleton.getInstance().post(new EventBus_Poster("populate_sample_data"));
                } else {
                    Crouton.makeText(Settings_Activity.this, getString(R.string.action_needs_internet), Style.ALERT).show();
                }
                return false;
            }
        });

        Preference aboutOpenSourceLibs = findPreference("pref_key_open_source_libs");

        aboutOpenSourceLibs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent startAboutOpenSourceLibs_Activity = new Intent(Settings_Activity.this, Open_Source_Libs_Activity.class);
                startActivity(startAboutOpenSourceLibs_Activity);
                return false;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        if (key.equals("pref_key_tutorial_mode")) {
            if (sharedPreferences.getBoolean("pref_key_tutorial_mode", true)) {
                //Set all coachmarks to Unseen
                dbHelper.reverseParamsTruths(1, "False");
                dbHelper.reverseParamsTruths(2, "False");
                dbHelper.reverseParamsTruths(3, "False");
            } else {
                //Set all coachmarks to Seen
                dbHelper.reverseParamsTruths(1, "True");
                dbHelper.reverseParamsTruths(2, "True");
                dbHelper.reverseParamsTruths(3, "True");
            }
        } else if (key.equals("pref_key_animations_mode")) {
            if (sharedPreferences.getBoolean("pref_key_animations_mode", true)) {
                dbHelper.reverseParamsTruths(10, "False");
            } else {
                dbHelper.reverseParamsTruths(10, "True");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
            overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
