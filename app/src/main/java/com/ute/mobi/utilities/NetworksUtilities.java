package com.ute.mobi.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by jonathanliono on 23/12/2015.
 * need <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"></uses-permission>
 */
public final class NetworksUtilities {
  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivityManager
            = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null;
  }

  public static boolean isOnWifi(Context context) {
    String buildModelName = android.os.Build.MODEL;
    if (buildModelName.equals("google_sdk") || buildModelName.contains("SDK built") || buildModelName.contains("google_sdk")) {
      // emulator
      return true;
    } else {
      //not emulator
      ConnectivityManager connManager
              = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo mWifi = connManager.getActiveNetworkInfo();

      if(mWifi == null)
        return false;

      if (mWifi.getType() == ConnectivityManager.TYPE_WIFI && mWifi.isConnected()) {
        return true;
      }

      return false;
    }
  }
}
