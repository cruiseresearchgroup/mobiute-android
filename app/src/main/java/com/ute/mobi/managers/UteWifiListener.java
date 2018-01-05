package com.ute.mobi.managers;

import com.ute.mobi.models.UteModelWifiInfo;

import java.util.List;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public interface UteWifiListener {
  public void onWifiChange(List<UteModelWifiInfo> wifis);
}
