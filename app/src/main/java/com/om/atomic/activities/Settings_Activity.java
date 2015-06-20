package com.om.atomic.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
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

        final LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();

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

        Preference sendFeedback = findPreference("pref_key_send_feedback");

        sendFeedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder alert = new AlertDialog.Builder(Settings_Activity.this);

                LayoutInflater inflater = (LayoutInflater) Settings_Activity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View alertComposeFeedback = inflater.inflate(R.layout.alert_send_feedback, root, false);

                final EditText inputFeedbackET = (EditText) alertComposeFeedback.findViewById(R.id.feedbackET);
                inputFeedbackET.setHintTextColor(Settings_Activity.this.getResources().getColor(R.color.edittext_hint_color));
                inputFeedbackET.setSelection(inputFeedbackET.getText().length());

                alert.setPositiveButton(Settings_Activity.this.getResources().getString(R.string.pref_send_feedback_button_title), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });

                alert.setNegativeButton(Settings_Activity.this.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                inputFeedbackET.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager keyboard = (InputMethodManager)
                                Settings_Activity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(inputFeedbackET, 0);
                    }
                }, 0);

                TextView feedbackSendingSummaryTV = (TextView) alertComposeFeedback.findViewById(R.id.feedbackSendingSummaryTV);
                feedbackSendingSummaryTV.append(" ");
                feedbackSendingSummaryTV.append(Html.fromHtml("<a href=\"mailto:apps.atomic@gmail.com\">apps.atomic@gmail.com</a>"));
                feedbackSendingSummaryTV.setMovementMethod(LinkMovementMethod.getInstance());

                alert.setTitle(Settings_Activity.this.getResources().getString(R.string.pref_sendfeedback_alert_title));
                alert.setView(alertComposeFeedback);
                alert.show();

                return false;
            }
        });

        Preference populateSampleData = findPreference("pref_key_populate_sample_data");

        populateSampleData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Helper_Methods.isInternetAvailable(Settings_Activity.this)) {
                    FlurryAgent.logEvent("Test_Drive");
                    EventBus_Singleton.getInstance().post(new EventBus_Poster("populate_sample_data"));
                    finish();
                } else {
                    Crouton.makeText(Settings_Activity.this, getString(R.string.action_needs_internet), Style.INFO).show();
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

        switch (key) {
            case "pref_key_tutorial_mode":
                if (sharedPreferences.getBoolean("pref_key_tutorial_mode", true)) {
                    FlurryAgent.logEvent("Tutorial_Mode_ON");

                    //Set all coachmarks to Unseen
                    dbHelper.reverseParamsTruths(1, "False");
                    dbHelper.reverseParamsTruths(2, "False");
                    dbHelper.reverseParamsTruths(3, "False");
                } else {
                    FlurryAgent.logEvent("Tutorial_Mode_OFF");

                    //Set all coachmarks to Seen
                    dbHelper.reverseParamsTruths(1, "True");
                    dbHelper.reverseParamsTruths(2, "True");
                    dbHelper.reverseParamsTruths(3, "True");
                }
                break;
            case "pref_key_animations_mode":
                //Reflect the change in the database because this preference has already changed
                if (sharedPreferences.getBoolean("pref_key_animations_mode", true)) {
                    FlurryAgent.logEvent("Layout_Animations_ON");

                    dbHelper.reverseParamsTruths(10, "True");
                } else {
                    FlurryAgent.logEvent("Layout_Animations_OFF");

                    dbHelper.reverseParamsTruths(10, "False");
                }

                break;
        }
    }
}
