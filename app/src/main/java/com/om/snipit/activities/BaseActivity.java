package com.om.snipit.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.R;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.SnipitApplication;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import javax.inject.Inject;

public class BaseActivity extends AppCompatActivity {
  @Inject SharedPreferences prefs;
  @Inject SharedPreferences.Editor prefsEditor;
  @Inject DatabaseHelper dbHelper;
  @Inject RuntimeExceptionDao<Snippet, Integer> snippetDAO;
  @Inject RuntimeExceptionDao<Book, Integer> bookDAO;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((SnipitApplication) getApplication()).getAppComponent().inject(this);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        super.onBackPressed();
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      super.onBackPressed();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void setupToolbar(Toolbar toolbar, String toolbarTitle, boolean backEnabled,
      int bookColorCode) {
    if (toolbarTitle == null) {
      toolbar.setTitle(R.string.app_name);
    } else {
      toolbar.setTitle(toolbarTitle);
    }

    setupToolbarColors(bookColorCode, toolbar);

    setSupportActionBar(toolbar);

    if (backEnabled) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
  }

  public void setupToolbarColors(int bookColorCode, Toolbar toolbar) {
    switch (bookColorCode) {
      case Constants.DEFAULT_ACTIVITY_TOOLBAR_COLORS:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_green));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.green));
        }
        break;
      case Constants.OPEN_SOURCE_LIBS_ACTIVITY_TOOLBAR_COLORS:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_purple));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.purple));
        }
        break;
      case 0:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_pink));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.pink));
        }
        break;
      case 1:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_red));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.red));
        }
        break;
      case 2:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_purple));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.purple));
        }
        break;
      case 3:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_yellow));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.yellow));
        }
        break;
      case 4:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_blue));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.blue));
        }
        break;
      case 5:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(getResources().getColor(R.color.darker_brown));
        }
        if (toolbar != null) {
          toolbar.setBackgroundColor(getResources().getColor(R.color.brown));
        }
        break;
    }
  }
}
