package com.om.snipit.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.om.snipit.classes.Constants;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.classes.SnipitApplication;
import com.om.snipit.models.Book;
import com.om.snipit.models.Snippet;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module public class AppModule {
  private final SnipitApplication application;

  public AppModule(SnipitApplication app) {
    this.application = app;
  }

  @Provides @Singleton Context providesApplicationContext() {
    return application;
  }

  @Provides @Singleton Resources provideActivityResources(Context app) {
    return app.getResources();
  }

  @Provides DatabaseHelper providesDBHelper(Context app) {
    return OpenHelperManager.getHelper(app, DatabaseHelper.class);
  }

  @Provides RuntimeExceptionDao<Book, Integer> providesBookDAO(DatabaseHelper dbHelper) {
    return dbHelper.getBookDAO();
  }

  @Provides RuntimeExceptionDao<Snippet, Integer> providesSnippetDAO(DatabaseHelper dbHelper) {
    return dbHelper.getSnippetDAO();
  }

  @Provides @Singleton SharedPreferences providesSharedPreferences(Context app) {
    return app.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
  }

  @Provides @Singleton SharedPreferences.Editor providesSharedPreferencesEditor(
      SharedPreferences prefs) {
    return prefs.edit();
  }
}
