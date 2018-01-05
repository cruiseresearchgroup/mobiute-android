package com.ute.mobi.services;

import android.content.Context;

import com.ute.mobi.utilities.TransformerUtilities;

/**
 * Created by jonathanliono on 27/04/15.
 */
public class UteCurrentSessionDBService {
  private static UteSessionDBService sessionInstance;

  private UteCurrentSessionDBService()
  {
  }

  public static UteSessionDBService getSessionInstance(Context context) {
    if(sessionInstance == null) {
      String cachedSessionId = AppStateService.getInstance().getCachedSessionId();
      String cachedExperimentId = AppStateService.getInstance().getCachedExperimentId();
      String cachedUniqueId = AppStateService.getInstance().getCachedUniqueId();
      if(cachedUniqueId != null) {
        sessionInstance = new UteSessionDBService(context.getApplicationContext(), getDatabaseFileName(cachedUniqueId) + TransformerUtilities.FILE_EXTENSION_SQLITE, cachedSessionId, cachedExperimentId);
        sessionInstance.open();
      }
    }

    return sessionInstance;
  }

  public static String getDatabaseFileName(String uniqueId) {
    return uniqueId;
  }

  public static void destroyAndClearSessionInstance()
  {
    if(sessionInstance != null)
    {
      sessionInstance.destroy();
    }

    sessionInstance = null;
  }

  public static void closeAndClearSessionInstance()
  {
    if(sessionInstance != null)
    {
      sessionInstance.close();
    }

    sessionInstance = null;
  }

}
