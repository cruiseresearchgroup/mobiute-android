package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.ute.mobi.managers.UteWifiManager;
import com.ute.mobi.services.AppStateService;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class WifiScannedBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    AppStateService appStateService = AppStateService.getInstance();
    String cachedSessionId = appStateService.getCachedSessionId();
    if(cachedSessionId == null) {
      return;
    }

    String action = intent.getAction();
    if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
      UteWifiManager.onScannedWifi();
    }
  }
}
