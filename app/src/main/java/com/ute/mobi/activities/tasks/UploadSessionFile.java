package com.ute.mobi.activities.tasks;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.ServerSettingsService;
import com.ute.mobi.services.UteSessionDBService;
import com.ute.mobi.utilities.GeneralCallBackActions;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;
import com.ute.mobi.utilities.TransformerUtilities;

/**
 * Created by jonathanliono on 22/12/2015.
 */
public class UploadSessionFile {
  Context context;
  String filename;
  ServerSettingsService settingsService;

  public UploadSessionFile(Context context, String filename, ServerSettingsService settingsService) {
    this.context = context;
    this.filename = filename;
    this.settingsService = settingsService;
  }

  public static Double ReadMinTimeFromSqliteFile(Context context, File sessionFile) {
    UteSessionDBService sessionInstance = new UteSessionDBService(context.getApplicationContext(), sessionFile.getName(), null, null);
    sessionInstance.open();

    final Double sessionEndTime = sessionInstance.getMinimumTimestamp();

    sessionInstance.close();

    return sessionEndTime;
  }

  public static Double ReadEndTimeFromSqliteFile(Context context, File sessionFile) {
    UteSessionDBService sessionInstance = new UteSessionDBService(context.getApplicationContext(), sessionFile.getName(), null, null);
    sessionInstance.open();

    final Double sessionEndTime = sessionInstance.getLastTimestamp();

    sessionInstance.close();

    return sessionEndTime;
  }

  public void executeUpload(final GeneralCallBackActions.OnSuccess onsuccessCallBack) {
    this.executeUpload(onsuccessCallBack, new GeneralCallBackActions.OnFail() {
      @Override
      public void onFail(String result) {

      }
    });
  }

  public void executeUpload(final GeneralCallBackActions.OnSuccess onsuccessCallBack, final GeneralCallBackActions.OnFail onfailCallBack) {
    this.executeAction(ActionType.Upload, "session/sensors/upload", onsuccessCallBack, onfailCallBack);
  }

  public void executeRemoveConnection(final GeneralCallBackActions.OnSuccess onsuccessCallBack) {
    this.executeAction(ActionType.RemoveConnection, "session/sensors/removeconn", onsuccessCallBack, new GeneralCallBackActions.OnFail() {
      @Override
      public void onFail(String result) {

      }
    });
  }

  private enum ActionType {
    Upload,
    RemoveConnection,
  }

  private void executeAction(final ActionType actionType, String path, final GeneralCallBackActions.OnSuccess onsuccessCallBack, final GeneralCallBackActions.OnFail onfailCallBack) {

    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), path);
    } catch (MalformedURLException e) {
      return;
    }

    final File sessionFile = new File(filename);

    HttpAsyncTask task = new HttpAsyncTask(context, actionType == ActionType.Upload ? HttpAsyncTask.Method.POST_MULTIPART : HttpAsyncTask.Method.POST, new HashMap<String, Object>(){{
      if(actionType == ActionType.Upload) {
        final Double sessionEndTime = ReadEndTimeFromSqliteFile(context, sessionFile);
        put("sessionId", sessionFile.getName().replace(TransformerUtilities.FILE_EXTENSION_SQLITE, ""));
        put("session_type", "UTE");
        put("session_end", sessionEndTime);
        put("did", AppStateService.getInstance().getDeviceId());
        put("model", settingsService.getDeviceModel());
        put("dtype", "Android");
        put("sessionInfos", sessionFile);
      } else {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sessionId", sessionFile.getName().replace(TransformerUtilities.FILE_EXTENSION_SQLITE, ""));
        jsonObject.addProperty("session_type", "UTE");
        jsonObject.addProperty("did", AppStateService.getInstance().getDeviceId());
        jsonObject.addProperty("model", settingsService.getDeviceModel());
        jsonObject.addProperty("dtype", "Android");
        put("content", jsonObject.toString());
      }
    }}, actionType == ActionType.Upload ? null : HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {
      class ResultWrapper {
        public String status;
      }

      @Override
      public void onSuccess(String result) {
        Gson gson = new Gson();
        final ResultWrapper resultObject = gson.fromJson(result, ResultWrapper.class);
        if(resultObject.status.equalsIgnoreCase("OK")) {
          onsuccessCallBack.onSuccess(result);
        } else {
          Toast.makeText(context.getApplicationContext(), "FAILED uploading file", Toast.LENGTH_LONG).show();
        }
      }

      @Override
      public void onErrorUnauthorized() {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Unauthorized request")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onfailCallBack.onFail("Unauthorized");
                  }
                })
                .show();
      }

      @Override
      public void onErrorGeneralRequest(final int statusCode) {
        new AlertDialog.Builder(context)
                .setTitle("Error: " + statusCode)
                .setMessage("Error sending request")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onfailCallBack.onFail("Error: " + statusCode);
                  }
                })
                .show();
      }

      @Override
      public void onNoNetworkAvailable() {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("No internet connection available")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onfailCallBack.onFail("Network Unavailable");
                  }
                })
                .show();
      }

      @Override
      public void onExceptionThrown(final Exception e) {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(e.getLocalizedMessage())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                    onfailCallBack.onFail(e.getLocalizedMessage());
                  }
                })
                .show();
      }
    }){
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();

        String actionTypeMsg;
        switch(actionType) {
          case RemoveConnection: actionTypeMsg = "Removing"; break;
          default: actionTypeMsg = "Uploading";
        }

        dialog = new ProgressDialog(context);
        dialog.setTitle(actionTypeMsg + " file");
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
    task.execute(requestUrl.toString());
  }
}
