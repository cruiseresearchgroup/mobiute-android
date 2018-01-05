package com.ute.mobi.managers;

import android.bluetooth.BluetoothDevice;

import com.ute.mobi.models.UteModelBluetoothInfo;

import java.util.List;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public interface BluetoothListener {
  public void onBluetoothChanged(List<UteModelBluetoothInfo> devices);
}
