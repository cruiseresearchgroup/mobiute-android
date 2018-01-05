package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ute.mobi.activities.SessionActivity;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.utilities.AndroidLogger;


/**
 * Created by jonathanliono on 14/05/15.
 */
public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
      // Set the alarm here.
      // check if there is any session recorded previously.
      Log.d("test", "======TRIGGERED AFTER BOOT");
      AppStateService appStateService = AppStateService.getInstance();
      String cachedSessionId = appStateService.getCachedSessionId();
      if(cachedSessionId != null) {
        // start session view.
        AndroidLogger.e("test", "=======SCHEDULING AFTER BOOT");
        //AppStateService.getInstance().setScheduledSensorReadingPendingIntent(SessionActivity.scheduleSensorReadingService(context));;
        SessionActivity.scheduleSensorReadingService(context);
      }
    }
  }
}
