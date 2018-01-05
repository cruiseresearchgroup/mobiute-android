package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.managers.BluetoothManager;
import com.ute.mobi.services.AppStateService;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class BluetoothBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    AppStateService appStateService = AppStateService.getInstance();
    String cachedSessionId = appStateService.getCachedSessionId();
    if(cachedSessionId == null) {
      return;
    }

    BluetoothManager.notifyBluetoothToUpdate();
  }
}
