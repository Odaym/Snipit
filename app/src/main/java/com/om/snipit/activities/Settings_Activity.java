package com.om.snipit.activities;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andreabaccega.widget.FormEditText;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.GMailSender;
import com.om.snipit.classes.Helper_Methods;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ProgressDialog sendEmailFeedbackDialog;

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

        root.addView(toolbar, 0);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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
                        if (Helper_Methods.isInternetAvailable(Settings_Activity.this)) {
                            if (inputFeedbackET.testValidity()) {
                                try {
                                    new SendFeedbackEmail().execute(inputFeedbackET.getText().toString());
                                } catch (Exception e) {
                                    Log.e("SendMail", e.getMessage(), e);
                                }
                            }
                        } else {
                            Crouton.makeText(Settings_Activity.this, getString(R.string.action_needs_internet), Style.ALERT).show();
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
                feedbackSendingSummaryTV.append(Html.fromHtml("<a href=\"mailto:snipit.me@gmail.com\">snipit.me@gmail.com</a>"));
                feedbackSendingSummaryTV.setMovementMethod(LinkMovementMethod.getInstance());

                alert.setTitle(Settings_Activity.this.getResources().getString(R.string.pref_sendfeedback_alert_title));
                alert.setView(alertComposeFeedback);
                alert.show();

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
                GMailSender sender = new GMailSender(Constants.FEEDBACK_EMAIL_FROM_ADDRESS, Constants.FEEDBACK_EMAIL_FROM_ADDRESS_PASSWORD);
                sender.sendMail(Constants.FEEDBACK_EMAIL_SUBJECT,
                        feedbackContent[0],
                        Constants.FEEDBACK_EMAIL_APPEARS_AS,
                        Constants.FEEDBACK_EMAIL_TO);
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
}
