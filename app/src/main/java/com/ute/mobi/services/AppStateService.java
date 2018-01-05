package com.ute.mobi.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ute.mobi.models.UteCachedExperiment;
import com.ute.mobi.models.UteSessionRecording;
import com.ute.mobi.settings.SessionSetupSettings;
import com.ute.mobi.utilities.AndroidLogger;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class AppStateService {
  private Context context;
  private SharedPreferences prefs;
  private static AppStateService instance;
  //private PendingIntent scheduledSensorReadingIntent;

  public static final String KEY_SH_DEVICE_ID = "com.android.sh.deviceid";
  public static final String KEY_SH_SERVER_ADDRESS = "com.android.sh.serveraddress";
  public static final String KEY_SH_APP_VERSION = "com.android.sh.appversion";
  private static final String KEY_SH_UNIQUE_ID = "com.android.sh.cached.uniqueid";
  private static final String KEY_SH_EXPERIMENT_ID = "com.android.sh.cached.experimentid";
  private static final String KEY_SH_SESSION_ID = "com.android.sh.cached.sessionid";
  private static final String KEY_SH_ROLE = "com.android.sh.cached.role";
  private static final String KEY_SH_IS_INITIATOR = "com.android.sh.cached.isinitiator";
  private static final String KEY_SH_SESSION_SETUP_SETTINGS = "com.android.sh.cached.session.settings";
  private static final String KEY_SH_SESSION_SERVER_START_TIME = "com.android.sh.cached.session.server.starttime";
  private static final String KEY_SH_SESSION_DEVICE_START_TIME = "com.android.sh.cached.session.device.starttime";
  private static final String KEY_SH_SESSION_SESSION_INFO_CHUNK = "com.android.sh.cached.session.sessioninfo.chunk.cached";
  private static final String KEY_SH_SESSION_SESSION_INFO_CHUNK_LAST_SEND = "com.android.sh.cached.session.sessioninfo.chunk.lastsend";
  private static final String KEY_SH_SESSION_INTERVAL_LABELS_CURRENTLABEL = "com.android.sh.cached.session.intervallabels.currentlabel";
  private static final String KEY_SH_SESSION_INTERVAL_LABELS_CURRENTSTARTDATE = "com.android.sh.cached.session.intervallabels.currentstartdate";
  private static final String KEY_SH_SESSION_CURRENT_RUNNING = "com.android.sh.cached.session.current.isrunning";
  private static final String KEY_SH_SESSION_CURRENT_STOPPING = "com.android.sh.cached.session.current.isstopping";

  private static final String KEY_SAVED_RECORDING = "com.mobi.ute.sessions.saved.recordings";
  private static final String KEY_SAVED_EXPERIMENT = "com.mobi.ute.cached.saved.experiments";

  private static Double cachedSessionServerStartTime;
  private static Double cachedSessionDeviceStartTime;
  private static SessionSetupSettings cachedSessionSetupSettings;

  // static
  public boolean isOnSessionActivity;

  private AppStateService(final Context context) {
    this.context = context.getApplicationContext();
    this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    //this.prefs = this.context.getSharedPreferences(this.context.getPackageName(), Context.MODE_PRIVATE);
    if(this.prefs == null) {
      AndroidLogger.e("UTE", "pref is NONEXISTENCE");
    }
  }

  public static void init(final Context context) {
    if(instance == null)
    {
      instance = new AppStateService(context);
    }
  }

  /*public void setScheduledSensorReadingPendingIntent(PendingIntent pi) {
    AndroidLogger.e("UbiQSenseIntent", "Set session reading pending intent");
    this.scheduledSensorReadingIntent = pi;
  }

  public PendingIntent getScheduledSensorReadingPendingIntent() {
    return this.scheduledSensorReadingIntent;
  }*/

  public static AppStateService getInstance()
  {
    return instance;
  }

  public String getSessionDatabasesLocationFolder() {
    String filepath = "/data/data/" + this.context.getPackageName() + "/databases/";
    File dir = new File(filepath);
    if(dir.exists() == false) {
      dir.mkdir();
    }

    String sessionFilePath = filepath + "sessions/";
    dir = new File(sessionFilePath);
    if(dir.exists() == false) {
      dir.mkdir();
    }

    return sessionFilePath;
  }

  public void clearCachedSessionPreferences() {
    this.prefs.edit()
            .remove(KEY_SH_UNIQUE_ID)
            .remove(KEY_SH_EXPERIMENT_ID)
            .remove(KEY_SH_SESSION_ID)
            .remove(KEY_SH_ROLE)
            .remove(KEY_SH_IS_INITIATOR)
            .remove(KEY_SH_SESSION_SETUP_SETTINGS)
            .remove(KEY_SH_SESSION_SERVER_START_TIME)
            .remove(KEY_SH_SESSION_DEVICE_START_TIME)
            .remove(KEY_SH_SESSION_SESSION_INFO_CHUNK)
            .remove(KEY_SH_SESSION_SESSION_INFO_CHUNK_LAST_SEND)
            .remove(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTLABEL)
            .remove(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTSTARTDATE)
            .remove(KEY_SH_SESSION_CURRENT_RUNNING)
            .remove(KEY_SH_SESSION_CURRENT_STOPPING)
            .apply();
    this.cachedSessionServerStartTime = null;
    this.cachedSessionDeviceStartTime = null;
    this.cachedSessionSetupSettings = null;
  }

  public void setDeviceId(String deviceId) {
    this.prefs.edit().putString(KEY_SH_DEVICE_ID, deviceId).apply();
  }

  public String getDeviceId() {
    String deviceId = this.prefs.getString(KEY_SH_DEVICE_ID, null);
    if(deviceId != null) {
      deviceId = deviceId.toUpperCase();
    }
    return deviceId;
  }

  public void setServerAddress(String serverAddress) {
    this.prefs.edit().putString(KEY_SH_SERVER_ADDRESS, serverAddress).apply();
  }

  public void setAppVersion(String appVersion) {
    if(appVersion == null) {
      this.prefs.edit().remove(KEY_SH_APP_VERSION).apply();
    } else {
      this.prefs.edit().putString(KEY_SH_APP_VERSION, appVersion).apply();
    }
  }

  public String getServerAddress() {
    return this.prefs.getString(KEY_SH_SERVER_ADDRESS, null);
  }

  public void setUniqueIdCache(String uniqueId) {
    this.prefs.edit().putString(KEY_SH_UNIQUE_ID, uniqueId).apply();
  }

  public void setExperimentIdCache(String experimentId) {
    this.prefs.edit().putString(KEY_SH_EXPERIMENT_ID, experimentId).apply();
  }

  public void setSessionIdCache(String sessionId) {
    this.prefs.edit().putString(KEY_SH_SESSION_ID, sessionId).apply();
  }

  public void setRoleCache(int role) {
    this.prefs.edit().putInt(KEY_SH_ROLE, role).apply();
  }

  public void setIsInitiatorCache(boolean isInitator) {
    this.prefs.edit().putBoolean(KEY_SH_IS_INITIATOR, isInitator).apply();
  }

  public String getCachedUniqueId() { return this.prefs.getString(KEY_SH_UNIQUE_ID, null); }

  public String getCachedExperimentId() {
    return this.prefs.getString(KEY_SH_EXPERIMENT_ID, null);
  }

  public String getCachedSessionId() {
    return this.prefs.getString(KEY_SH_SESSION_ID, null);
  }

  public int getCachedRole() {
    return this.prefs.getInt(KEY_SH_ROLE, 0);
  }

  public boolean getCachedIsDeviceInitiator() {
    return this.prefs.getBoolean(KEY_SH_IS_INITIATOR, false);
  }

  public synchronized void setSessionSetupSettings(SessionSetupSettings settings) {
    this.prefs.edit().putString(KEY_SH_SESSION_SETUP_SETTINGS, new Gson().toJson(settings)).apply();
    this.cachedSessionSetupSettings = settings;
  }

  public synchronized SessionSetupSettings getSessionSetupSettings() {
    if(this.cachedSessionSetupSettings != null) {
      return cachedSessionSetupSettings;
    }

    String setupSettings = this.prefs.getString(KEY_SH_SESSION_SETUP_SETTINGS, null);
    if (setupSettings == null) {
      return null;
    }

    SessionSetupSettings sessionSetupSettings = new Gson().fromJson(setupSettings, SessionSetupSettings.class);
    cachedSessionSetupSettings = sessionSetupSettings;
    return sessionSetupSettings;
  }

  public void setCachedSessionInfoChunk(boolean shouldCache) {
    this.prefs.edit().putBoolean(KEY_SH_SESSION_SESSION_INFO_CHUNK, shouldCache).apply();
  }

  public void clearCachedSessionInfoChunk() {
    this.prefs.edit().remove(KEY_SH_SESSION_SESSION_INFO_CHUNK).apply();
  }

  public boolean getCachedSessionInfoChunk() {
    return this.prefs.getBoolean(KEY_SH_SESSION_SESSION_INFO_CHUNK, false);
  }

  public void setCachedSessionInfoChunkLastSend(double timestamp) {
    this.prefs.edit().putString(KEY_SH_SESSION_SESSION_INFO_CHUNK_LAST_SEND, String.valueOf(timestamp)).apply();
  }

  public Double getCachedSessionInfoChunkLastSend() {
    String timestamp = this.prefs.getString(KEY_SH_SESSION_SESSION_INFO_CHUNK_LAST_SEND, null);
    Double lastSend = null;
    if(timestamp != null)
      lastSend = Double.parseDouble(timestamp);
    return lastSend;
  }

  public void setCachedSessionIntervalLabelsCurrentLabel(String labels) {
    this.prefs.edit().putString(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTLABEL, labels).apply();
  }

  public String getCachedSessionIntervalLabelsCurrentLabel() {
    return this.prefs.getString(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTLABEL, null);
  }

  public void setCachedSessionIntervalLabelsCurrentStartDate(double timestamp) {
    this.prefs.edit().putString(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTSTARTDATE, String.valueOf(timestamp)).apply();
  }

  public double getCachedSessionIntervalLabelsCurrentStartTime() {
    String timestamp = this.prefs.getString(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTSTARTDATE, null);
    double startTime = 0;
    if(timestamp != null) {
      startTime = Double.parseDouble(timestamp);
    }
    return startTime;
  }

  public void clearCachedSessionIntervalLabels() {
    this.prefs.edit().remove(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTLABEL)
                     .remove(KEY_SH_SESSION_INTERVAL_LABELS_CURRENTSTARTDATE)
                     .apply();
  }

  public boolean isCurrentSessionRunning() {
    return this.prefs.getBoolean(KEY_SH_SESSION_CURRENT_RUNNING, false);
  }

  public synchronized void setCurrentSessionIsRunning(boolean isRunning) {
    this.prefs.edit().putBoolean(KEY_SH_SESSION_CURRENT_RUNNING, isRunning).apply();
  }

  public boolean isCurrentSessionStopping() {
    return this.prefs.getBoolean(KEY_SH_SESSION_CURRENT_STOPPING, false);
  }

  public void setCurrentSessionIsStopping(boolean isStopping) {
    this.prefs.edit().putBoolean(KEY_SH_SESSION_CURRENT_STOPPING, isStopping).apply();
  }

  public void setSessionServerStartTime(double timestamp) {
    double deviceStartTimeStamp = this.getCurrentTimeStamp();
    this.prefs.edit().putString(KEY_SH_SESSION_SERVER_START_TIME, String.valueOf(timestamp))
            .putString(KEY_SH_SESSION_DEVICE_START_TIME, String.valueOf(deviceStartTimeStamp)).apply();

    cachedSessionServerStartTime = timestamp;
    cachedSessionDeviceStartTime = deviceStartTimeStamp;
  }

  public double getSessionServerStartTime() {
    if (cachedSessionServerStartTime != null) {
      return cachedSessionServerStartTime.doubleValue();
    }

    String sessionStartTimeStr = this.prefs.getString(KEY_SH_SESSION_SERVER_START_TIME, null);
    if (sessionStartTimeStr == null) {
      return 0;
    }

    try {
      double sessionStartTime = Double.parseDouble(sessionStartTimeStr);
      cachedSessionServerStartTime = sessionStartTime;
      return sessionStartTime;
    } catch (Exception e) {
      return 0;
    }
  }

  public double getSessionDeviceStartTime() {
    if (cachedSessionDeviceStartTime != null) {
      return cachedSessionDeviceStartTime.doubleValue();
    }

    String sessionStartTimeStr = this.prefs.getString(KEY_SH_SESSION_DEVICE_START_TIME, null);
    if (sessionStartTimeStr == null) {
      return 0;
    }

    try {
      double sessionStartTime = Double.parseDouble(sessionStartTimeStr);
      cachedSessionDeviceStartTime = sessionStartTime;
      return sessionStartTime;
    } catch (Exception e) {
      return 0;
    }
  }

  public double getSynchronizedCurrentTime() {
    long currentTimeMillis = System.currentTimeMillis();
    double currentTimeStamp = ((double) currentTimeMillis) / 1000;

    double serverStartTime = this.getSessionServerStartTime();
    double deviceStartTime = this.getSessionDeviceStartTime();
    if (serverStartTime == 0 || deviceStartTime == 0) {
      return currentTimeStamp;
    }

    double delta = serverStartTime - deviceStartTime;
    return currentTimeStamp + delta;
  }

  public double getCurrentTimeStamp() {
    long currentTimeMillis = System.currentTimeMillis();
    double currentTimeStamp = ((double) currentTimeMillis) / 1000;
    return currentTimeStamp;
  }

  //================================================================================
  // Session Records for tracking.
  //================================================================================

  public void addSessionRecord(String uniqueId, String experimentId, String experimentAlias, String sessionId, String dbPath, boolean isOffline, Double createdAt, boolean isInitiator) {
    UteSessionRecording recording = new UteSessionRecording();
    recording.unique_id = uniqueId;
    recording.experiment_id = experimentId;
    recording.experiment_alias = experimentAlias;
    recording.session_id = sessionId;
    recording.db_path = dbPath;
    recording.is_offline = isOffline;
    recording.created_at = createdAt;
    recording.is_initiator = isInitiator;

    // retrieve current session recording list.
    List<UteSessionRecording> list = this.getSessionRecords();
    list.add(recording);
    String jsonList = new Gson().toJson(list);
    this.prefs.edit().putString(KEY_SAVED_RECORDING, jsonList).apply();
  }

  public List<UteSessionRecording> getSessionRecords() {
    Gson gson = new Gson();
    List<UteSessionRecording> list = null;
    String jsonList = this.prefs.getString(KEY_SAVED_RECORDING, null);
    if(jsonList != null && jsonList.isEmpty() == false) {
      list = gson.fromJson(jsonList, new TypeToken<List<UteSessionRecording>>(){}.getType());
    }

    if(list == null) {
      list = new ArrayList<UteSessionRecording>();
    }

    return list;
  }

  public void deleteSessionRecordSynced(String uniqueId) {
    List<UteSessionRecording> list = getSessionRecords();
    if(list != null && list.size() > 0) {
      // find the index of array
      int foundIndex = -1;
      for(int i = 0; i < list.size(); i++) {
        if(list.get(i).unique_id.equalsIgnoreCase(uniqueId)) {
          foundIndex = i;
          break;
        }
      }

      if(foundIndex != -1) {
        list.remove(foundIndex);
      }
    }

    String jsonList = new Gson().toJson(list);
    this.prefs.edit().putString(KEY_SAVED_RECORDING, jsonList).apply();
  }

  public void updateSessionRecordSynced(String uniqueId, String sessionIdForUpdate) {
    if(sessionIdForUpdate == null || sessionIdForUpdate.isEmpty())
      return;

    List<UteSessionRecording> recordings = this.getSessionRecords();
    for(int i = 0; i < recordings.size(); i++) {
      UteSessionRecording recording = recordings.get(i);
      if(recording.unique_id != null && recording.unique_id.equalsIgnoreCase(uniqueId)) {
        // found the recording to update.
        recording.session_id = sessionIdForUpdate;
        break;
      }
    }

    String jsonList = new Gson().toJson(recordings);
    this.prefs.edit().putString(KEY_SAVED_RECORDING, jsonList).apply();
  }

  //================================================================================
  // Experiment caching.
  //================================================================================

  public List<UteCachedExperiment> getCachedExperiments() {
    Gson gson = new Gson();
    List<UteCachedExperiment> temp = null;
    List<UteCachedExperiment> list = new ArrayList<UteCachedExperiment>();
    String jsonList = this.prefs.getString(KEY_SAVED_EXPERIMENT, null);
    if(jsonList != null && jsonList.isEmpty() == false) {
      temp = gson.fromJson(jsonList, new TypeToken<List<UteCachedExperiment>>(){}.getType());
    }

    if(temp != null && temp.size() > 0) {
      for(int i = 0; i < temp.size(); i++) {
        UteCachedExperiment cached = temp.get(i);
        if(cached.campaign_end_at != null) {
          if(cached.campaign_end_at < getCurrentTimeStamp()) {
            continue;
          }
        }

        list.add(cached);
      }
    }

    return list;
  }

  public UteCachedExperiment findCachedExperimentById(String experimentId) {
    if(experimentId == null || experimentId.isEmpty()) {
      return null;
    }

    List<UteCachedExperiment> cachedExperiments = this.getCachedExperiments();
    for(int i = 0; i < cachedExperiments.size(); i++) {
      UteCachedExperiment cached = cachedExperiments.get(i);
      if(cached.experiment_id != null && cached.experiment_id.equalsIgnoreCase(experimentId)) {
        return cached;
      }
    }

    return null;
  }

  public boolean cacheExperiment(UteCachedExperiment cache) {
    if(cache == null || cache.experiment_id == null || cache.experiment_id.isEmpty()) {
      return false;
    }

    List<UteCachedExperiment> cachedExperiments = this.uncacheExperimentFromCollection(cache.experiment_id);
    cachedExperiments.add(cache);
    String jsonList = new Gson().toJson(cachedExperiments);
    this.prefs.edit().putString(KEY_SAVED_EXPERIMENT, jsonList).apply();

    return true;
  }

  public List<UteCachedExperiment> uncacheExperimentFromCollection(String experimentId) {
    List<UteCachedExperiment> cachedExperiments = this.getCachedExperiments();

    // remove if cached id is null OR not equal to param.
    for (Iterator<UteCachedExperiment> iterator = cachedExperiments.iterator(); iterator.hasNext();) {
      UteCachedExperiment cached = iterator.next();
      if(cached.experiment_id == null || cached.experiment_id.equalsIgnoreCase(experimentId)) {
        iterator.remove();
      }
    }

    return cachedExperiments;
  }

  public void uncacheExperimentSynced(String experimentId) {
    List<UteCachedExperiment> cachedExperiments = this.uncacheExperimentFromCollection(experimentId);
    String jsonList = new Gson().toJson(cachedExperiments);
    this.prefs.edit().putString(KEY_SAVED_EXPERIMENT, jsonList).apply();
  }
}
