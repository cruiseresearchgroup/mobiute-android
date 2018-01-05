package com.ute.mobi.utilities;

import android.util.Log;

/**
 * Created by jonathanliono on 21/12/2015.
 */
public class AndroidLogger {
  public static void v(String tag, String msg) {
    Log.v(tag, msg);
  }

  public static void d(String tag, String msg) {
    Log.d(tag, msg);
  }

  public static void w(String tag, String msg) {
    Log.w(tag, msg);
  }

  public static void e(String tag, String msg) {
    Log.e(tag, msg);
  }

  public static void i(String tag, String msg) {
    Log.i(tag, msg);
  }
}
