package com.om.snipit.classes;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import com.om.snipit.BuildConfig;
import com.om.snipit.dagger.AppComponent;
import com.om.snipit.dagger.AppModule;
import com.om.snipit.dagger.DaggerAppComponent;
import io.fabric.sdk.android.Fabric;

public class SnipitApplication extends Application {
  private AppComponent component;

  @Override public void onCreate() {
    super.onCreate();

    initDagger();

    if (!BuildConfig.DEBUG) {
      //Initialize Crashlytics
      Fabric.with(this, new Crashlytics());
    }
  }

  public AppComponent getAppComponent() {
    return component;
  }

  private void initDagger() {
    component = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

    component.inject(this);
  }
}
