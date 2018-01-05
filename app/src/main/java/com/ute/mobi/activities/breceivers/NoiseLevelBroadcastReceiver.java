package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.activities.services.SessionSensorRecorderSrv;
import com.ute.mobi.managers.NoiseLevelManager;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.settings.SessionRoleSettings;

/**
 * Created by jonathanliono on 8/11/2016.
 */

public class NoiseLevelBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    NoiseLevelManager.notifyNoiseLevelToUpdate();
  }
}
