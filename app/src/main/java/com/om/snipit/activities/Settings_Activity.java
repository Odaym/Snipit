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
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.atomic.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.GMailSender;
import com.om.snipit.classes.Helper_Methods;
import com.om.snipit.classes.Param;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class Settings_Activity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ProgressDialog sendEmailFeedbackDialog;
    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<Param, Integer> paramDAO;
    private Param animationsParam;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        paramDAO = getHelper().getParamDAO();

        animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);

        if (animationsParam.isEnabled())
            overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);

        final Helper_Methods helperMethods = new Helper_Methods(this);

        final LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();

        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);

        if (helperMethods.getCurrentapiVersion() >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.darker_red));
            toolbar.setElevation(25f);
        }

        toolbar.setBackgroundColor(getResources().getColor(R.color.red));

        root.addView(toolbar, 0);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();

                if (animationsParam.isEnabled())
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
                feedbackSendingSummaryTV.append(Html.fromHtml("<a href=\"mailto:apps.atomic@gmail.com\">apps.atomic@gmail.com</a>"));
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
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case "pref_key_tutorial_mode":
                Param tutorialParam = paramDAO.queryForId(Constants.TUTORIAL_MODE_DATABASE_VALUE);

                Param seenBookParam = paramDAO.queryForId(Constants.SEEN_BOOK_TUTORIAL_DATABASE_VALUE);
                Param seenBookmarkParam = paramDAO.queryForId(Constants.SEEN_BOOKMARK_TUTORIAL_DATABASE_VALUE);
                Param seenCreateBookParam = paramDAO.queryForId(Constants.SEEN_CREATE_BOOK_TUTORIAL_DATABASE_VALUE);

                if (sharedPreferences.getBoolean("pref_key_tutorial_mode", true)) {
                    FlurryAgent.logEvent("Tutorial_Mode_ON");

                    //Set all coachmarks to Unseen
                    seenBookParam.setEnabled(true);
                    seenBookmarkParam.setEnabled(true);
                    seenCreateBookParam.setEnabled(true);

                    tutorialParam.setEnabled(true);
                } else {
                    FlurryAgent.logEvent("Tutorial_Mode_OFF");

                    //Set all coachmarks to Seen
                    seenBookParam.setEnabled(false);
                    seenBookmarkParam.setEnabled(false);
                    seenCreateBookParam.setEnabled(false);

                    tutorialParam.setEnabled(false);
                }

                paramDAO.update(seenBookParam);
                paramDAO.update(seenBookmarkParam);
                paramDAO.update(seenCreateBookParam);

                paramDAO.update(tutorialParam);

                break;
            case "pref_key_animations_mode":
                Param animationsParam = paramDAO.queryForId(Constants.ANIMATIONS_DATABASE_VALUE);

                //Reflect the change in the database because this preference has already changed
                if (sharedPreferences.getBoolean("pref_key_animations_mode", true)) {
                    FlurryAgent.logEvent("Layout_Animations_ON");

                    animationsParam.setEnabled(true);
                } else {
                    FlurryAgent.logEvent("Layout_Animations_OFF");

                    animationsParam.setEnabled(false);
                }

                paramDAO.update(animationsParam);

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

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }

        return databaseHelper;
    }
}
