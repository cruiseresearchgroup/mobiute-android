package com.ute.mobi.managers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class GyroscopeManager {
    private static Context aContext=null;

    private static Sensor sensor;
    private static SensorManager sensorManager;
    // you could use an OrientationListener array instead
    // if you plans to use more than one listener
    private static GyroscopeListener listener;

    /** indicates whether or not Gyroscope Sensor is supported */
    private static Boolean supported;
    /** indicates whether or not Gyroscope Sensor is running */
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
     * Returns true if at least one Gyroscope sensor is available
     */
    public static boolean isSupported(Context context) {
        aContext = context;
        if (supported == null) {
            if (aContext != null) {


                sensorManager = (SensorManager) aContext.
                        getSystemService(Context.SENSOR_SERVICE);
                // Get all sensors in device
                List<Sensor> sensors = sensorManager.getSensorList(
                        Sensor.TYPE_GYROSCOPE_UNCALIBRATED);

                boolean isValid = sensors.size() > 0;
                if(!isValid) {
                    java.lang.reflect.Method method = null;
                    try {
                        Class noparams[] = {};
                        method = sensorManager.getClass().getDeclaredMethod("getFullSensorList", noparams);
                        try {
                            method.setAccessible(true);
                            ArrayList<Sensor> arrayOfSensors = (ArrayList<Sensor>)method.invoke(sensorManager);
                            method.setAccessible(false);
                            for(int i = 0; i < arrayOfSensors.size(); i++) {
                                if(arrayOfSensors.get(i).getName().equalsIgnoreCase("Uncalibrated Gyroscope Sensor")) {
                                    isValid = true;
                                    break;
                                }
                            }

                        } catch (IllegalAccessException e) {
                        } catch (InvocationTargetException e) {
                        }
                    } catch (SecurityException e) { }
                    catch (NoSuchMethodException e) { }
                }

                supported = new Boolean(isValid);
            } else {
                supported = Boolean.FALSE;
            }
        }
        return supported;
    }

    /**
     * Registers a listener and start listening
     * @param gyroscopeListener
     *             callback for Gyroscope events
     */
    public static void startListening( GyroscopeListener gyroscopeListener, long milliseconds )
    {
        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);
        int samplingPeriod = (int) (milliseconds * 1000);
        // Take all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED);

        if (sensors.size() > 0) {

            sensor = sensors.get(0);

            // Register Gyroscope Listener
            running = sensorManager.registerListener(
                    sensorEventListener, sensor,
                    samplingPeriod);
            listener = gyroscopeListener;
        } else {
            java.lang.reflect.Method method = null;
            try {
                Class noparams[] = {};
                method = sensorManager.getClass().getDeclaredMethod("getFullSensorList", noparams);
                try {
                    method.setAccessible(true);
                    ArrayList<Sensor> arrayOfSensors = (ArrayList<Sensor>)method.invoke(sensorManager);
                    method.setAccessible(false);
                    for(int i = 0; i < arrayOfSensors.size(); i++) {
                        if(arrayOfSensors.get(i).getName().equalsIgnoreCase("Uncalibrated Gyroscope Sensor")) {
                            sensor = arrayOfSensors.get(i);
                        }
                    }

                    if(sensor != null) {
                        // Register Uncalibrated Gyroscope Listener
                        running = sensorManager.registerListener(
                                sensorEventListener, sensor,
                                samplingPeriod);

                        listener = gyroscopeListener;
                    }
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            } catch (SecurityException e) { }
            catch (NoSuchMethodException e) { }
        }
    }

    /**
     * The listener that listen to events from the Gyroscope listener
     */
    private static SensorEventListener sensorEventListener =
            new SensorEventListener() {

                // Create a constant to convert nanoseconds to seconds.
                private static final float NS2S = 1.0f / 1000000000.0f;
                private final float[] deltaRotationVector = new float[4];
                private float timestamp;
                final double EPSILON = 1E-14;

                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                public void onSensorChanged(SensorEvent event) {
                    // This timestep's delta rotation to be multiplied by the current rotation
                    // after computing it from the gyro sample data.

                    // Axis of the rotation sample, not normalized yet.
                    float axisX = event.values[0];
                    float axisY = event.values[1];
                    float axisZ = event.values[2];

                    if (timestamp != 0) {
                        final float dT = (event.timestamp - timestamp) * NS2S;

                        // Calculate the angular speed of the sample
                        float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                        // Normalize the rotation vector if it's big enough to get the axis
                        // (that is, EPSILON should represent your maximum allowable margin of error)
                        if (omegaMagnitude > EPSILON) {
                            axisX /= omegaMagnitude;
                            axisY /= omegaMagnitude;
                            axisZ /= omegaMagnitude;
                        }

                        // Integrate around this axis with the angular speed by the timestep
                        // in order to get a delta rotation from this sample over the timestep
                        // We will convert this axis-angle representation of the delta rotation
                        // into a quaternion before turning it into the rotation matrix.
                        float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                        deltaRotationVector[0] = sinThetaOverTwo * axisX;
                        deltaRotationVector[1] = sinThetaOverTwo * axisY;
                        deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                        deltaRotationVector[3] = cosThetaOverTwo;
                    }
                    timestamp = event.timestamp;
                    float[] deltaRotationMatrix = new float[9];
                    SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                    // User code should concatenate the delta rotation we computed with the current rotation
                    // in order to get the updated rotation.
                    // rotationCurrent = rotationCurrent * deltaRotationMatrix;

                    listener.onGyroscopeChanged(axisX, axisY, axisZ);
                }
            };

}
