package com.ute.mobi.settings;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonathanliono on 30/12/2015.
 */
public class SessionSetupSettings {
  public class SettingsSensor {
    public String name;
    public Double freq;
    public Double sec;
  }

  public class SettingsLabel {
    public String type;
    public ArrayList<SettingsLabelInterval> schema;
  }

  public class SettingsLabelInterval {
    public String set_name;
    public ArrayList<String> set;
    public boolean is_nullable;
    public boolean only_can_select_one;
  }

  public double version;
  public int maximumRecordingDuration;
  public SettingsSensor[] sensors;
  public SettingsLabel label;

  public static void setupSessionBasedOnSettings(Context context) {

  }
}
