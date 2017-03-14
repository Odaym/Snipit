package com.om.snipit.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.EventBus_Poster;
import com.om.snipit.classes.EventBus_Singleton;
import com.om.snipit.classes.SnipitApplication;
import com.om.snipit.dagger.DaggerAppComponent;

import javax.inject.Inject;

public class SettingsActivity extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
    GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

  @Inject SharedPreferences prefs;
  @Inject SharedPreferences.Editor prefsEditor;

  @Bind(R.id.toolbar) Toolbar toolbar;

  private GoogleApiClient googleApiClient;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((SnipitApplication) getApplication()).getAppComponent().inject(this);

    final LinearLayout root =
        (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();

    ButterKnife.bind(root);

    Toolbar toolbar =
        (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(getResources().getColor(R.color.darker_red));
      toolbar.setElevation(25f);
    }

    toolbar.setBackgroundColor(getResources().getColor(R.color.red));
    toolbar.setTitleTextColor(getResources().getColor(R.color.white));
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);

    root.addView(toolbar, 0);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());

    GoogleSignInOptions gso =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();

    googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
        .build();

    googleApiClient.connect();

    addPreferencesFromResource(R.xml.preferences);

    PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(this);

    Preference aboutOpenSourceLibs = findPreference("pref_key_open_source_libs");

    aboutOpenSourceLibs.setOnPreferenceClickListener(preference -> {
      Intent startAboutOpenSourceLibs_Activity =
          new Intent(SettingsActivity.this, OpenSourceLibsActivity.class);
      startActivity(startAboutOpenSourceLibs_Activity);
      return false;
    });

    Preference signOut = findPreference("pref_key_sign_out");

    signOut.setOnPreferenceClickListener(preference -> {
      prefsEditor.putBoolean(Constants.EXTRAS_USER_LOGGED_IN, false);
      prefsEditor.apply();

      signOut();
      return false;
    });
  }

  private void signOut() {
    Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(status -> {
      EventBus_Singleton.getInstance().post(new EventBus_Poster("logged_out"));
      startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
      finish();
    });
  }

  @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

  }

  @Override public void onConnectionFailed(ConnectionResult connectionResult) {

  }

  @Override public void onConnected(Bundle bundle) {

  }

  @Override public void onConnectionSuspended(int i) {

  }
}
