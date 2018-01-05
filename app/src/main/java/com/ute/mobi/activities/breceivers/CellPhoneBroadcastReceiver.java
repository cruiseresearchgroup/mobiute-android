package com.ute.mobi.activities.breceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ute.mobi.managers.CellPhoneManager;
import com.ute.mobi.managers.NoiseLevelManager;

/**
 * Created by jonathanliono on 31/12/2016.
 */

public class CellPhoneBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    CellPhoneManager.notifyCellPhoneInfosToUpdate();
  }
}
