package com.ute.mobi.activities.breceivers;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.managers.BluetoothManager;
import com.ute.mobi.services.AppStateService;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class BluetoothDiscoveryBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    AppStateService appStateService = AppStateService.getInstance();
    String cachedSessionId = appStateService.getCachedSessionId();
    if(cachedSessionId == null) {
      return;
    }

    String action = intent.getAction();
    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
      BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      // Get the "RSSI" to get the signal strength as integer,
      // but should be displayed in "dBm" units
      int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
      BluetoothManager.onDiscoveredDevices(device, rssi);
    }
  }
}
