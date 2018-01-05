package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.managers.BatteryInfoManager;

/**
 * Created by jonathanliono on 8/01/2017.
 */

public class BatteryInfoBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    BatteryInfoManager.notifyBatteryInfosToUpdate();
  }
}
