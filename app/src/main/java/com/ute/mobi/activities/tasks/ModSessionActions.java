package com.ute.mobi.activities.tasks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.ute.mobi.activities.SessionActivity;
import com.ute.mobi.activities.breceivers.BootReceiver;
import com.ute.mobi.activities.services.SessionSensorRecorderSrv;
import com.ute.mobi.services.AppStateService;

/**
 * Created by jonathanliono on 30/12/2015.
 */
public final class ModSessionActions {
  public static void FinishSession(Context context, AppStateService appStateService) {
    Context appContext = context.getApplicationContext();

    appStateService.setCurrentSessionIsRunning(false);
    appStateService.setCurrentSessionIsStopping(true);

    // clear cached session id.
    appStateService.clearCachedSessionPreferences();

    Intent service = new Intent(appContext, SessionSensorRecorderSrv.class);
    appContext.stopService(service);

    // disable bootreceiver onboot
    ComponentName receiver = new ComponentName(appContext, BootReceiver.class);
    PackageManager pm = appContext.getPackageManager();

    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);

    SessionSensorRecorderSrv.isRecordingRunning = false;
  }

  public static void SendBroadcastToFinishSessionActivityAndNavigate(Context context) {
    context.sendBroadcast(new Intent(SessionActivity.BR_FINISH_ACTIVITY));
  }
}
