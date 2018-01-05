package com.ute.mobi;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import com.ute.mobi.activities.SessionActivity;
import com.ute.mobi.activities.SessionConnectExperimentActivity;
import com.ute.mobi.activities.SessionInfoListForUpload;
import com.ute.mobi.activities.SettingsActivity;
import com.ute.mobi.activities.tasks.UploadSessionFile;
import com.ute.mobi.models.UteSessionRecording;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.ServerSettingsService;
import com.ute.mobi.services.UteCurrentSessionDBService;
import com.ute.mobi.services.UteSessionDBService;
import com.ute.mobi.utilities.AndroidLogger;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;
import com.ute.mobi.utilities.NetworksUtilities;
import com.ute.mobi.utilities.TransformerUtilities;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

  private static final int ACTIVITY_RESULT_CODE_FOR_SESSION_SERVICE = 1;
  private static final int ACTIVITY_RESULT_CODE_FOR_CONNECT_EXPERIMENT = 2;

  private final static int PERMISSION_LOCATION_SERVICE = 101;

  @BindView(R.id.btn_start)
  AppCompatButton startSessionButton;

  @BindView(R.id.btn_connect)
  AppCompatButton connectSessionButton;

  private ServerSettingsService settingsService;
  private AppStateService appStateService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // init local variables
    this.settingsService = new ServerSettingsService(this);
    this.appStateService = AppStateService.getInstance();

    this.setupUI();

    this.setupDefaultServerAddressIfNecessray();

    this.requestDeviceIdIfNecessary();

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION  },
              PERMISSION_LOCATION_SERVICE );
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_LOCATION_SERVICE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted, yay! Do the
          // contacts-related task you need to do.
        } else {
          // permission denied, boo! Disable the
          // functionality that depends on this permission.
        }
        return;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }
  }

  private void setupUI() {
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        //Snackbar.make(view, "Navigating to list to upload saved files", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show();

        List<UteSessionRecording> trackedSessionRecords = appStateService.getSessionRecords();
        File parentDir = new File(appStateService.getSessionDatabasesLocationFolder());
        File[] files = parentDir.listFiles();
        int counter = 0;
        if(files != null) {

          for (File file : files) {
            if(file.getName().endsWith(TransformerUtilities.FILE_EXTENSION_SQLITE)){
              boolean exists = false;
              String filenameWithoutExt = file.getName().replace(TransformerUtilities.FILE_EXTENSION_SQLITE, "");
              if(trackedSessionRecords != null && trackedSessionRecords.size() > 0) {
                for(int i = 0; i < trackedSessionRecords.size(); i++) {
                  UteSessionRecording record = trackedSessionRecords.get(i);
                  if(record.unique_id.equalsIgnoreCase(filenameWithoutExt)) {
                    exists = true;
                    counter++;
                    break;
                  }
                }
              }

              if(exists == false) {
                file.delete();
              }
            }
          }
        }

        if(counter > 0) {
          if(NetworksUtilities.isOnWifi(MainActivity.this)) {
            // navigate
            Intent intent = new Intent(MainActivity.this, SessionInfoListForUpload.class);
            startActivity(intent);
            overridePendingTransition(R.anim.up_in, R.anim.up_out);
          } else {
            Snackbar.make(view, "To upload recorded session files, please connect through WiFi connection", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
          }
        } else {
          Snackbar.make(view, "No session file to upload", Snackbar.LENGTH_LONG)
                  .setAction("Action", null).show();
        }
      }
    });

    // inject all views into properties of this activity.
    ButterKnife.bind(this);

    getSupportActionBar().setTitle("Mobi-UTE");

    //ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{0xffffcc00});
    //this.startSessionButton.setSupportBackgroundTintList(csl);
    //this.connectSessionButton.setSupportBackgroundTintList(csl);

    this.startSessionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // start session view.

        // start new session
        // check if has session id
        if(appStateService.getDeviceId() != null) {
          Intent i = new Intent(MainActivity.this, SessionConnectExperimentActivity.class);
          i.putExtra(SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_ESTABLISHSESSION, SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_CREATESESSION);
          MainActivity.this.startActivityForResult(i, ACTIVITY_RESULT_CODE_FOR_CONNECT_EXPERIMENT);
          //createNewSession();
        } else{
          Toast.makeText(getApplicationContext(), "No ID found for this device, please contact administrator", Toast.LENGTH_LONG).show();
        }
      }
    });

    this.connectSessionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // set disable.
        view.setEnabled(false);

        // navigate to activity to display session list.
        Intent i = new Intent(MainActivity.this, SessionConnectExperimentActivity.class);
        i.putExtra(SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_ESTABLISHSESSION, SessionConnectExperimentActivity.INTENT_EXTRA_ACTION_CONNECTSESSION);
        MainActivity.this.startActivityForResult(i, ACTIVITY_RESULT_CODE_FOR_CONNECT_EXPERIMENT);

        //Intent i = new Intent(MainActivity.this, SessionLabelingActivity.class);
        //MainActivity.this.startActivity(i);

        // after connect and get the result then start survey session.
        // store the session id in the shared preferences.
        // create DB on the fly.
        // do survey action tracking.
        // submit and finish the task.
      }
    });

    String cachedUniqueId = this.appStateService.getCachedUniqueId();
    String cachedSessionId = this.appStateService.getCachedSessionId();
    String cachedExperimentId = this.appStateService.getCachedExperimentId();
    //String cachedExperimentAlias = this.appStateService.getCachedExperimentId();
    int cachedRole = this.appStateService.getCachedRole();
    boolean cachedIsInitiator = this.appStateService.getCachedIsDeviceInitiator();
    if(cachedSessionId != null && cachedExperimentId != null) {
      // start session view.
      Toast.makeText(getApplicationContext(), "Resuming last session", Toast.LENGTH_LONG).show();
      Intent i = new Intent(MainActivity.this, SessionActivity.class);
      i.putExtra(SessionActivity.EXTRA_UNIQUE_ID, cachedUniqueId);
      i.putExtra(SessionActivity.EXTRA_SESSION_ID, cachedSessionId);
      i.putExtra(SessionActivity.EXTRA_EXPERIMENT_ID, cachedExperimentId);
      i.putExtra(SessionActivity.EXTRA_ROLE, cachedRole);
      i.putExtra(SessionActivity.EXTRA_IS_INITIATOR, cachedIsInitiator);
      i.putExtra(SessionActivity.EXTRA_SESSION_MODE, SessionActivity.SessionMode.START);
      i.putExtra(SessionActivity.EXTRA_IS_RESUMING, true);
      MainActivity.this.startActivityForResult(i, ACTIVITY_RESULT_CODE_FOR_SESSION_SERVICE);
    }
  }

  private void setupDefaultServerAddressIfNecessray() {
    if(this.appStateService.getServerAddress() == null) {
      this.appStateService.setServerAddress(this.settingsService.getServerBaseUrl());
    }
  }

  private void requestDeviceIdIfNecessary() {
    if(this.appStateService.getDeviceId() == null) {
      appStateService.setDeviceId(UUID.randomUUID().toString().toUpperCase());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      Intent i = new Intent(this, SettingsActivity.class);
      startActivityForResult(i, 0);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch(requestCode) {
      case ACTIVITY_RESULT_CODE_FOR_CONNECT_EXPERIMENT:
        if(resultCode == RESULT_CANCELED)
        {
          this.connectSessionButton.setEnabled(true);
          return;
        }

        String uniqueId = data.getStringExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_UNIQUE_ID);
        String sessionId = data.getStringExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_SESSION_ID);
        String experimentId = data.getStringExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ID);
        String experimentAlias = data.getStringExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ALIAS);
        int role = data.getIntExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_ROLE, 0);
        boolean isInitiator = data.getBooleanExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_IS_INITIATOR, false);
        double sessionServerStartTime = data.getDoubleExtra(SessionConnectExperimentActivity.INTENT_RESULT_ACTION_CONNECTED_SRV_START_TIME, 0);

        switch(resultCode) {
          case SessionConnectExperimentActivity.ACTIVITY_RESULT_SESSIONCONNECTED:
            if(sessionId != null) {
              // start session view.
              Intent i = new Intent(MainActivity.this, SessionActivity.class);
              i.putExtra(SessionActivity.EXTRA_UNIQUE_ID, uniqueId);
              i.putExtra(SessionActivity.EXTRA_SESSION_ID, sessionId);
              i.putExtra(SessionActivity.EXTRA_EXPERIMENT_ID, experimentId);
              i.putExtra(SessionActivity.EXTRA_EXPERIMENT_ALIAS, experimentAlias);
              i.putExtra(SessionActivity.EXTRA_ROLE, role);
              i.putExtra(SessionActivity.EXTRA_IS_INITIATOR, isInitiator);
              i.putExtra(SessionActivity.EXTRA_SESSION_MODE, SessionActivity.SessionMode.CONNECT);
              MainActivity.this.startActivityForResult(i, ACTIVITY_RESULT_CODE_FOR_SESSION_SERVICE);
            }
            break;
          case SessionConnectExperimentActivity.ACTIVITY_RESULT_SESSIONCREATED:
            if(sessionId != null) {
              // start session view.
              Intent i = new Intent(MainActivity.this, SessionActivity.class);
              i.putExtra(SessionActivity.EXTRA_UNIQUE_ID, uniqueId);
              i.putExtra(SessionActivity.EXTRA_SESSION_ID, sessionId);
              i.putExtra(SessionActivity.EXTRA_EXPERIMENT_ID, experimentId);
              i.putExtra(SessionActivity.EXTRA_EXPERIMENT_ALIAS, experimentAlias);
              i.putExtra(SessionActivity.EXTRA_ROLE, role);
              i.putExtra(SessionActivity.EXTRA_IS_INITIATOR, isInitiator);
              i.putExtra(SessionActivity.EXTRA_SESSION_MODE, SessionActivity.SessionMode.START);
              MainActivity.this.startActivityForResult(i, ACTIVITY_RESULT_CODE_FOR_SESSION_SERVICE);
              AndroidLogger.w("StartActivity", "SessionActivity intent");
            }
            break;
        }

        this.connectSessionButton.setEnabled(true);
        break;
      case ACTIVITY_RESULT_CODE_FOR_SESSION_SERVICE:
        switch (resultCode) {
          case RESULT_OK:
            Toast.makeText(getApplicationContext(), "Session recording has finished. ", Toast.LENGTH_LONG).show();
            break;
        }
        break;
      default: break;
    }
  }
}
