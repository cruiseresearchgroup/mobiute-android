package com.ute.mobi.activities.helpers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ute.mobi.activities.SessionConnectExperimentActivity;
import com.ute.mobi.activities.tasks.UploadSessionFile;
import com.ute.mobi.models.UteCachedExperiment;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.ServerSettingsService;
import com.ute.mobi.settings.SessionRoleSettings;
import com.ute.mobi.settings.SessionSetupSettings;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;
import com.ute.mobi.utilities.TransformerUtilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by jonathanliono on 23/05/2016.
 */

public class SessionConnectExperimentActivityHelpers {

  public class ExperimentListItem {
    public String experiment_id;
    public boolean is_cacheable;
    public String talias;
    public String title;
    public String description;
    public boolean cached;
    public String uid;

  }

  private SessionConnectExperimentActivity activity;
  private ServerSettingsService settingsService;
  private AppStateService appStateService;

  public SessionConnectExperimentActivityHelpers(SessionConnectExperimentActivity activity, ServerSettingsService settingsService, AppStateService appStateService) {
    this.activity = activity;
    this.settingsService = settingsService;
    this.appStateService = appStateService;
  }

  public ExperimentListItem constructCachedExperimentListItem() {
    ExperimentListItem experimentListItem = new ExperimentListItem();
    experimentListItem.cached = true;

    return experimentListItem;
  }

  public void requestLatestExperimentListHttp() {
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/list");
    } catch (MalformedURLException e) {
      return;
    }

    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.GET, null, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public List<ExperimentListItem> experiments;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        activity.updateConnectListForExperiments(resultObject.experiments, SessionConnectExperimentActivity.LIST_DISPLAYMODE_EXPERIMENTS, true);
      }

      @Override
      public void onErrorUnauthorized() {
        displayUnauthorizedErrorRequest();
        activity.updateConnectListForExperiments(null, SessionConnectExperimentActivity.LIST_DISPLAYMODE_EXPERIMENTS, false);
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        displayGeneralErrorRequest(statusCode);
        activity.updateConnectListForExperiments(null, SessionConnectExperimentActivity.LIST_DISPLAYMODE_EXPERIMENTS, false);
      }

      @Override
      public void onNoNetworkAvailable() {
        displayNoNetworkAvailable();
        activity.updateConnectListForExperiments(null, SessionConnectExperimentActivity.LIST_DISPLAYMODE_EXPERIMENTS, false);
      }

      @Override
      public void onExceptionThrown(Exception e) {
        displayExceptionThrownRequest(e);
        activity.updateConnectListForExperiments(null, SessionConnectExperimentActivity.LIST_DISPLAYMODE_EXPERIMENTS, false);
      }
    });
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  public void requestLatestSessionListHttp(final ArrayAdapter<String> adp, String experimentId) {
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/session/list");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("experiment_id", experimentId);
    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST,
            new HashMap<String, Object>(){{
              put("content", jsonObject.toString());
            }}
            , HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public List<String> sessions;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        activity.updateConnectListForSession(adp, resultObject.sessions, SessionConnectExperimentActivity.LIST_DISPLAYMODE_SESSIONS);
      }

      @Override
      public void onErrorUnauthorized() {
        displayUnauthorizedErrorRequest();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        displayGeneralErrorRequest(statusCode);
      }

      @Override
      public void onNoNetworkAvailable() {
        displayNoNetworkAvailable();
      }

      @Override
      public void onExceptionThrown(Exception e) {
        displayExceptionThrownRequest(e);
      }
    });
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  public void createNewSession(final String experiment_id, final String experimentAlias, final int role, final int sessionMode, boolean cached) {
    if(sessionMode == SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_CREATESESSION && !cached) {
      this.requestToCreateNewSession(experiment_id, experimentAlias, role);
    } else if (cached) {
      final UteCachedExperiment cachedExperiment = appStateService.findCachedExperimentById(experiment_id);
      if(sessionMode == SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_CREATESESSION) {
        handleSuccessfullSessionCreation(
                cachedExperiment.experiment_id,
                cachedExperiment.experiment_alias,
                "", // empty session id.
                role,
                appStateService.getCurrentTimeStamp(),
                cachedExperiment.settings, true);
      } else if (sessionMode == SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_CONNECTSESSION) {
        // request for the session role.
        new AlertDialog.Builder(activity)
                .setTitle("Role selection")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Sensing", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    // logic for role selection
                    handleSuccessfullSessionCreation(
                            cachedExperiment.experiment_id,
                            cachedExperiment.experiment_alias,
                            "", // empty session id.
                            SessionRoleSettings.ROLE_SENSING,
                            appStateService.getCurrentTimeStamp(),
                            cachedExperiment.settings, false);
                  }})
                .setNegativeButton("Labeling", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int whichButton) {
                    // logic for role selection
                    handleSuccessfullSessionCreation(
                            cachedExperiment.experiment_id,
                            cachedExperiment.experiment_alias,
                            "", // empty session id.
                            SessionRoleSettings.ROLE_LABELING,
                            appStateService.getCurrentTimeStamp(),
                            cachedExperiment.settings, false);
                  }})
                .setNeutralButton(android.R.string.no, null).show();
      }
    }
  }

  public void requestToCreateNewSession(final String experiment_id, final String experimentAlias, final int role) {
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/create");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("experiment_id", experiment_id);
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    HttpAsyncTask task = new HttpAsyncTask(activity, HttpAsyncTask.Method.POST, new HashMap<String, Object>(){{
      put("content", jsonObject.toString());
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String sessionId;
        public Double created_at;
        public SessionSetupSettings settings;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        handleSuccessfullSessionCreation(experiment_id, experimentAlias, resultObject.sessionId, role, resultObject.created_at, resultObject.settings, true);
      }

      @Override
      public void onErrorUnauthorized() {
        displayUnauthorizedErrorRequest();
      }

      @Override
      public void onErrorGeneralRequest(final int statusCode) {
        displayGeneralErrorRequest(statusCode);
      }

      @Override
      public void onNoNetworkAvailable() {
        displayNoNetworkAvailable();
      }

      @Override
      public void onExceptionThrown(final Exception e) {
        displayExceptionThrownRequest(e);
      }
    }){
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if (activity.isActivityStillActive()) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("New Session");
          dialog.setMessage("Loading...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if (activity.isActivityStillActive()) {
          dialog.dismiss();
        }
        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  private void handleSuccessfullSessionCreation(String experiment_id, String experimentAlias, String sessionId, int role, Double created_at, SessionSetupSettings settings, boolean isInitiator) {
    // generate unique uuid
    String uniqueId = UUID.randomUUID().toString();

    appStateService.setUniqueIdCache(uniqueId);
    appStateService.setExperimentIdCache(experiment_id);
    appStateService.setSessionIdCache(sessionId);
    appStateService.setRoleCache(role);
    appStateService.setIsInitiatorCache(isInitiator);
    appStateService.setSessionServerStartTime(created_at != null ? created_at.doubleValue() : 0);

    if(settings != null) {
      appStateService.setSessionSetupSettings(settings);
    }

    appStateService.addSessionRecord(uniqueId, experiment_id, experimentAlias, sessionId, uniqueId + TransformerUtilities.FILE_EXTENSION_SQLITE, sessionId == "" ? true : false, appStateService.getCurrentTimeStamp(), isInitiator);

    activity.onSessionCreated(uniqueId, sessionId, experiment_id, experimentAlias, created_at, role, isInitiator);
  }

  public void requestToCacheExperiment(final String otp, final UteCachedExperiment cache) {
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/pairdevice");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("experiment_id", cache.experiment_id);
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("otp", otp);
    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST,
            new HashMap<String, Object>(){{
              put("content", jsonObject.toString());
            }}
            , HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
        public String uid;
        public Double server_time;
        public Double campaign_end_at;
        public SessionSetupSettings settings;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          cache.settings        = resultObject.settings;
          cache.campaign_end_at = resultObject.campaign_end_at;
          cache.uid             = resultObject.uid;
          cache.server_time     = resultObject.server_time;

          boolean isCached = appStateService.cacheExperiment(cache);
          if(isCached) {
            displayCustomMessage("Experiment Caching", "The experiment has been cached successfully.");
            requestLatestExperimentListHttp();
          } else {
            displayCustomErrorMessage("Unable to cache this experiment");
          }
        } else {
          displayCustomErrorMessage("Invalid request");
        }
      }

      @Override
      public void onErrorUnauthorized() {
        displayUnauthorizedErrorRequest();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        displayGeneralErrorRequest(statusCode);
      }

      @Override
      public void onNoNetworkAvailable() {
        displayNoNetworkAvailable();
      }

      @Override
      public void onExceptionThrown(Exception e) {
        displayExceptionThrownRequest(e);
      }
    }){
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if (activity.isActivityStillActive()) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("INFO");
          dialog.setMessage("Requesting to cache...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if (activity.isActivityStillActive()) {
          dialog.dismiss();
        }
        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  public void requestToUncacheExperiment(final String experimentId, final String uid) {
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/unpairdevice");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("experiment_id", experimentId);
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("uid", uid);
    HttpAsyncTask task = new HttpAsyncTask(this.activity, HttpAsyncTask.Method.POST,
            new HashMap<String, Object>(){{
              put("content", jsonObject.toString());
            }}
            , HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status != null && resultObject.status.equalsIgnoreCase("OK")) {
          appStateService.uncacheExperimentSynced(experimentId);
          displayCustomMessage("Uncache Experiment", "The experiment has been uncached successfully.");
          requestLatestExperimentListHttp();
        } else {
          displayCustomErrorMessage("Unable to uncache this experiment");
        }
      }

      @Override
      public void onErrorUnauthorized() {
        displayUnauthorizedErrorRequest();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        displayGeneralErrorRequest(statusCode);
      }

      @Override
      public void onNoNetworkAvailable() {
        displayNoNetworkAvailable();
      }

      @Override
      public void onExceptionThrown(Exception e) {
        displayExceptionThrownRequest(e);
      }
    }){
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if (activity.isActivityStillActive()) {
          dialog = new ProgressDialog(activity);
          dialog.setTitle("INFO");
          dialog.setMessage("Requesting to uncache...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if (activity.isActivityStillActive()) {
          dialog.dismiss();
        }
        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  private void displayUnauthorizedErrorRequest() {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("Unauthorized request")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      });
    }
  }

  private void displayGeneralErrorRequest(final int statusCode) {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (activity.isActivityStillActive()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Error: " + statusCode)
                    .setMessage("Error sending request")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
          }
        }
      });
    }
  }

  private void displayNoNetworkAvailable() {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      });
    }
  }

  private void displayExceptionThrownRequest(final Exception e) {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      });
    }
  }

  private void displayCustomMessage(final String title, final String message) {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle(title)
                  .setMessage(message)
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      });
    }
  }

  private void displayCustomErrorMessage(final String message) {
    if(activity.isActivityStillActive()) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(activity)
                  .setTitle("Error")
                  .setMessage(message)
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      });
    }
  }
}
