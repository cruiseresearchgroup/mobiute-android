package com.ute.mobi.managers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import java.util.List;

/**
 * Created by jonathanliono on 11/01/15.
 */
public class MagnetometerManager {

    private static Context aContext=null;

    private static Sensor sensor;
    private static SensorManager sensorManager;
    // you could use an OrientationListener array instead
    // if you plans to use more than one listener
    private static MagnetometerListener listener;

    /** indicates whether or not Accelerometer Sensor is supported */
    private static Boolean supported;
    /** indicates whether or not Accelerometer Sensor is running */
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
     * Returns true if at least one Accelerometer sensor is available
     */
    public static boolean isSupported(Context context) {
        aContext = context;

        // This only exist in API LEVEL 18 JELLY BEAN v2
        if (Build.VERSION.SDK_INT < 18) {
            supported = Boolean.FALSE;
        }

        if (supported == null) {
              if (aContext != null) {


                sensorManager = (SensorManager) aContext.
                        getSystemService(Context.SENSOR_SERVICE);

                // Get all sensors in device
                List<Sensor> sensors = sensorManager.getSensorList(
                        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

                supported = new Boolean(sensors.size() > 0);



            } else {
                supported = Boolean.FALSE;
            }
        }
        return supported;
    }

    /**
     * Registers a listener and start listening
     * @param magnetometerListener
     *             callback for accelerometer events
     */
    public static void startListening( MagnetometerListener magnetometerListener, long milliseconds )
    {
        // This only exist in API LEVEL 18 JELLY BEAN v2
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);

        // Take all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        if (sensors.size() > 0) {

            sensor = sensors.get(0);
            int samplingPeriod = (int) (milliseconds * 1000);
            // Register Accelerometer Listener
            running = sensorManager.registerListener(
                    sensorEventListener, sensor,
                    samplingPeriod);

            listener = magnetometerListener;
        }
    }

    /**
     * The listener that listen to events from the accelerometer listener
     */
    private static SensorEventListener sensorEventListener =
            new SensorEventListener() {

                private long now = 0;

                private float x = 0;
                private float y = 0;
                private float z = 0;

                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                public void onSensorChanged(SensorEvent event) {
                    now = event.timestamp;

                    x = event.values[0];
                    y = event.values[1];
                    z = event.values[2];

                    // trigger change event
                    listener.onMagnetometerChanged(x, y, z);
                }

            };
}
