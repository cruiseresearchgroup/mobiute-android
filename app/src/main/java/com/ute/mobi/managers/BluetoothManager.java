package com.ute.mobi.managers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ute.mobi.activities.breceivers.BluetoothBroadcastReceiver;
import com.ute.mobi.models.UteModelBluetoothInfo;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by jonathanliono on 20/11/2016.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothManager {

  private static Context aContext=null;

  private static BluetoothListener listener;

  private static BluetoothAdapter BTAdapter;

  private static List<UteModelBluetoothInfo> bluetoothDevices = new ArrayList<UteModelBluetoothInfo>();

  private static Long MIN_TIME_TO_UPDATE_MILLISEC = null;

  /** indicates whether or not Barometer Sensor is supported */
  private static Boolean supported;
  /** indicates whether or not Barometer Sensor is running */
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
    BTAdapter.cancelDiscovery();
    stopBleScan();
    BTAdapter = null;
    bluetoothDevices = new ArrayList<>();
  }

  public static int REQUEST_BLUETOOTH = 1;

  /**
   * Returns true if at least one Barometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if(BTAdapter == null)
          return false;

        /*if (!BTAdapter.isEnabled()) {
          Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          activity.startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }*/

        return true;
      }
    }

    return false;
  }

  /**
   * Registers a listener and start listening
   * @param bluetoothListener
   *             callback for bluetooth events
   */
  public static void startListening(BluetoothListener bluetoothListener, long milliseconds) {
    if(BTAdapter != null) {
      listener = bluetoothListener;
      MIN_TIME_TO_UPDATE_MILLISEC = milliseconds;
      running = true;
      BTAdapter.startDiscovery();
      startBleScan();
      scheduleReadBluetooth(milliseconds);
    }
  }

  static BluetoothAdapter.LeScanCallback mLeScanCallback = new
          BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi,
                                 final byte[] scanRecord) {
              BluetoothDevice btDevice = device;
              System.out.println("discovered a device JELLYBEAN");
              onDiscoveredDevices(btDevice, rssi);
            }
          };

  static BluetoothLeScanner scanner;
  static ScanCallback mScanCallback = new ScanCallback() {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      BluetoothDevice btDevice = result.getDevice();
      System.out.println("discovered a device");
      onDiscoveredDevices(btDevice, result.getRssi());
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      System.out.println("scan batch");
      for (ScanResult sr : results) {

      }
    }

    @Override
    public void onScanFailed(int errorCode) {
    }
  };

  @TargetApi(JELLY_BEAN_MR2)
  public static void startBleScan() {
    if(Build.VERSION.SDK_INT < JELLY_BEAN_MR2)
      return;


    if (Build.VERSION.SDK_INT < 21) {
      BTAdapter.startLeScan(mLeScanCallback);
    } else {
      ScanSettings settings = new ScanSettings.Builder()
              .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
              .build();
      ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
      scanner = BTAdapter.getBluetoothLeScanner();
      scanner.startScan(filters, settings, mScanCallback);
    }
  }

  @TargetApi(JELLY_BEAN_MR2)
  public static void stopBleScan() {
    if(Build.VERSION.SDK_INT < JELLY_BEAN_MR2)
      return;

    if (Build.VERSION.SDK_INT < 21) {
      BTAdapter.stopLeScan(mLeScanCallback);
    } else {

      ScanSettings settings = new ScanSettings.Builder()
              .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
              .build();
      ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
      scanner = BTAdapter.getBluetoothLeScanner();
      scanner.stopScan(mScanCallback);
    }
  }


  private static void scheduleReadBluetooth(long milliseconds) {
    if (aContext != null) {
      Context appContext = aContext.getApplicationContext();
      AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(appContext, BluetoothBroadcastReceiver.class);
      PendingIntent scheduleNoiseLevelIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

      scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + milliseconds, scheduleNoiseLevelIntent);
    }
  }

  public static void notifyBluetoothToUpdate() {
    if(isListening() && BTAdapter != null && MIN_TIME_TO_UPDATE_MILLISEC != null) {
      BTAdapter.cancelDiscovery();
      stopBleScan();
      listener.onBluetoothChanged(bluetoothDevices);
      bluetoothDevices = new ArrayList<>();
      BTAdapter.startDiscovery();
      startBleScan();
      scheduleReadBluetooth(MIN_TIME_TO_UPDATE_MILLISEC);
    }
  }

  public static void onDiscoveredDevices(BluetoothDevice device, int rssi) {
    if(isListening() && BTAdapter != null) {
      UteModelBluetoothInfo bluetoothInfo = new UteModelBluetoothInfo();
      bluetoothInfo.uuid = device.getAddress();
      bluetoothInfo.name = device.getName();
      bluetoothInfo.rssi = new Double(rssi);

      bluetoothDevices.add(bluetoothInfo);
    }
  }
}
