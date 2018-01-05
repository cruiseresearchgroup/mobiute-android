package com.ute.mobi.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ute.mobi.models.UteCachedExperiment;
import com.ute.mobi.models.UteModelBluetoothInfo;
import com.ute.mobi.models.UteModelCellInfo;
import com.ute.mobi.models.UteModelIntervalLabels;
import com.ute.mobi.models.UteModelSensorInfo;
import com.ute.mobi.models.UteModelWifiInfo;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonathanliono on 25/05/2016.
 */

public class SensorInfosUploadService {
  private int FLAT_SENSORINFOS_LIMIT = 1000;
  private int FLAT_BLUETOOTHINFOS_LIMIT = 5000;
  private int FLAT_WIFIINFOS_LIMIT = 1000;
  private int FLAT_CELLINFOS_LIMIT = 5000;
  private int FLAT_INTERVALLABELS_LIMIT = 1000;

  private int HTTP_TIMEOUT = 60 * 1000;

  private Activity activity;
  private ServerSettingsService settingsService;
  private AppStateService appStateService;
  private UteSessionDBService dbService;

  private String uniqueId;
  private String sessionId;
  private String experimentId;
  private boolean isInitiator;

  private boolean isSendingSensorInfo;

  private OnFinishSubmitListener onFinishSubmitListener;

  public interface OnFinishSubmitListener {
    void executeOnSuccess();
    void executeOnFailure();
  }

  public SensorInfosUploadService(Activity activity,
                                  ServerSettingsService settingsService,
                                  AppStateService appStateService,
                                  UteSessionDBService dbService,
                                  String uniqueId,
                                  String sessionId,
                                  String experimentId,
                                  boolean isInitiator) {
    this.activity = activity;
    this.settingsService = settingsService;
    this.appStateService = appStateService;
    this.dbService = dbService;
    this.uniqueId = uniqueId;
    this.sessionId = sessionId;
    this.experimentId = experimentId;
    this.isInitiator = isInitiator;
  }

  public void sendSessionData(OnFinishSubmitListener onFinishSubmitListener) {
    this.onFinishSubmitListener = onFinishSubmitListener;
    // display loading view before sending HTTP request to finish the session

    //self.isStoppingSession = true;
    while(this.isSendingSensorInfo) {
      // wait until sessioninfo finish sending from scheduler
    }

    this.sendChunkSensorInfosGeneric(true);
  }

  private static final int MAXIMUM_ATTEMPT = 6;

  private void sendChunkSensorInfosGeneric(boolean isRecursive) {
    this.isSendingSensorInfo = true;

    ArrayList result = this.dbService.fetchSessionInfosByLimit(FLAT_SENSORINFOS_LIMIT);

    Log.i("test", "RESULT COUNT:" + result.size());
    if(isRecursive) {
      Log.w("test", "HITTING Recursive");
    }

    if(isRecursive) {
      if(result.size() == 0) {
        // send the session stop request
        //this.stopSession();
        this.sendChunkBluetoothInfosGeneric(true);
        return;
      }
    }

    this.sendStreamSessionInfos(result, 1, isRecursive, isRecursive);
  }

  private void sendStreamSessionInfos(final ArrayList<HashMap<String, Object>> infosToSend, final int attempt, final boolean needUserFeedback, final boolean isRecursive) {
    // default for needUserFeedBack is true
    if (attempt == MAXIMUM_ATTEMPT) {
      this.setNotSendingSensorInfo();
      if(isRecursive) {
        this.sendStreamSessionInfos(infosToSend, 1, needUserFeedback, isRecursive);
      }
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/sensors/submit");
    } catch (MalformedURLException e) {
      return;
    }

    Log.d("test", "sending items: " + infosToSend.size());
    final boolean isOfflineMode = this.sessionId == null || this.sessionId.isEmpty();
    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("experiment_id", experimentId);
    if(isOfflineMode) {
      UteCachedExperiment cached = appStateService.findCachedExperimentById(this.experimentId);
      if(cached != null) {
        jsonObject.addProperty("uid", cached.uid);
        jsonObject.addProperty("is_initiator", this.isInitiator);
      }
    } else {
      jsonObject.addProperty("session_id", this.sessionId);
    }

    JsonArray jsonArray = new JsonArray();
    for(HashMap<String, Object> item : infosToSend) {
      JsonObject obj = new JsonObject();
      for(Map.Entry<String, Object> entry : item.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if(value != null && (value instanceof Double
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Long)
                ) {
          obj.addProperty(key, (Number) value);
        } else if(value != null
                && (value instanceof String)
                ) {
          obj.addProperty(key, (String) value);
        }
      }

      jsonArray.add(obj);
    }
    jsonObject.add("sensor_infos", jsonArray);

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject);
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
        public String session_id;
        public int code;
      }
      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          // delete session infos since last send timestamp
          double lasttimestamp = (double) infosToSend.get(infosToSend.size() - 1).get(UteModelSensorInfo.KEY_TIMESTAMP) + 0.001;
          Log.w("test", "deleting session info since last send timestamp: " + lasttimestamp);
          dbService.deleteSessionInfosBeforeAndEqualTo(lasttimestamp);

          if(isOfflineMode) {
            if(resultObject.session_id != null) {
              sessionId = resultObject.session_id;
              dbService.updateSessionId(sessionId);
              appStateService.updateSessionRecordSynced(uniqueId, resultObject.session_id);
            }
          }


          setNotSendingSensorInfo();
          if(isRecursive) {
            Log.w("test", "HITTING Recursive sendChunkSensorInfosGeneric");
            sendChunkSensorInfosGeneric(true);
          }
        } else {
          if (resultObject.code == 404) {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Initiator session has not been uploaded yet. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          } else {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Error in submitting data. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          }
        }
      }

      @Override
      public void onErrorUnauthorized() {
        onFinishSubmitListener.executeOnFailure();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if(statusCode == 500) {
          onFinishSubmitListener.executeOnFailure();
        } else {
          // delay and call the same thing
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              Log.e("test", "fail general request attempt" + (attempt + 1));
              sendStreamSessionInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
            }
          }, 10*1000);
        }
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail no network attempt" + (attempt + 1));
            sendStreamSessionInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail exception attempt" + (attempt + 1));
            sendStreamSessionInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isRecursive) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("Finishing Session.");
          dialog.setMessage("Sending sensor data...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isRecursive) {
          dialog.dismiss();
        }

        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  // UPLOAD Bluetooth infos
  private void sendChunkBluetoothInfosGeneric(boolean isRecursive) {
    this.isSendingSensorInfo = true;

    ArrayList result = this.dbService.fetchBluetoothInfosByLimit(FLAT_BLUETOOTHINFOS_LIMIT);

    Log.i("test", "RESULT COUNT:" + result.size());
    if(isRecursive) {
      Log.w("test", "HITTING Recursive");
    }

    if(isRecursive) {
      if(result.size() == 0) {
        // send the session stop request
        //this.stopSession();
        this.sendChunkWifiInfosGeneric(true);
        return;
      }
    }

    this.sendStreamBluetoothInfos(result, 1, isRecursive, isRecursive);
  }

  private void sendStreamBluetoothInfos(final ArrayList<HashMap<String, Object>> infosToSend, final int attempt, final boolean needUserFeedback, final boolean isRecursive) {
    // default for needUserFeedBack is true
    if (attempt == MAXIMUM_ATTEMPT) {
      this.setNotSendingSensorInfo();
      if(isRecursive) {
        this.sendStreamBluetoothInfos(infosToSend, 1, needUserFeedback, isRecursive);
      }
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/bluetooths/submit");
    } catch (MalformedURLException e) {
      return;
    }

    Log.d("test", "sending items: " + infosToSend.size());
    final boolean isOfflineMode = this.sessionId == null || this.sessionId.isEmpty();
    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("experiment_id", experimentId);
    if(isOfflineMode) {
      UteCachedExperiment cached = appStateService.findCachedExperimentById(this.experimentId);
      if(cached != null) {
        jsonObject.addProperty("uid", cached.uid);
        jsonObject.addProperty("is_initiator", this.isInitiator);
      }
    } else {
      jsonObject.addProperty("session_id", this.sessionId);
    }

    JsonArray jsonArray = new JsonArray();
    for(HashMap<String, Object> item : infosToSend) {
      JsonObject obj = new JsonObject();
      for(Map.Entry<String, Object> entry : item.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if(value != null && (value instanceof Double
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Long)
                ) {
          obj.addProperty(key, (Number) value);
        } else if(value != null
                && (value instanceof String)
                ) {
          obj.addProperty(key, (String) value);
        }
      }

      jsonArray.add(obj);
    }
    jsonObject.add("bluetooth_infos", jsonArray);

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject);
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
        public String session_id;
        public int code;
      }
      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          // delete session infos since last send timestamp
          double lasttimestamp = (double) infosToSend.get(infosToSend.size() - 1).get(UteModelBluetoothInfo.KEY_TIMESTAMP) + 0.001;
          Object id = infosToSend.get(infosToSend.size() - 1).get("id");
          long lastId = (long) id;
          Log.w("test", "deleting session info since last send timestamp: " + lasttimestamp);
          dbService.deleteBluetoothInfosBeforeAndEqualTo(lasttimestamp, lastId);

          if(isOfflineMode) {
            if(resultObject.session_id != null) {
              sessionId = resultObject.session_id;
              dbService.updateSessionId(sessionId);
              appStateService.updateSessionRecordSynced(uniqueId, resultObject.session_id);
            }
          }


          setNotSendingSensorInfo();
          if(isRecursive) {
            Log.w("test", "HITTING Recursive sendChunkSensorInfosGeneric");
            sendChunkBluetoothInfosGeneric(true);
          }
        } else {
          if (resultObject.code == 404) {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Initiator session has not been uploaded yet. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          } else {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Error in submitting data. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          }
        }
      }

      @Override
      public void onErrorUnauthorized() {
        onFinishSubmitListener.executeOnFailure();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if(statusCode == 500) {
          onFinishSubmitListener.executeOnFailure();
        } else {
          // delay and call the same thing
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              Log.e("test", "fail general request attempt" + (attempt + 1));
              sendStreamBluetoothInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
            }
          }, 10*1000);
        }
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail no network attempt" + (attempt + 1));
            sendStreamBluetoothInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail exception attempt" + (attempt + 1));
            sendStreamBluetoothInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isRecursive) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("Finishing Session.");
          dialog.setMessage("Sending sensor data...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isRecursive) {
          dialog.dismiss();
        }

        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  // UPLOAD WIFI infos
  private void sendChunkWifiInfosGeneric(boolean isRecursive) {
    this.isSendingSensorInfo = true;

    ArrayList result = this.dbService.fetchWifiInfosByLimit(FLAT_WIFIINFOS_LIMIT);

    Log.i("test", "RESULT COUNT:" + result.size());
    if(isRecursive) {
      Log.w("test", "HITTING Recursive");
    }

    if(isRecursive) {
      if(result.size() == 0) {
        // send the session stop request
        //this.stopSession();
        this.sendChunkCellInfosGeneric(true);
        return;
      }
    }

    this.sendStreamWifiInfos(result, 1, isRecursive, isRecursive);
  }

  private void sendStreamWifiInfos(final ArrayList<HashMap<String, Object>> infosToSend, final int attempt, final boolean needUserFeedback, final boolean isRecursive) {
    // default for needUserFeedBack is true
    if (attempt == MAXIMUM_ATTEMPT) {
      this.setNotSendingSensorInfo();
      if(isRecursive) {
        this.sendStreamWifiInfos(infosToSend, 1, needUserFeedback, isRecursive);
      }
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/wifis/submit");
    } catch (MalformedURLException e) {
      return;
    }

    Log.d("test", "sending items: " + infosToSend.size());
    final boolean isOfflineMode = this.sessionId == null || this.sessionId.isEmpty();
    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("experiment_id", experimentId);
    if(isOfflineMode) {
      UteCachedExperiment cached = appStateService.findCachedExperimentById(this.experimentId);
      if(cached != null) {
        jsonObject.addProperty("uid", cached.uid);
        jsonObject.addProperty("is_initiator", this.isInitiator);
      }
    } else {
      jsonObject.addProperty("session_id", this.sessionId);
    }

    JsonArray jsonArray = new JsonArray();
    for(HashMap<String, Object> item : infosToSend) {
      JsonObject obj = new JsonObject();
      for(Map.Entry<String, Object> entry : item.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if(value != null && (value instanceof Double
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Long)
                ) {
          obj.addProperty(key, (Number) value);
        } else if(value != null
                && (value instanceof String)
                ) {
          obj.addProperty(key, (String) value);
        }
      }

      jsonArray.add(obj);
    }
    jsonObject.add("wifi_infos", jsonArray);

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject);
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
        public String session_id;
        public int code;
      }
      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          // delete session infos since last send timestamp
          double lasttimestamp = (double) infosToSend.get(infosToSend.size() - 1).get(UteModelWifiInfo.KEY_TIMESTAMP) + 0.001;
          Object id = infosToSend.get(infosToSend.size() - 1).get("id");
          long lastId = (long) id;
          Log.w("test", "deleting session info since last send timestamp: " + lasttimestamp);
          dbService.deleteWifiInfosBeforeAndEqualTo(lasttimestamp, lastId);

          if(isOfflineMode) {
            if(resultObject.session_id != null) {
              sessionId = resultObject.session_id;
              dbService.updateSessionId(sessionId);
              appStateService.updateSessionRecordSynced(uniqueId, resultObject.session_id);
            }
          }


          setNotSendingSensorInfo();
          if(isRecursive) {
            Log.w("test", "HITTING Recursive sendChunkSensorInfosGeneric");
            sendChunkWifiInfosGeneric(true);
          }
        } else {
          if (resultObject.code == 404) {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Initiator session has not been uploaded yet. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          } else {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Error in submitting data. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          }
        }
      }

      @Override
      public void onErrorUnauthorized() {
        onFinishSubmitListener.executeOnFailure();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if(statusCode == 500) {
          onFinishSubmitListener.executeOnFailure();
        } else {
          // delay and call the same thing
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              Log.e("test", "fail general request attempt" + (attempt + 1));
              sendStreamWifiInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
            }
          }, 10*1000);
        }
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail no network attempt" + (attempt + 1));
            sendStreamWifiInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail exception attempt" + (attempt + 1));
            sendStreamWifiInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isRecursive) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("Finishing Session.");
          dialog.setMessage("Sending sensor data...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isRecursive) {
          dialog.dismiss();
        }

        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  // UPLOAD CELL INFOS
  private void sendChunkCellInfosGeneric(boolean isRecursive) {
    this.isSendingSensorInfo = true;

    ArrayList result = this.dbService.fetchCellInfosByLimit(FLAT_CELLINFOS_LIMIT);

    Log.i("test", "RESULT COUNT:" + result.size());
    if(isRecursive) {
      Log.w("test", "HITTING Recursive");
    }

    if(isRecursive) {
      if(result.size() == 0) {
        // send the session stop request
        //this.stopSession();
        this.sendChunkLabelsGeneric(true);
        return;
      }
    }

    this.sendStreamCellInfos(result, 1, isRecursive, isRecursive);
  }

  private void sendStreamCellInfos(final ArrayList<HashMap<String, Object>> infosToSend, final int attempt, final boolean needUserFeedback, final boolean isRecursive) {
    // default for needUserFeedBack is true
    if (attempt == MAXIMUM_ATTEMPT) {
      this.setNotSendingSensorInfo();
      if(isRecursive) {
        this.sendStreamCellInfos(infosToSend, 1, needUserFeedback, isRecursive);
      }
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/cells/submit");
    } catch (MalformedURLException e) {
      return;
    }

    Log.d("test", "sending items: " + infosToSend.size());
    final boolean isOfflineMode = this.sessionId == null || this.sessionId.isEmpty();
    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("experiment_id", experimentId);
    if(isOfflineMode) {
      UteCachedExperiment cached = appStateService.findCachedExperimentById(this.experimentId);
      if(cached != null) {
        jsonObject.addProperty("uid", cached.uid);
        jsonObject.addProperty("is_initiator", this.isInitiator);
      }
    } else {
      jsonObject.addProperty("session_id", this.sessionId);
    }

    JsonArray jsonArray = new JsonArray();
    for(HashMap<String, Object> item : infosToSend) {
      JsonObject obj = new JsonObject();
      for(Map.Entry<String, Object> entry : item.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if(value != null && (value instanceof Double
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Long)
                ) {
          obj.addProperty(key, (Number) value);
        } else if(value != null
                && (value instanceof String)
                ) {
          obj.addProperty(key, (String) value);
        }
      }

      jsonArray.add(obj);
    }
    jsonObject.add("cell_infos", jsonArray);

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject);
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
        public String session_id;
        public int code;
      }
      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          // delete session infos since last send timestamp
          double lasttimestamp = (double) infosToSend.get(infosToSend.size() - 1).get(UteModelCellInfo.KEY_TIMESTAMP) + 0.001;
          Object id = infosToSend.get(infosToSend.size() - 1).get("id");
          long lastId = (long) id;
          Log.w("test", "deleting session info since last send timestamp: " + lasttimestamp);
          dbService.deleteCellInfosBeforeAndEqualTo(lasttimestamp, lastId);

          if(isOfflineMode) {
            if(resultObject.session_id != null) {
              sessionId = resultObject.session_id;
              dbService.updateSessionId(sessionId);
              appStateService.updateSessionRecordSynced(uniqueId, resultObject.session_id);
            }
          }


          setNotSendingSensorInfo();
          if(isRecursive) {
            Log.w("test", "HITTING Recursive sendChunkSensorInfosGeneric");
            sendChunkCellInfosGeneric(true);
          }
        } else {
          if (resultObject.code == 404) {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Initiator session has not been uploaded yet. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          } else {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Error in submitting data. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          }
        }
      }

      @Override
      public void onErrorUnauthorized() {
        onFinishSubmitListener.executeOnFailure();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if(statusCode == 500) {
          onFinishSubmitListener.executeOnFailure();
        } else {
          // delay and call the same thing
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              Log.e("test", "fail general request attempt" + (attempt + 1));
              sendStreamCellInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
            }
          }, 10*1000);
        }
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail no network attempt" + (attempt + 1));
            sendStreamCellInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail exception attempt" + (attempt + 1));
            sendStreamCellInfos(infosToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isRecursive) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("Finishing Session.");
          dialog.setMessage("Sending sensor data...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isRecursive) {
          dialog.dismiss();
        }

        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  // UPLOAD INTERVAL LABELS
  private void sendChunkLabelsGeneric(boolean isRecursive) {
    this.isSendingSensorInfo = true;

    ArrayList result = this.dbService.fetchSessionIntervalLabelsByLimit(FLAT_INTERVALLABELS_LIMIT);

    Log.i("test", "RESULT COUNT:" + result.size());
    if(isRecursive) {
      Log.w("test", "HITTING Recursive");
    }

    if(isRecursive) {
      if(result.size() == 0) {
        // send the session stop request
        //this.stopSession();
        this.closeSessionConnection(this.onFinishSubmitListener);
        return;
      }
    }

    this.sendStreamSessionIntervalLabels(result, 1, isRecursive, isRecursive);
  }

  private void sendStreamSessionIntervalLabels(final ArrayList<HashMap<String, Object>> intervalLabelsToSend, final int attempt, final boolean needUserFeedback, final boolean isRecursive) {
    // default for needUserFeedBack is true
    if (attempt == 6) {
      this.setNotSendingSensorInfo();
      if(isRecursive) {
        this.sendStreamSessionIntervalLabels(intervalLabelsToSend, 1, needUserFeedback, isRecursive);
      }
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/labels/submit");
    } catch (MalformedURLException e) {
      return;
    }

    Log.d("test", "sending items: " + intervalLabelsToSend.size());
    final boolean isOfflineMode = this.sessionId == null || this.sessionId.isEmpty();
    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("experiment_id", experimentId);
    if(isOfflineMode) {
      UteCachedExperiment cached = appStateService.findCachedExperimentById(this.experimentId);
      if(cached != null) {
        jsonObject.addProperty("uid", cached.uid);
        jsonObject.addProperty("is_initiator", this.isInitiator);
      }
    } else {
      jsonObject.addProperty("session_id", this.sessionId);
    }

    JsonObject labelInfoObject = new JsonObject();

    labelInfoObject.addProperty("type", "interval");
    JsonArray jsonArray = new JsonArray();
    for(HashMap<String, Object> item : intervalLabelsToSend) {
      JsonObject obj = new JsonObject();
      for(Map.Entry<String, Object> entry : item.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if(value != null && (value instanceof Double
                || value instanceof Integer
                || value instanceof Float
                || value instanceof Long)
                ) {
          obj.addProperty(key, (Number) value);
        } else if(value != null && (value instanceof String[])) {
          JsonArray stringArray = new JsonArray();
          for(int is = 0; is < ((String[]) value).length; is++) {
            JsonPrimitive element = new JsonPrimitive(((String[]) value)[is]);
            stringArray.add(element);
          }
          obj.add(key, stringArray);
        }
      }

      jsonArray.add(obj);
    }
    labelInfoObject.add("data", jsonArray);
    jsonObject.add("label_info", labelInfoObject);

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject);
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {

      class ResultWrapper {
        public String status;
        public String session_id;
        public int code;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          // delete session infos since last send timestamp
          double lasttimestamp = (double) intervalLabelsToSend.get(intervalLabelsToSend.size() - 1).get(UteModelIntervalLabels.KEY_T_START) + 0.001;
          Log.w("test", "deleting session info since last send timestamp: " + lasttimestamp);
          dbService.deleteSessionIntervalLabelsBeforeAndEqualTo(lasttimestamp);

          if(isOfflineMode) {
            if(resultObject.session_id != null) {
              sessionId = resultObject.session_id;
              dbService.updateSessionId(sessionId);
              appStateService.updateSessionRecordSynced(uniqueId, resultObject.session_id);
            }
          }

          setNotSendingSensorInfo();
          if(isRecursive) {
            Log.w("test", "HITTING Recursive sendChunkSensorInfosGeneric");
            sendChunkLabelsGeneric(true);
          }
        } else {
          if (resultObject.code == 404) {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Initiator session has not been uploaded yet. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          } else {
            if (needUserFeedback) {
              new AlertDialog.Builder(activity)
                      .setTitle("Error")
                      .setMessage("Error in submitting data. ")
                      .setIcon(android.R.drawable.ic_dialog_alert)
                      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          onFinishSubmitListener.executeOnFailure();
                        }
                      })
                      .show();
            } else {
              onFinishSubmitListener.executeOnFailure();
            }
          }
        }
      }

      @Override
      public void onErrorUnauthorized() {
        onFinishSubmitListener.executeOnFailure();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        // delay and call the same thing
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail general request attempt" + (attempt + 1));
            sendStreamSessionIntervalLabels(intervalLabelsToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail no network attempt" + (attempt + 1));
            sendStreamSessionIntervalLabels(intervalLabelsToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        // delay and repeat
        new Handler().postDelayed(new Runnable() {
          @Override
          public void run() {
            Log.e("test", "fail exception attempt" + (attempt + 1));
            sendStreamSessionIntervalLabels(intervalLabelsToSend, attempt+1, needUserFeedback, isRecursive);
          }
        }, 10*1000);
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isRecursive) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("Finishing Session.");
          dialog.setMessage("Sending sensor data...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isRecursive) {
          dialog.dismiss();
        }

        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  public void closeSessionConnection(final OnFinishSubmitListener onFinishSubmitListener) {
    if(sessionId != null && sessionId.isEmpty()) {
      onFinishSubmitListener.executeOnSuccess();
      return;
    }

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/connection/close");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("session_id", this.sessionId);
    jsonObject.addProperty("experiment_id", this.experimentId);

    HttpAsyncTask task = new HttpAsyncTask(activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject.toString());
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      @Override
      public void onSuccess(String result) {
        onFinishSubmitListener.executeOnSuccess();
      }

      @Override
      public void onErrorUnauthorized() {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage("Unauthorized request")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onFinishSubmitListener.executeOnFailure();
                  }
                })
                .show();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage("Error sending request")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onFinishSubmitListener.executeOnFailure();
                  }
                })
                .show();
      }

      @Override
      public void onNoNetworkAvailable() {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage("No internet connection available")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onFinishSubmitListener.executeOnFailure();
                  }
                })
                .show();
      }

      @Override
      public void onExceptionThrown(final Exception e) {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(e.getLocalizedMessage())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onFinishSubmitListener.executeOnFailure();
                  }
                })
                .show();
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(activity);
        dialog.setTitle("Closing session connection");
        dialog.setMessage("Loading...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
      }

      @Override
      protected void onPostExecute(String result) {
        dialog.dismiss();
        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.setCustomTimeout(HTTP_TIMEOUT, HTTP_TIMEOUT);
    task.execute(requestUrl.toString());
  }

  /*private synchronized void executeOnSuccessListener() {
    if(this.onFinishSubmitListener != null) {
      this.onFinishSubmitListener.executeOnSuccess();
      this.onFinishSubmitListener = null;
    }
  }*/

  private void setNotSendingSensorInfo() {
    this.isSendingSensorInfo = false;
  }
}
