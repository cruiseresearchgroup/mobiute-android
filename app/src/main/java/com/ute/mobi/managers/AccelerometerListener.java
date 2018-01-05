package com.ute.mobi.managers;

/**
 * Created by jonathanliono on 9/01/15.
 */
public interface AccelerometerListener {

    public void onAccelerationChanged(float x, float y, float z);

    public void onShake(float force);

}