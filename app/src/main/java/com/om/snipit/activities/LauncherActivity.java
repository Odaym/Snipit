package com.om.snipit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.SnipitApplication;
import javax.inject.Inject;

public class LauncherActivity extends Activity {

  @Inject SharedPreferences prefs;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((SnipitApplication) getApplication()).getAppComponent().inject(this);

    if (prefs.getBoolean(Constants.EXTRAS_USER_LOGGED_IN, false)) {
      startActivity(new Intent(LauncherActivity.this, BooksActivity.class));
    } else {
      startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
    }
  }

  @Override protected void onResume() {
    super.onResume();
    finish();
  }
}
