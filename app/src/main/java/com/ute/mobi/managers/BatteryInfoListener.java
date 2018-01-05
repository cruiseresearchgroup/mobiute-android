package com.ute.mobi.managers;

/**
 * Created by jonathanliono on 8/01/2017.
 */

public interface BatteryInfoListener {
  void onBatteryInfoChanged(float batteryLevel, boolean isCharging);
}
