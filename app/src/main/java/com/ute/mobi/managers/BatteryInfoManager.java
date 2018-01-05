package com.ute.mobi.managers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.ute.mobi.activities.breceivers.BatteryInfoBroadcastReceiver;

/**
 * Created by jonathanliono on 8/01/2017.
 */

public class BatteryInfoManager {

  private static Context aContext=null;

  private static BatteryInfoListener listener = null;

  private static Long MIN_TIME_TO_UPDATE_MILLISEC = null;

  /** indicates whether or not Microphone Sensor is supported */
  private static Boolean supported;
  /** indicates whether or not Microphone Sensor is running */
  private static boolean running = false;

  /**
   * Returns true if the manager is listening to orientation changes
   */
  public static boolean isListening() {
    return running;
  }

  /**
   * Unregisters listeners
   */
  public static void stopListening() {
    running = false;

    MIN_TIME_TO_UPDATE_MILLISEC = null;
  }

  /**
   * Returns true if at least one Accelerometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {
        supported = Boolean.TRUE;
      } else {
        supported = Boolean.FALSE;
      }
    }
    return supported;
  }

  /**
   * Registers a listener and start listening
   * @param batteryListener
   *             callback for battery events
   */
  public static void startListening(BatteryInfoListener batteryListener, long milliseconds)
  {
    listener = batteryListener;
    MIN_TIME_TO_UPDATE_MILLISEC = milliseconds;
    running = true;
    //scheduleReadCellPhoneInfos(milliseconds);
  }

  public static void scheduleReadBatteryInfos(long milliseconds) {
    if(aContext != null) {
      Context appContext = aContext.getApplicationContext();
      AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(appContext, BatteryInfoBroadcastReceiver.class);
      PendingIntent scheduleBatteryInfoIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

      scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + milliseconds, scheduleBatteryInfoIntent);
    }
  }

  public static void notifyBatteryInfosToUpdate() {
    if(isListening() && listener != null && MIN_TIME_TO_UPDATE_MILLISEC != null) {
      IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Intent batteryStatus = aContext.registerReceiver(null, ifilter);

      int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
              status == BatteryManager.BATTERY_STATUS_FULL;

      int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
      float batteryLevel = level / (float)scale;

      listener.onBatteryInfoChanged(batteryLevel, isCharging);
      scheduleReadBatteryInfos(MIN_TIME_TO_UPDATE_MILLISEC);
    }
  }

}
