package com.ute.mobi.managers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/**
 * Created by jonathanliono on 9/11/2016.
 */

public class BarometerManager {
  private static Context aContext=null;

  private static Sensor sensor;
  private static SensorManager sensorManager;
  // you could use an OrientationListener array instead
  // if you plans to use more than one listener
  private static BarometerListener listener;

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
    try {
      if (sensorManager != null && sensorEventListener != null) {
        sensorManager.unregisterListener(sensorEventListener);
      }
    } catch (Exception e) {}
  }

  /**
   * Returns true if at least one Barometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {


        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);

        // Get all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_PRESSURE);

        supported = new Boolean(sensors.size() > 0);



      } else {
        supported = Boolean.FALSE;
      }
    }
    return supported;
  }

  /**
   * Registers a listener and start listening
   * @param barometerListener
   *             callback for accelerometer events
   */
  public static void startListening( BarometerListener barometerListener, long milliseconds )
  {
    sensorManager = (SensorManager) aContext.
            getSystemService(Context.SENSOR_SERVICE);

    // Take all sensors in device
    List<Sensor> sensors = sensorManager.getSensorList(
            Sensor.TYPE_PRESSURE);

    if (sensors.size() > 0) {

      sensor = sensors.get(0);
      int samplingPeriod = (int) (milliseconds * 1000);
      // Register Barometer Listener
      running = sensorManager.registerListener(
              sensorEventListener, sensor,
              samplingPeriod);

      listener = barometerListener;
    }
  }

  /**
   * The listener that listen to events from the accelerometer listener
   */
  private static SensorEventListener sensorEventListener =
          new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
              float pressure = event.values[0];
              float altitude = SensorManager.getAltitude(
                      SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
              listener.onBarometerSensorChanged(pressure, altitude);
            }
          };
}
