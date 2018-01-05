package com.ute.mobi;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import com.ute.mobi.services.AppStateService;

/**
 * Created by jonathanliono on 27/04/15.
 */
public class AppBoxManager extends MultiDexApplication {
  @Override
  public void onCreate() {
    super.onCreate();
    AppStateService.init(this);
  }
}
