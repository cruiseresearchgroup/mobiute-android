package com.ute.mobi.models;

import java.util.HashMap;

/**
 * Created by jonathanliono on 20/11/2016.
 */

public class UteModelBluetoothInfo {
  public final static String KEY_TIMESTAMP = "t";

  public long id;
  public String uuid;
  public String name;
  public Double rssi;
  public Double timestamp;

  public HashMap<String, Object> toHashMap() {
    HashMap<String, Object> hashmap = new HashMap<String, Object>();

    hashmap.put("id", this.id);
    hashmap.put("uuid", this.uuid);
    hashmap.put("name", this.name);
    hashmap.put("rssi", this.rssi);

    hashmap.put("t", this.timestamp);

    return hashmap;
  }

}
