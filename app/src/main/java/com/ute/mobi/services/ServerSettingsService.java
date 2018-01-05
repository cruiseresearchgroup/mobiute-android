package com.ute.mobi.services;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.jaredrummler.android.device.DeviceName;
import com.ute.mobi.R;


/**
 * Created by jonathanliono on 8/01/15.
 */
public class ServerSettingsService {
  private final Context context;

  public ServerSettingsService(Context context) {
    this.context = context.getApplicationContext();
  }

  public String getServerBaseUrl() {
    String appstateServerAddress = AppStateService.getInstance().getServerAddress();
    if (appstateServerAddress == null || appstateServerAddress.isEmpty())
      appstateServerAddress = this.context.getResources().getString(R.string.app_settings_base_server_url);
    return appstateServerAddress;
  }

  public String getDeviceId() {
    return Settings.Secure.getString(this.context.getContentResolver(),
            Settings.Secure.ANDROID_ID);
  }

  public String getDeviceModel() {
    return DeviceName.getDeviceName();
  }

  public int dpToPixels(int dpMeasure) {
    Resources r = this.context.getResources();
    return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpMeasure,
            r.getDisplayMetrics()
    );
  }

  public int dipToPixels(float dipValue) {
    DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
    return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics));
  }
}
