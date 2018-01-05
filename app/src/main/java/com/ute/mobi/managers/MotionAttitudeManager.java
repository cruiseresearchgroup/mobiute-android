package com.ute.mobi.managers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class MotionAttitudeManager {
    private static Context aContext=null;

    private static Sensor accelerometer;
    private static Sensor magneticField;
    private static SensorManager sensorManager;
    // you could use an OrientationListener array instead
    // if you plans to use more than one listener
    private static MotionAttitudeListener listener;

    /** indicates whether or not Orientation Sensor is supported */
    private static Boolean supported;
    /** indicates whether or not Orientation Sensor is running */
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
     * Returns true if at least one Orientation sensor is available
     */
    public static boolean isSupported(Context context) {
        aContext = context;
        if (supported == null) {
            if (aContext != null) {


                sensorManager = (SensorManager) aContext.
                        getSystemService(Context.SENSOR_SERVICE);

                // Get all sensors in device
                List<Sensor> accelerometers = sensorManager.getSensorList(
                        Sensor.TYPE_ACCELEROMETER);

                List<Sensor> magneticFields = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);

                supported = new Boolean(accelerometers.size() > 0 && magneticFields.size() > 0);
            } else {
                supported = Boolean.FALSE;
            }
        }
        return supported;
    }

    /**
     * Registers a listener and start listening
     * @param motionAttitudeListener
     *             callback for Motion Attitude events
     */
    public static void startListening( MotionAttitudeListener motionAttitudeListener, long milliseconds )
    {
        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);

        // Take all sensors in device
        List<Sensor> accelerometers = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        List<Sensor> magneticFields = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometers.size() > 0 && magneticFields.size() > 0) {

            accelerometer = accelerometers.get(0);
            magneticField = magneticFields.get(0);
            int samplingPeriod = (int) (milliseconds * 1000);
            // Register Gyroscope Listener
            running = sensorManager.registerListener(sensorEventListener, accelerometer, samplingPeriod) &&
                      sensorManager.registerListener(sensorEventListener, magneticField, samplingPeriod);

            listener = motionAttitudeListener;
        }
    }

    /**
     * The listener that listen to events from the Gyroscope listener
     */
    private static SensorEventListener sensorEventListener =
            new SensorEventListener() {

                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                float[] mGravity;
                float[] mGeomagnetic;
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                        mGravity = event.values;
                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                        mGeomagnetic = event.values;
                    if (mGravity != null && mGeomagnetic != null) {
                        float R[] = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                        if (success) {
                            float orientation[] = new float[3];
                            SensorManager.getOrientation(R, orientation);

                            // orientation contains: azimut, pitch and roll
                            listener.onMotionAttitudeChanged(orientation[0], orientation[1], orientation[2]);
                        }
                    }
                }
            };
}
