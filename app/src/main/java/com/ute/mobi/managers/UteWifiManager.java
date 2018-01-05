package com.ute.mobi.managers;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.ute.mobi.activities.breceivers.WifiBroadcastReceiver;
import com.ute.mobi.models.UteModelWifiInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class UteWifiManager {
  private static Context aContext = null;

  private static UteWifiListener listener;

  private static WifiManager wifiManager;

  private static List<UteModelWifiInfo> scannedWifis = new ArrayList<UteModelWifiInfo>();

  private static Long MIN_TIME_TO_UPDATE_MILLISEC = null;

  /**
   * indicates whether or not Barometer Sensor is supported
   */
  private static Boolean supported;
  /**
   * indicates whether or not Barometer Sensor is running
   */
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
    wifiManager = null;
    scannedWifis = new ArrayList<UteModelWifiInfo>();
  }

  /**
   * Returns true if at least one Barometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
          return false;
        }

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null)
          return false;

        if (wifiManager.isWifiEnabled() == false) {
          wifiManager.setWifiEnabled(true);
        }

        return true;
      }
    }

    return false;
  }

  /**
   * Registers a listener and start listening
   *
   * @param wifiListener callback for bluetooth events
   */
  public static void startListening(UteWifiListener wifiListener, long milliseconds) {
    if (wifiManager != null) {
      listener = wifiListener;
      MIN_TIME_TO_UPDATE_MILLISEC = milliseconds;
      running = true;
      wifiManager.startScan();
      scheduleReadWifi(milliseconds);
    }
  }

  private static void scheduleReadWifi(long milliseconds) {
    if (aContext != null) {
      Context appContext = aContext.getApplicationContext();
      AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(appContext, WifiBroadcastReceiver.class);
      PendingIntent scheduleNoiseLevelIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

      scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + milliseconds, scheduleNoiseLevelIntent);
    }
  }

  public static void onScannedWifi() {
    if(isListening() && wifiManager != null) {
      List<ScanResult> scannedResults = wifiManager.getScanResults();
      if(scannedResults != null) {
        for(ScanResult result: scannedResults) {
          UteModelWifiInfo info = new UteModelWifiInfo();
          info.ssid = result.SSID;
          info.bssid = result.BSSID;
          info.rssi = new Double(result.level);
          info.capabilities = result.capabilities;
          info.freq20 = result.frequency;
          setAdditionalWifiInfoForApi23(result, info);
          scannedWifis.add(info);
        }
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private static void setAdditionalWifiInfoForApi23(ScanResult result, UteModelWifiInfo info) {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
      return;

    try{
      switch(result.channelWidth) {
        case ScanResult.CHANNEL_WIDTH_20MHZ:
          info.channel_width = 20; break;
        case ScanResult.CHANNEL_WIDTH_40MHZ:
          info.channel_width = 40;
          info.freq_center = result.centerFreq0; break;
        case ScanResult.CHANNEL_WIDTH_80MHZ:
          info.channel_width = 80;
          info.freq_center = result.centerFreq0;
          break;
        case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
          info.channel_width = 80;
          info.dual_channel = true;
          info.freq_center = result.centerFreq0;
          info.freq_center_2 = result.centerFreq1;
          break;
        case ScanResult.CHANNEL_WIDTH_160MHZ:
          info.channel_width = 160;
          info.freq_center = result.centerFreq0;
          break;
        default: break;
      }
    } catch(Exception ex) {

    }

    info.venue_name = result.venueName.toString();
  }

  public static void notifyWifiToUpdate() {
    if(isListening() && wifiManager != null && MIN_TIME_TO_UPDATE_MILLISEC != null) {
      listener.onWifiChange(scannedWifis);
      scannedWifis = new ArrayList<UteModelWifiInfo>();
      wifiManager.startScan();
      scheduleReadWifi(MIN_TIME_TO_UPDATE_MILLISEC);
    }
  }
}
