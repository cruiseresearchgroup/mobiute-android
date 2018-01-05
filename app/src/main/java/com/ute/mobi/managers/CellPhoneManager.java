package com.ute.mobi.managers;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.ute.mobi.activities.breceivers.CellPhoneBroadcastReceiver;
import com.ute.mobi.models.UteModelCellInfo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanliono on 30/12/2016.
 */

public class CellPhoneManager {
  private static Context aContext=null;

  private static CellPhoneListener listener;

  private static PhoneStateListener phoneStateListener;

  private static TelephonyManager telephonyManager;

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
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    phoneStateListener = null;
    MIN_TIME_TO_UPDATE_MILLISEC = null;
  }

  /**
   * Returns true if at least one Accelerometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          supported = Boolean.TRUE;
          return true;
        } else {
          supported = Boolean.TRUE;
          return true;
        }
      } else {
        supported = Boolean.FALSE;
      }
    }
    return supported;
  }

  /**
   * Registers a listener and start listening
   * @param cellphoneListener
   *             callback for accelerometer events
   */
  public static void startListening(CellPhoneListener cellphoneListener, long milliseconds)
  {
    listener = cellphoneListener;
    telephonyManager = (TelephonyManager) aContext.getSystemService(Context.TELEPHONY_SERVICE);
    phoneStateListener = setupPhoneStateListener();
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    MIN_TIME_TO_UPDATE_MILLISEC = milliseconds;
    running = true;
    scheduleReadCellPhoneInfos(milliseconds);
  }

  public static void scheduleReadCellPhoneInfos(long milliseconds) {
    if(aContext != null) {
      Context appContext = aContext.getApplicationContext();
      AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(appContext, CellPhoneBroadcastReceiver.class);
      PendingIntent scheduleNoiseLevelIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

      scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + milliseconds, scheduleNoiseLevelIntent);
    }
  }

  public static void notifyCellPhoneInfosToUpdate() {
    if(isListening() && telephonyManager != null && MIN_TIME_TO_UPDATE_MILLISEC != null) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        getupdateForPhoneInfosFromJelliBean();
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
  private static void getupdateForPhoneInfosFromJelliBean () {
    List<CellInfo> cellInfos = (List<CellInfo>) telephonyManager.getAllCellInfo();
    if(cellInfos == null) {
      cellInfos = new ArrayList<CellInfo>();
    }

    ArrayList<UteModelCellInfo> cellinfoToUpdate = new ArrayList<UteModelCellInfo>();

    for(CellInfo ci: cellInfos) {
      CellInfoGsm cigsm = (CellInfoGsm) ci;
      UteModelCellInfo cellinfo = new UteModelCellInfo();
      cellinfo.cid = cigsm.getCellIdentity().getCid();
      cellinfo.lac = cigsm.getCellIdentity().getLac();
      cellinfo.ss = cigsm.getCellSignalStrength().getDbm();
      cellinfoToUpdate.add(cellinfo);
    }

    if(cellinfoToUpdate.size() == 0) {
      if(cachedSignalStrength != null) {
        UteModelCellInfo cellinfo = new UteModelCellInfo();
        cellinfo.cid = cachedCid;
        cellinfo.lac = cachedLac;
        cellinfo.ss = cachedSignalStrength;
        cellinfoToUpdate.add(cellinfo);

        cachedSignalStrength = null;
      }
    }

    listener.onCellInfoChanged(cellinfoToUpdate);
    scheduleReadCellPhoneInfos(MIN_TIME_TO_UPDATE_MILLISEC);
  }

  private static Integer cachedCid;
  private static Integer cachedLac;
  private static Integer cachedSignalStrength;

  private static PhoneStateListener setupPhoneStateListener()
  {
    return new PhoneStateListener() {

      /** Callback invoked when device cell location changes. */
      @SuppressLint("NewApi")
      public void onCellLocationChanged(CellLocation location)
      {
        GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
        cachedCid = gsmCellLocation.getCid();
        cachedLac = gsmCellLocation.getLac();
      }

      /** invoked when data connection state changes (only way to get the network type) */
      public void onDataConnectionStateChanged(int state, int networkType)
      {
        //Log.d("cellp", "registered: "+networkType);
      }

      /** Callback invoked when network signal strengths changes. */
      public void onSignalStrengthsChanged(SignalStrength signalStrength)
      {
        //Log.d("cellp", "registered: "+signalStrength.getGsmSignalStrength());
        int signalStrengthGsm = signalStrength.getGsmSignalStrength();
        cachedSignalStrength = (2 * signalStrengthGsm) - 113; // -> dBm

        if(cachedSignalStrength == 85) {
          Log.d("Ori", "original signal strength: " + signalStrength);
        }
      }

    };
  }
}
