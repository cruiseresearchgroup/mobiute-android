package com.ute.mobi.managers;

/**
 * Created by jonathanliono on 9/01/15.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

public class UserAccelerationManager {

    private static Context aContext=null;


    /** Accuracy configuration */
    private static float threshold  = 15.0f;
    private static int interval     = 200;

    private static Sensor sensor;
    private static SensorManager sensorManager;
    // you could use an OrientationListener array instead
    // if you plans to use more than one listener
    private static UserAccelerationListener listener;

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
        if (supported == null) {
            if (aContext != null) {


                sensorManager = (SensorManager) aContext.
                        getSystemService(Context.SENSOR_SERVICE);

                // Get all sensors in device
                List<Sensor> sensors = sensorManager.getSensorList(
                        Sensor.TYPE_LINEAR_ACCELERATION);

                supported = new Boolean(sensors.size() > 0);



            } else {
                supported = Boolean.FALSE;
            }
        }
        return supported;
    }

    /**
     * Configure the listener for shaking
     * @param threshold
     *             minimum acceleration variation for considering shaking
     * @param interval
     *             minimum interval between to shake events
     */
    public static void configure(int threshold, int interval) {
        UserAccelerationManager.threshold = threshold;
        UserAccelerationManager.interval = interval;
    }

    /**
     * Registers a listener and start listening
     * @param userAccelerationListener
     *             callback for accelerometer events
     */
    public static void startListening( UserAccelerationListener userAccelerationListener, long milliseconds )
    {
        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);

        // Take all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_LINEAR_ACCELERATION);

        if (sensors.size() > 0) {

            sensor = sensors.get(0);
            int samplingPeriod = (int) (milliseconds * 1000);
            // Register Accelerometer Listener
            running = sensorManager.registerListener(
                    sensorEventListener, sensor,
                    samplingPeriod);

            listener = userAccelerationListener;
        }
    }

    /**
     * Configures threshold and interval
     * And registers a listener and start listening
     * @param userAccelerationListener
     *             callback for accelerometer events
     * @param threshold
     *             minimum acceleration variation for considering shaking
     * @param interval
     *             minimum interval between to shake events
     */
    public static void startListening(
            UserAccelerationListener userAccelerationListener,
            int threshold, int interval, int milliseconds ) {
        configure(threshold, interval);
        startListening(userAccelerationListener, milliseconds);
    }

    /**
     * The listener that listen to events from the accelerometer listener
     */
    private static SensorEventListener sensorEventListener =
            new SensorEventListener() {

                private long now = 0;
                private long timeDiff = 0;
                private long lastUpdate = 0;
                private long lastShake = 0;

                private float x = 0;
                private float y = 0;
                private float z = 0;
                private float lastX = 0;
                private float lastY = 0;
                private float lastZ = 0;
                private float force = 0;

                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                public void onSensorChanged(SensorEvent event) {
                    // use the event timestamp as reference
                    // so the manager precision won't depends
                    // on the UserAccelerationListener implementation
                    // processing time
                    now = event.timestamp;

                    x = event.values[0];
                    y = event.values[1];
                    z = event.values[2];

                    // if not interesting in shake events
                    // just remove the whole if then else block
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        lastShake = now;
                        lastX = x;
                        lastY = y;
                        lastZ = z;
                    } else {
                        timeDiff = now - lastUpdate;

                        if (timeDiff > 0) {

                    /*force = Math.abs(x + y + z - lastX - lastY - lastZ)
                                / timeDiff;*/
                            force = Math.abs(x + y + z - lastX - lastY - lastZ);

                            if (Float.compare(force, threshold) >0 ) {
                                //Toast.makeText(Accelerometer.getContext(),
                                //(now-lastShake)+"  >= "+interval, 1000).show();

                                if (now - lastShake >= interval) {

                                    // trigger shake event
                                    //listener.onShake(force);
                                }

                                lastShake = now;
                            }
                            lastX = x;
                            lastY = y;
                            lastZ = z;
                            lastUpdate = now;
                        }
                    }

                    /*final float alpha = 0.8;

                    // Isolate the force of gravity with the low-pass filter.
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    // Remove the gravity contribution with the high-pass filter.
                    linear_acceleration[0] = event.values[0] - gravity[0];
                    linear_acceleration[1] = event.values[1] - gravity[1];
                    linear_acceleration[2] = event.values[2] - gravity[2];*/

                    // trigger change event
                    listener.onUserAccelerationChanged(x, y, z);
                }

            };

}
