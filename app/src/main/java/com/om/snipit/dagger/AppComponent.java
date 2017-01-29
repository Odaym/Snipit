package com.om.snipit.dagger;

import com.om.snipit.activities.BaseActivity;
import com.om.snipit.activities.LauncherActivity;
import com.om.snipit.activities.SettingsActivity;
import com.om.snipit.activities.ViewSnippetActivity;
import com.om.snipit.classes.SnipitApplication;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = { AppModule.class }) public interface AppComponent {

  void inject(SnipitApplication application);

  void inject(LauncherActivity launcherActivity);

  void inject(BaseActivity base_activity);

  void inject(SettingsActivity settingsActivity);

  void inject(ViewSnippetActivity.View_Snippet_Fragment viewSnippetFragment);
}
