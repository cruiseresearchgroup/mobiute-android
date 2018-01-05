package com.ute.mobi.managers;

/**
 * Created by jonathanliono on 9/11/2016.
 */

public interface BarometerListener {
  public void onBarometerSensorChanged(float pressure, float altitude);
}
