package com.ute.mobi.managers;

import android.location.Location;

/**
 * Created by jonathanliono on 10/04/15.
 */
public interface LocManager {
  public void setNetworkLocation(Location location);
  public void setGPSLocation(Location location);
}
