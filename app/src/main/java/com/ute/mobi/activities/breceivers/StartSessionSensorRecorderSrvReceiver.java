package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.activities.services.SessionSensorRecorderSrv;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.settings.SessionRoleSettings;

/**
 * Created by jonathanliono on 27/04/15.
 */
public class StartSessionSensorRecorderSrvReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    //AndroidLogger.e("Service Loop", "Initiating reading");

    if(AppStateService.getInstance().getCachedRole() == SessionRoleSettings.ROLE_SENSING) {
      Intent service = new Intent(context, SessionSensorRecorderSrv.class );
      service.putExtra(SessionSensorRecorderSrv.BUNDLE_KEY_ACTION, SessionSensorRecorderSrv.SERVICE_ACTION_READSENSOR);
      context.startService(service);
    }
  }
}
