package com.ute.mobi.models;

import java.util.HashMap;

/**
 * Created by jonathanliono on 26/05/2016.
 */

public class UteModelIntervalLabels {
  public final static String LABEL_SPLITTER = ":";
  public final static String KEY_T_START = "t_start";

  public Double start_date;
  public Double end_date;
  public String labels;

  public HashMap<String, Object> toHashMap() {
    HashMap<String, Object> hashmap = new HashMap<String, Object>();
    hashmap.put(KEY_T_START, this.start_date);
    hashmap.put("t_end", this.end_date);

    if(labels != null && labels.isEmpty() == false) {
      String[] labels = this.labels.split(LABEL_SPLITTER);
      hashmap.put("labels", labels);
    }

    return hashmap;
  }
}
