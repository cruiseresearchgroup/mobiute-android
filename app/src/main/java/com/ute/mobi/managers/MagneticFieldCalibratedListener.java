package com.ute.mobi.managers;

/**
 * Created by jonathanliono on 11/01/15.
 */
public interface MagneticFieldCalibratedListener {
    public void onCalibratedMagneticFieldChanged(float x, float y, float z, int accuracy);
}
