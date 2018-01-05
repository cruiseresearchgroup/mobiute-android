package com.ute.mobi.utilities;

import android.content.Context;

/**
 * Created by jonathanliono on 23/05/2016.
 */

public class TransformerUtilities {

  public static final String FILE_EXTENSION_SQLITE = ".sqlite";

  public static long convertHzToMillisec(double hz) {
    return new Double(round(1000.0/hz, 0)).longValue();
  }

  public static double round(double value, int places) {
    if (places < 0) throw new IllegalArgumentException();

    long factor = (long) Math.pow(10, places);
    value = value * factor;
    long tmp = Math.round(value);
    return (double) tmp / factor;
  }

  public static int dp2px(Context context, int dp) {
    float scale = context.getResources().getDisplayMetrics().density;
    int pixels = (int) (dp * scale + 0.5f);
    return pixels;
  }
}
