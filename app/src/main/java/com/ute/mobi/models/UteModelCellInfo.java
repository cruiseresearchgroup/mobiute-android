package com.ute.mobi.models;

import java.util.HashMap;

/**
 * Created by jonathanliono on 31/12/2016.
 */

public class UteModelCellInfo {
  public final static String KEY_TIMESTAMP = "t";

  public long id;
  public Integer cid; // cell id
  public Integer lac; // local area code
  public Integer ss; // signal strength
  public Double timestamp;

  public HashMap<String, Object> toHashMap() {
    HashMap<String, Object> hashmap = new HashMap<String, Object>();

    hashmap.put("id", this.id);
    hashmap.put("cid", this.cid);
    hashmap.put("lac", this.lac);
    hashmap.put("ss", this.ss);
    hashmap.put("t", this.timestamp);

    return hashmap;
  }
}
