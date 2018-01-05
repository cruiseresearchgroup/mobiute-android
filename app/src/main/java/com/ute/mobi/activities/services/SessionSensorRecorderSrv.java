package com.ute.mobi.activities.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.ute.mobi.activities.SessionActivity;
import com.ute.mobi.activities.breceivers.BootReceiver;
import com.ute.mobi.activities.breceivers.StartSessionSensorRecorderSrvReceiver;
import com.ute.mobi.activities.tasks.ModSessionActions;
import com.ute.mobi.models.UteModelBluetoothInfo;
import com.ute.mobi.models.UteModelCellInfo;
import com.ute.mobi.models.UteModelSensorInfo;
import com.ute.mobi.models.UteModelWifiInfo;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.SessionSensorService;
import com.ute.mobi.services.UteCurrentSessionDBService;
import com.ute.mobi.services.UteSessionDBService;
import com.ute.mobi.settings.SessionSetupSettings;
import com.ute.mobi.utilities.AndroidLogger;

import java.util.List;

/**
 * Created by jonathanliono on 27/04/15.
 */
public class SessionSensorRecorderSrv extends Service {
  public static boolean isRecordingRunning;
  private final IBinder mBinder = new SessionSensorRecorderSrvBinder();
  private UteSessionDBService dbService;
  private SessionSensorService sensorService;

  public final static String BUNDLE_KEY_ACTION = "Action";
  public final static int SERVICE_ACTION_READSENSOR = 1;
  public final static int SERVICE_ACTION_DESTROY = 2;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public class SessionSensorRecorderSrvBinder extends Binder
  {
    SessionSensorRecorderSrv getService() {
      return SessionSensorRecorderSrv.this;
    }
  }

  @Override
  public void onCreate() {
    Log.d("test", "OnCreate service");
    super.onCreate();
    this.initVariables();
  }

  // This is the old onStart method that will be called on the pre-2.0
  // platform.  On 2.0 or later we override onStartCommand() so this
  // method will not be called.
  @Override
  public void onStart(Intent intent, int startId)
  {
    this.handleCommand(intent);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    this.handleCommand(intent);
    return Service.START_REDELIVER_INTENT;
  }

  private void handleCommand(Intent intent) {
    initVariables();

    Bundle extras = intent.getExtras();
    if (extras != null) {
      int action = extras.getInt(BUNDLE_KEY_ACTION, 0);
      switch(action) {
        case SERVICE_ACTION_READSENSOR:
          AppStateService appstate = AppStateService.getInstance();
          if(appstate.isCurrentSessionRunning()) {
            boolean isInvalidToInsertData = !appstate.isCurrentSessionRunning() || appstate.isCurrentSessionStopping();
            if (isInvalidToInsertData == false) {
              //Log.d("test", "reading and saving sensor info to db");
              new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                  try{
                    UteModelSensorInfo reading = sensorService.readSensors();
                    if(dbService != null) {

                      dbService.insertSensorInfo(reading);

                      List<UteModelBluetoothInfo> bluetoothInfos = sensorService.readBluetooths();
                      if(bluetoothInfos != null && bluetoothInfos.size() > 0) {
                        for(UteModelBluetoothInfo bluetoothInfo: bluetoothInfos) {
                          dbService.insertBluetoothInfo(bluetoothInfo);
                        }
                      }

                      List<UteModelWifiInfo> wifiInfos = sensorService.readWifis();
                      if(wifiInfos != null && wifiInfos.size() > 0) {
                        for(UteModelWifiInfo wifiInfo: wifiInfos) {
                          dbService.insertWifiInfo(wifiInfo);
                        }
                      }

                      List<UteModelCellInfo> cellInfos = sensorService.readcellInfos();
                      if(cellInfos != null && cellInfos.size() > 0) {
                        for(UteModelCellInfo cellInfo: cellInfos) {
                          dbService.insertCellInfo(cellInfo);
                        }
                      }
                    }
                  } catch(Exception ex) {

                  }

                  return null;
                }
              }.execute();
            }
          }

          String cachedSessionId = appstate.getCachedSessionId();
          if(cachedSessionId == null) {

          } else{
            boolean scheduleNextReading = true;
            SessionSetupSettings setupSettings = appstate.getSessionSetupSettings();
            if(setupSettings != null) {
              if(setupSettings.maximumRecordingDuration > 0) {
                double deltaTimeStamp = appstate.getCurrentTimeStamp() - appstate.getSessionDeviceStartTime();
                if(deltaTimeStamp > setupSettings.maximumRecordingDuration) {
                  scheduleNextReading = false;
                  if(appstate.isOnSessionActivity) {
                    ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(this);
                  }

                  ModSessionActions.FinishSession(this, appstate);
                  Toast.makeText(getApplicationContext(), "Session is finishing due to maximum duration for recording...", Toast.LENGTH_LONG).show();
                  break;
                }
              }
            }

            if(scheduleNextReading) {
              SessionSensorRecorderSrv.isRecordingRunning = true;

              // schedule next reading
              SessionActivity.scheduleSensorReadingBroadcastReceiver(this.getApplicationContext());
            } else {
              SessionSensorRecorderSrv.isRecordingRunning = false;
            }
          }
          break;
        default: break;
      }
    }
  }

  private void initVariables() {
    if(this.dbService == null) {
      this.dbService = UteCurrentSessionDBService.getSessionInstance(this);
      AndroidLogger.d("test", "init sensor service dbservice");
    }else{
      return;
    }

    if(this.sensorService == null) {
      this.sensorService = new SessionSensorService(this, AppStateService.getInstance());
      AndroidLogger.d("test", "init sensor service for reading");
    }
    else{
      return;
    }
  }

  @Override
  public void onDestroy() {
    AndroidLogger.e("test", "DESTROY triggered");
    super.onDestroy();
    this.stopService();
    SessionSensorRecorderSrv.isRecordingRunning = false;
  }

  public void stopService() {
    UteCurrentSessionDBService.closeAndClearSessionInstance();
    this.dbService = null;

    if(this.sensorService != null) {
      this.sensorService.stopSensors();
      this.sensorService = null;
    }
    else
    {
      Log.d("test", "sensor service not set");
    }
  }

  public static synchronized void destroySensorReadingService(Context context) {
    stopSensorReadingService(context);

    // disable bootreceiver onboot
    Context appContext = context.getApplicationContext();
    ComponentName receiver = new ComponentName(appContext, BootReceiver.class);
    PackageManager pm = appContext.getPackageManager();

    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
  }

  public static void stopSensorReadingService(Context context) {

    ComponentName receiver = new ComponentName(context, StartSessionSensorRecorderSrvReceiver.class);
    PackageManager pm = context.getPackageManager();

    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);

    //((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).cancel(SessionActivity.scheduleSensorReadingService(context));

    Intent service = new Intent(context.getApplicationContext(), SessionSensorRecorderSrv.class);
    context.getApplicationContext().stopService(service);
  }
}
