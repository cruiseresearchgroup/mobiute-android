package com.ute.mobi.models;

import java.util.HashMap;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class UteModelWifiInfo {

  public final static String KEY_TIMESTAMP = "t";

  public long id;
  public String ssid;
  public String bssid;
  public String capabilities;
  public Integer channel_width;
  public boolean dual_channel;
  public Integer freq20;
  public Integer freq_center;
  public Integer freq_center_2;
  public String venue_name;
  public Double rssi;
  public Double timestamp;

  public HashMap<String, Object> toHashMap() {
    HashMap<String, Object> hashmap = new HashMap<String, Object>();

    hashmap.put("id", this.id);
    hashmap.put("ssid", this.ssid);
    hashmap.put("bssid", this.bssid);
    hashmap.put("cap", this.capabilities);
    hashmap.put("bw", this.channel_width);
    hashmap.put("i_d", this.dual_channel);
    hashmap.put("f20", this.freq20);
    hashmap.put("fc", this.freq_center);
    hashmap.put("fc2", this.freq_center_2);
    hashmap.put("vn", this.venue_name);
    hashmap.put("rssi", this.rssi);
    hashmap.put("t", this.timestamp);

    return hashmap;
  }
}
