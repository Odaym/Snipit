package com.om.atomic.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andreabaccega.widget.FormEditText;
import com.flurry.android.FlurryAgent;
import com.om.atomic.R;
import com.om.atomic.classes.Constants;
import com.om.atomic.classes.DatabaseHelper;
import com.om.atomic.classes.EventBus_Poster;
import com.om.atomic.classes.EventBus_Singleton;
import com.om.atomic.classes.GMailSender;
import com.om.atomic.classes.Helper_Methods;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ProgressDialog sendEmailFeedbackDialog;
    private DatabaseHelper dbHelper = new DatabaseHelper(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (dbHelper.getParam(null, Constants.ANIMATIONS_ENABLED_DATABASE_VALUE))
            overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        final Helper_Methods helperMethods = new Helper_Methods(this);

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
                onBackPressed();

                if (dbHelper.getParam(null, Constants.ANIMATIONS_ENABLED_DATABASE_VALUE))
                    overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
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

                final FormEditText inputFeedbackET = (FormEditText) alertComposeFeedback.findViewById(R.id.feedbackET);
                inputFeedbackET.setHintTextColor(Settings_Activity.this.getResources().getColor(R.color.edittext_hint_color));
                inputFeedbackET.setSelection(inputFeedbackET.getText().length());

                alert.setPositiveButton(Settings_Activity.this.getResources().getString(R.string.pref_send_feedback_button_title), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (inputFeedbackET.testValidity()) {
                            try {
                                new SendFeedbackEmail().execute(inputFeedbackET.getText().toString());
                            } catch (Exception e) {
                                Log.e("SendMail", e.getMessage(), e);
                            }
                        }
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

    private class SendFeedbackEmail extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            sendEmailFeedbackDialog = ProgressDialog.show(Settings_Activity.this, getResources().getString(R.string.loading_book_info_title),
                    getResources().getString(R.string.loading_sending_feedback), true);
        }

        @Override
        protected Void doInBackground(String... feedbackContent) {
            try {
                GMailSender sender = new GMailSender("oday.maleh@gmail.com", "EASYPASSWORDiseasy1+2+3+4+");
                sender.sendMail("In-app Feedback",
                        feedbackContent[0],
                        "oday.maleh@gmail.com",
                        "apps.atomic@gmail.com");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            sendEmailFeedbackDialog.hide();
            Crouton.makeText(Settings_Activity.this, getResources().getString(R.string.feedback_sent_alert), Style.CONFIRM).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
            //If Animations are enabled
            if (dbHelper.getParam(null, Constants.ANIMATIONS_ENABLED_DATABASE_VALUE))
                overridePendingTransition(R.anim.right_slide_in_back, R.anim.right_slide_out_back);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
