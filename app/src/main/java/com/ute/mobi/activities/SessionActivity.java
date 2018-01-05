package com.ute.mobi.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ute.mobi.R;
import com.ute.mobi.activities.breceivers.BootReceiver;
import com.ute.mobi.activities.breceivers.StartSessionSensorRecorderSrvReceiver;
import com.ute.mobi.activities.helpers.SessionActivityHelpers;
import com.ute.mobi.activities.services.SessionSensorRecorderSrv;
import com.ute.mobi.activities.tasks.ModSessionActions;
import com.ute.mobi.activities.views.UteIntervalLabelButton;
import com.ute.mobi.models.UteModelIntervalLabels;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.SensorInfosUploadService;
import com.ute.mobi.services.ServerSettingsService;
import com.ute.mobi.services.UteCurrentSessionDBService;
import com.ute.mobi.services.UteSessionDBService;
import com.ute.mobi.settings.SessionRoleSettings;
import com.ute.mobi.settings.SessionSetupSettings;
import com.ute.mobi.utilities.AndroidLogger;
import com.ute.mobi.utilities.GeneralCallBackActions;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;
import com.ute.mobi.utilities.NetworksUtilities;
import com.ute.mobi.utilities.TransformerUtilities;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class SessionActivity extends AppCompatActivity {

  public enum SessionMode {
    START,
    CONNECT,
  }

  public final static String EXTRA_UNIQUE_ID = "com.extra.unique_id";
  public final static String EXTRA_SESSION_ID = "com.extra.session_id";
  public final static String EXTRA_EXPERIMENT_ID = "com.extra.experiment_id";
  public final static String EXTRA_EXPERIMENT_ALIAS = "com.extra.experiment_alias";
  public final static String EXTRA_ROLE = "com.extra.role";
  public final static String EXTRA_IS_INITIATOR = "com.extra.is_initiator";
  public final static String EXTRA_SESSION_MODE = "com.extra.session_mode";
  public final static String EXTRA_IS_RESUMING = "com.extra.resuming";

  public final static String BR_FINISH_ACTIVITY = "UTE.br.SessionActivity.FinishActivity";

  private final static int STREAM_SEND_INFO_INTERVAL_SECS = 2*60;

  private ServerSettingsService settingsService;
  private UteSessionDBService dbService;
  //private SessionSensorService sensorService;
  private AppStateService appStateService;
  private SessionActivityHelpers sessionActivityHelpers;

  private String uniqueId;
  private String sessionId;
  private String experimentId;
  private String experimentAlias;
  private int role;
  private boolean isDeviceInitiator;
  private SessionMode mode;

  private boolean isRunning;
  private boolean isStopped;
  private boolean isSendingSensorInfo;

  List<List<UteIntervalLabelButton>> allButtonlist;

  @Nullable
  @BindView(R.id.btn_stop)
  Button stopButton;

  @Nullable
  @BindView(R.id.btn_start_labeling)
  Button startLabelingButton;

  @Nullable
  @BindView(R.id.experimentid_display)
  TextView experimentIdTextView;

  @Nullable
  @BindView(R.id.sessionid_display)
  TextView sessionIdTextView;

  @Nullable
  @BindView(R.id.btn_finish_labeling)
  Button finishLabelingButton;

  @Nullable
  @BindView(R.id.btn_clear_labels)
  Button clearAllLabelsButton;

  @Nullable
  @BindView(R.id.ll_labels)
  LinearLayout scrollViewContainer;

  /*@InjectView(R.id.textViewLatitude)
  TextView textViewLatitude;

  @InjectView(R.id.textViewLongitude)
  TextView textViewLongitude;*/

  private final BroadcastReceiver finishSessionNotify = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      onSessionFinishNavigate();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    Intent intent = getIntent();

    AndroidLogger.w("SessionActivity", "SessionID check");
    this.uniqueId = intent.getStringExtra(EXTRA_UNIQUE_ID);
    if (this.uniqueId == null || this.uniqueId.isEmpty()) {
      this.finish();
      return;
    }

    this.sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
    if (this.sessionId == null) {
      this.finish();
      return;
    }

    this.experimentId = intent.getStringExtra(EXTRA_EXPERIMENT_ID);
    if (this.experimentId == null || this.experimentId.isEmpty()) {
      this.finish();
      return;
    }

    this.experimentAlias = intent.getStringExtra(EXTRA_EXPERIMENT_ALIAS);

    this.role = intent.getIntExtra(EXTRA_ROLE, 0);
    if(role == 0) {
      this.finish();
      return;
    }

    this.isDeviceInitiator = intent.getBooleanExtra(EXTRA_IS_INITIATOR, false);

    AndroidLogger.w("SessionActivity", "SessionMode check");
    this.mode = (SessionMode) intent.getSerializableExtra(EXTRA_SESSION_MODE);
    if (this.mode == null) {
      this.finish();
      return;
    }

    // init local variables
    this.settingsService = new ServerSettingsService(this);
    this.appStateService = AppStateService.getInstance();
    this.sessionActivityHelpers = new SessionActivityHelpers(this, this.settingsService, this.appStateService);
    this.dbService = UteCurrentSessionDBService.getSessionInstance(this);
    this.appStateService.isOnSessionActivity = true;

    boolean immediatelyStartRecording = intent.getBooleanExtra(EXTRA_IS_RESUMING, false);
    this.resumeSensorReadings();
    if (immediatelyStartRecording) {
      logicForLabelChanges(null);
    }

    if(role == SessionRoleSettings.ROLE_SENSING) {
      //this.appStateService.setScheduledSensorReadingPendingIntent(SessionActivity.scheduleSensorReadingService(this));

      //this.scheduleNextSendChunkSensorInfo();
      /*if (this.mode == SessionMode.CONNECT) {
        this.scheduleNextSessionIsActiveCheck();
      }*/
    }

    // register broadcast receiver
    this.registerBroadcastReceivers();

    // prevent device on this activity to sleep.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // container for all interval button labels.
    allButtonlist = new ArrayList<List<UteIntervalLabelButton>>();

    this.setupUI(immediatelyStartRecording);
    AndroidLogger.w("SessionActivity", "Finished setup UI");
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.appStateService.isOnSessionActivity = true;
    //this.dbService.open();

    // check if sessionid still exist.
    String cachedSessionId = this.appStateService.getCachedSessionId();
    if(cachedSessionId == null) {
      ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(this);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    this.appStateService.isOnSessionActivity = false;
    //this.dbService.close();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    this.appStateService.isOnSessionActivity = false;
    this.unregisterBroadcastReceivers();
  }

  @Override
  public void onBackPressed() {
    // don't do anything when back button is pressed.
  }

  /*private void scheduleNextSensorReadings() {
    if (!isStopped) {
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          if (isRunning) {
            boolean isInvalidToInsertData = !isRunning || isStopped;
            if (isInvalidToInsertData == false) {
              RTDModelSensorInfo reading = sensorService.readSensors();
              textViewLatitude.setText(String.valueOf(reading.location_latitude));
              textViewLongitude.setText(String.valueOf(reading.location_longitude));
              Log.i("test", "sensor readings:lat["+reading.location_latitude+"] lon["+ reading.location_longitude +"]");
              dbService.insertSensorInfo(reading);
            }
          }

          scheduleNextSensorReadings();
        }
      }, INTERVAL_MILLIS);
    }
  }*/

  public static void scheduleSensorReadingBroadcastReceiver(Context context) {
    Context appContext = context.getApplicationContext();
    AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(appContext, StartSessionSensorRecorderSrvReceiver.class);
    PendingIntent scheduledSensorReadingIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

    //scheduler.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL_MILLIS, INTERVAL_MILLIS, scheduledSensorReadingIntent);

    long smallestMillisec = Long.MAX_VALUE;
    ArrayList<Long> vectorMillisec = new ArrayList<Long>();
    SessionSetupSettings settings = AppStateService.getInstance().getSessionSetupSettings();
    if(settings.sensors != null) {
      for(int j = 0; j < settings.sensors.length; j++) {
        SessionSetupSettings.SettingsSensor sensorSetting = settings.sensors[j];
        if(sensorSetting.freq != null && sensorSetting.freq != 0)
          vectorMillisec.add(TransformerUtilities.convertHzToMillisec(sensorSetting.freq));
        else if(sensorSetting.sec != null && sensorSetting.sec != 0)
          vectorMillisec.add(new Double(sensorSetting.sec * 1000).longValue());
      }
    }

    for(int in = 0; in < vectorMillisec.size(); in++) {
      long val = vectorMillisec.get(in);
      if(val < smallestMillisec)
        smallestMillisec = val;
    }

    //private final static int INTERVAL_MILLIS = 20;
    // convert frequency Hz to Millis
    long INTERVAL_MILLIS = smallestMillisec;

    scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL_MILLIS, scheduledSensorReadingIntent);
  }

  public static void scheduleSensorReadingService(Context context) {
    scheduleSensorReadingBroadcastReceiver(context);

    // enable the receiver component onboot
    ComponentName receiver = new ComponentName(context, BootReceiver.class);
    PackageManager pm = context.getPackageManager();

    pm.setComponentEnabledSetting(receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);

    //return scheduledSensorReadingIntent;
  }

  /*private void scheduleNextSessionIsActiveCheck() {
    if (!isStopped) {
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          if (isRunning) {
            checkIsSessionActive();
          }

          scheduleNextSessionIsActiveCheck();
        }
      }, 60 * 1000); // check session is active every 1 minute.
    }
  }*/

  private void registerBroadcastReceivers() {
    this.registerReceiver(finishSessionNotify, new IntentFilter(BR_FINISH_ACTIVITY));
  }

  private void unregisterBroadcastReceivers() {
    this.unregisterReceiver(this.finishSessionNotify);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private void setupUI(boolean immediatelyStartRecording) {
    ActionBar actionBar = getSupportActionBar();
    if(role == SessionRoleSettings.ROLE_SENSING) {
      setContentView(R.layout.activity_session_inprogress);

      if(actionBar != null)
        actionBar.hide();

      // inject all views into properties of this activity.
      ButterKnife.bind(this);

      if(SessionSensorRecorderSrv.isRecordingRunning) {
        onSessionRecordingStartedAction();
      } else {
        this.sessionActivityHelpers.setButtonGenericStates(this.stopButton);
        this.stopButton.setText("Start Recording");
        this.stopButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onSessionRecordingStartedAction();
            startLabelingButton.setVisibility(View.VISIBLE);
          }
        });

        if(immediatelyStartRecording) {
          onSessionRecordingStartedAction();
          startLabelingButton.setVisibility(View.VISIBLE);
        }

        startLabelingButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            new AlertDialog.Builder(SessionActivity.this)
                    .setTitle("Start labeling")
                    .setMessage("Do you want to start labeling for this session recording?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        role = SessionRoleSettings.ROLE_LABELING;
                        setupUI(false);
                      }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
          }
        });
      }

      this.experimentIdTextView.setText("Experiment ID: \n" + this.getDisplayNameForExperiment(this.experimentAlias, this.experimentId));
      this.sessionIdTextView.setText("Session ID: \n" + (this.sessionId.equalsIgnoreCase("") ? "Cached Session" : this.sessionId));
    } else if (role == SessionRoleSettings.ROLE_LABELING) {
      setContentView(R.layout.activity_session_labeling_inprogress);

      // inject all views into properties of this activity.
      ButterKnife.bind(this);

      if(actionBar != null)
        actionBar.setTitle("Mobi-UTE: Labeling - " + (sessionId.isEmpty() ? "Cached Session" : sessionId));

      this.finishLabelingButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          confirmFinishLabelingSession();
        }
      });

      this.clearAllLabelsButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          clearAllLabels();
        }
      });

      this.intervalLabelsCurrentLabel = this.appStateService.getCachedSessionIntervalLabelsCurrentLabel();
      this.intervalLabelsStartDate = this.appStateService.getCachedSessionIntervalLabelsCurrentStartTime();
      SessionSetupSettings setupSettings = this.appStateService.getSessionSetupSettings();
      if(setupSettings != null && setupSettings.label != null) {
        if("interval".equals(setupSettings.label.type)) {
          if(setupSettings.label.schema.isEmpty() == false) {
            // currently only support 1 label set.
            boolean onlyonelabelset = setupSettings.label.schema.size() == 1;
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
            int MIN_WIDTH_LABEL_GROUP = 300;
            int numbofmaxcols = width / MIN_WIDTH_LABEL_GROUP;
            int totalcountlabelsets = setupSettings.label.schema.size();
            boolean stretch = totalcountlabelsets < numbofmaxcols;
            int numbofrows = totalcountlabelsets / numbofmaxcols;
            if(totalcountlabelsets%numbofmaxcols != 0) {
              numbofrows++;
            }

            List<LinearLayout> collectionContainers = new ArrayList<>();
            for(int iilr = 0; iilr < numbofrows; iilr++) {
              LinearLayout labelsetgroupContainer = new LinearLayout(this);
              labelsetgroupContainer.setOrientation(LinearLayout.HORIZONTAL);
              labelsetgroupContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
              collectionContainers.add(labelsetgroupContainer);
              scrollViewContainer.addView(labelsetgroupContainer);
            }

            for(int iil = 0; iil < setupSettings.label.schema.size(); iil++) {
              SessionSetupSettings.SettingsLabelInterval labelIntervalSchema = setupSettings.label.schema.get(iil);
              // get list of labels in the set
              ArrayList<String> labels = labelIntervalSchema.set;

              LinearLayout labelsetgroup = new LinearLayout(this);
              labelsetgroup.setOrientation(LinearLayout.VERTICAL);
              labelsetgroup.setPadding(
                      this.settingsService.dpToPixels(10),
                      this.settingsService.dpToPixels(10),
                      this.settingsService.dpToPixels(10),
                      this.settingsService.dpToPixels(10)
              );

              int rownum = iil / numbofmaxcols;
              collectionContainers.get(rownum).addView(labelsetgroup);

              TextView grouplabel = new TextView(this);
              grouplabel.setText(labelIntervalSchema.set_name);
              grouplabel.setTypeface(null, Typeface.BOLD_ITALIC);
              grouplabel.setPadding(3,3,3,3);
              grouplabel.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
              grouplabel.setMaxLines(2);
              labelsetgroup.addView(grouplabel);
              LinearLayout groupViewOfLabels = new LinearLayout(this);
              if(onlyonelabelset) {
                //create group view.
                labelsetgroup.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                groupViewOfLabels.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                groupViewOfLabels.setOrientation(LinearLayout.VERTICAL);
                groupViewOfLabels.setGravity(Gravity.CENTER);
                groupViewOfLabels.setPadding(
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5)
                );
                groupViewOfLabels.setBackgroundColor(Color.parseColor("#ECEFF1"));
                labelsetgroup.addView(groupViewOfLabels);
              } else {
                labelsetgroup.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                int eqwidth = 0;
                if(stretch) {
                  eqwidth = width / setupSettings.label.schema.size();
                } else {
                  eqwidth = MIN_WIDTH_LABEL_GROUP;
                }

                //create group view.
                ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.width = eqwidth;
                groupViewOfLabels.setLayoutParams(params);
                groupViewOfLabels.setOrientation(LinearLayout.VERTICAL);
                groupViewOfLabels.setGravity(Gravity.CENTER);
                groupViewOfLabels.setPadding(
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5),
                        this.settingsService.dpToPixels(5)
                );
                groupViewOfLabels.setBackgroundColor(Color.parseColor("#ECEFF1"));
                labelsetgroup.addView(groupViewOfLabels);
              }

              final List<UteIntervalLabelButton> buttonlist = new ArrayList<UteIntervalLabelButton>();
              allButtonlist.add(buttonlist);

              for(int i = 0; i < labels.size(); i++) {
                final UteIntervalLabelButton newButton = new UteIntervalLabelButton(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, this.settingsService.dpToPixels(10));
                lp.setLayoutDirection(Gravity.CENTER);
                newButton.setLayoutParams(lp);
                newButton.setMinimumWidth(this.settingsService.dpToPixels(90));
                newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                newButton.setPadding(this.settingsService.dipToPixels(10), 0, this.settingsService.dipToPixels(10), 0);

                // check is activated or not
                newButton.setText(labels.get(i));
                newButton.setTransformationMethod(null);
                sessionActivityHelpers.setButtonGenericColor(newButton);
                buttonlist.add(newButton);

                newButton.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    UteIntervalLabelButton otherActivatedButton = sessionActivityHelpers.getAnyButtonInGroupIsActivated(buttonlist, newButton);
                    if(otherActivatedButton != null) {
                      sessionActivityHelpers.triggerIntervalLabelButtonActivation(otherActivatedButton);
                    }

                    sessionActivityHelpers.triggerIntervalLabelButtonActivation(newButton);

                    // trigger label logic.
                    logicForLabelChanges(getActiveLabelsFromAllLabellist(allButtonlist));
                  }
                });

                // if match with current label #ONLY SUPPORT one label
                if(newButton.getText().toString().equals(this.intervalLabelsCurrentLabel)) {
                  sessionActivityHelpers.triggerIntervalLabelButtonActivation(newButton);
                }

                groupViewOfLabels.addView(newButton);
              }
            }
          }
        }
      }
    }
  }

  private void clearAllLabels() {
    for(int i = 0; i < allButtonlist.size(); i++) {
      List<UteIntervalLabelButton> buttonlist = allButtonlist.get(i);
      for(int j = 0; j < buttonlist.size(); j++) {
        UteIntervalLabelButton intervalLabelButton = buttonlist.get(j);
        if(intervalLabelButton.isIntervalLabelActivated()) {
          sessionActivityHelpers.triggerIntervalLabelButtonActivation(intervalLabelButton);
        }
      }
    }
    logicForLabelChanges(null);
  }

  private String getActiveLabelsFromAllLabellist(List<List<UteIntervalLabelButton>> allButtonlist) {
    String activeLabelsFinal = null;
    List<String> arractvlbls = new ArrayList<String>();
    for(int i = 0; i < allButtonlist.size(); i++) {
      List<UteIntervalLabelButton> buttonlist = allButtonlist.get(i);
      arractvlbls.add(sessionActivityHelpers.getActiveLabels(buttonlist));
    }

    boolean isallnull = true;
    for(int i = 0; i < arractvlbls.size(); i++) {
      if(arractvlbls.get(i) != null) {
        isallnull = false;
        break;
      }
    }

    if(isallnull) {
      return null;
    }

    for(int i = 0; i < arractvlbls.size(); i++) {
      if(i == 0) {
        activeLabelsFinal = arractvlbls.get(i);
        if(activeLabelsFinal == null) {
          activeLabelsFinal = "none";
        }
      } else {
        activeLabelsFinal += ":";
        String toadd = arractvlbls.get(i);
        if(toadd == null) {
          toadd = "none";
        }
        activeLabelsFinal += toadd;
      }
    }

    return activeLabelsFinal;
  }

  private void onSessionRecordingStartedAction() {
    SessionActivity.scheduleSensorReadingService(this);
    this.sessionActivityHelpers.setButtonAlertStates(this.stopButton);
    this.stopButton.setText("Finish Recording");
    this.stopButton.setOnClickListener(null);
    this.stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        confirmFinishSession();
      }
    });
  }

  String intervalLabelsCurrentLabel;
  double intervalLabelsStartDate;

  private void logicForLabelChanges(String label) {
    if(intervalLabelsCurrentLabel == label) {
      return;
    } else if(intervalLabelsCurrentLabel != null && intervalLabelsCurrentLabel.equals(label)) {
      return;
    }

    if(intervalLabelsCurrentLabel != label && intervalLabelsCurrentLabel != null && intervalLabelsCurrentLabel.isEmpty() == false && intervalLabelsStartDate != 0) {
      // end current labeling and start with new labeling
      UteModelIntervalLabels intervalLabels = new UteModelIntervalLabels();
      intervalLabels.start_date = intervalLabelsStartDate;
      intervalLabels.end_date = this.appStateService.getSynchronizedCurrentTime();
      intervalLabels.labels = intervalLabelsCurrentLabel;
      this.dbService.insertSensorIntervalLabel(intervalLabels);
      this.appStateService.clearCachedSessionIntervalLabels();
    }

    intervalLabelsCurrentLabel = label;

    if(label == null || label.isEmpty()) {
      intervalLabelsStartDate = 0;
    } else {
      intervalLabelsStartDate = this.appStateService.getSynchronizedCurrentTime();
      this.appStateService.setCachedSessionIntervalLabelsCurrentLabel(label);
      this.appStateService.setCachedSessionIntervalLabelsCurrentStartDate(intervalLabelsStartDate);
    }
  }

  private void confirmFinishSession() {
    new AlertDialog.Builder(SessionActivity.this)
            .setTitle("Finish Session")
            .setMessage("Are you sure you want to finish this session?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                SessionActivity.this.stopButton.setEnabled(false);
                SessionActivity.this.pauseSensorReadings();
                //SessionActivity.this.finishSessionV2();
                finishSessionAskForUpload();
              }
            })
            .setNegativeButton(android.R.string.no, null).show();
  }

  private void confirmFinishLabelingSession() {
    new AlertDialog.Builder(SessionActivity.this)
            .setTitle("Finish Session")
            .setMessage("Are you sure you want to finish this session?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                SessionActivity.this.finishLabelingButton.setEnabled(false);
                SessionActivity.this.pauseSensorReadings();

                clearAllLabels();
                finishSessionAskForUpload();
              }
            })
            .setNegativeButton(android.R.string.no, null).show();
  }

  /*private void scheduleNextSendChunkSensorInfo() {
    if (!isStopped) {
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          scheduleNextSendChunkSensorInfo();
          sendChunkSensorInfos();
        }
      }, STREAM_SEND_INFO_INTERVAL_SECS * 1000);
    }
  }*/

  /**
   * This method to send chunk stream of sensor infos is only being triggered by the scheduler.
   */
  /*private void sendChunkSensorInfos() {
    // prevent send chunk sensors when it is paused.
    if(this.isRunning == false) {
      return;
    }

    if(this.isSendingSensorInfo) {
      return;
    }

    this.sendChunkSensorInfosGeneric(false);
  }*/

  /*private void toastLatestMessageForLonLat() {
    final String message = this.dbService.getLatestMessage();

    if(message != null) {
      Log.w("test", message);
    }
    else {
      Log.w("test", "NO LAT LON DETECTED");
    }

    new Handler().post(new Runnable() {
      @Override
      public void run() {
        if(message != null) {
          Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
        else {
          Toast.makeText(getApplicationContext(), "NO LAT LON DETECTED", Toast.LENGTH_LONG).show();
        }
      }
    });
  }*/

  private void onSessionFinishDisablesAll() {
    isStopped = true;

    ModSessionActions.FinishSession(this, this.appStateService);
  }

  private void onSessionFinishNavigate() {
    Intent returnIntent = new Intent();
    SessionActivity.this.setResult(Activity.RESULT_OK, returnIntent);

    SessionActivity.this.finish();
  }

  private void finishSession(final boolean needUserFeedback) {
    SessionActivity.this.pauseSensorReadings();
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "session/service/sensor/submit/v2");
    } catch (MalformedURLException e) {
      return;
    }

    String encodedDbFile = null;
    try {
      encodedDbFile = this.dbService.get64EncodedDbFile();
    } catch (IOException e) {
      Log.e("Error", e.getLocalizedMessage());
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", settingsService.getDeviceId());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("sessionId", this.sessionId);
    jsonObject.addProperty("sensorsInfosBase64", encodedDbFile);

    HttpAsyncTask task = new HttpAsyncTask(this, HttpAsyncTask.Method.POST, new HashMap<String, Object>() {{
      put("content", jsonObject.toString());
    }}, HttpAsyncTask.ContentType.JSON, new HttpAsyncTaskCallbacks() {

      @Override
      public void onSuccess(String result) {
        onSessionFinishDisablesAll();
        ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(getApplicationContext());
      }

      @Override
      public void onErrorUnauthorized() {
        if (needUserFeedback) {
          new AlertDialog.Builder(SessionActivity.this)
                  .setTitle("Error")
                  .setMessage("Unauthorized request")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        SessionActivity.this.resumeSensorReadings();
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if (needUserFeedback) {
          new AlertDialog.Builder(SessionActivity.this)
                  .setTitle("Error")
                  .setMessage("Error sending request")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        SessionActivity.this.resumeSensorReadings();
      }

      @Override
      public void onNoNetworkAvailable() {
        if (needUserFeedback) {
          new AlertDialog.Builder(SessionActivity.this)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        SessionActivity.this.resumeSensorReadings();
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if (needUserFeedback) {
          new AlertDialog.Builder(SessionActivity.this)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }

        SessionActivity.this.resumeSensorReadings();
      }
    }) {
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(SessionActivity.this);
        dialog.setTitle("Finishing Session.");
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
    task.setCustomTimeout(15 * 60 * 1000, 15 * 60 * 1000);
    task.execute(requestUrl.toString());
  }

  private void finishSessionAskForUpload() {
    final String sessionId = this.sessionId;
    final String uniqueId = this.uniqueId;
    onSessionFinishDisablesAll();

    String contentToUploadTxt = role == SessionRoleSettings.ROLE_SENSING ? "records" : "labels";

    // check if device on wifi
    if(NetworksUtilities.isOnWifi(this)) {
      new AlertDialog.Builder(SessionActivity.this)
              .setTitle("Session Finished")
              .setMessage("Do you want to upload the sensor " + contentToUploadTxt + " of this session?")
              .setIcon(android.R.drawable.ic_dialog_alert)
              .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  String sessionFileName = UteCurrentSessionDBService.getDatabaseFileName(uniqueId) + TransformerUtilities.FILE_EXTENSION_SQLITE;
                  final UteSessionDBService sessionInstance = new UteSessionDBService(getApplicationContext(), sessionFileName, sessionId, experimentId);

                  SensorInfosUploadService uploadService = new SensorInfosUploadService(
                          SessionActivity.this,
                          settingsService,
                          appStateService,
                          sessionInstance,
                          uniqueId,
                          sessionId,
                          experimentId,
                          isDeviceInitiator);
                  sessionInstance.open();
                  uploadService.sendSessionData(new SensorInfosUploadService.OnFinishSubmitListener() {
                    @Override
                    public void executeOnSuccess() {
                      isStopped = true;
                      appStateService.setCurrentSessionIsStopping(true);

                      // clear cached session id.
                      appStateService.clearCachedSessionPreferences();

                      stopSensorReadingService();

                      sessionInstance.destroy();
                      AppStateService.getInstance().deleteSessionRecordSynced(uniqueId);
                      new AlertDialog.Builder(SessionActivity.this)
                                     .setTitle("Data Submission")
                                     .setMessage("Sensor data have been submitted successfully. ")
                                     .setIcon(android.R.drawable.ic_dialog_alert)
                                     .setCancelable(false)
                                     .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                         ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(getApplicationContext());
                                       }
                                     })
                                     .show();
                    }

                    @Override
                    public void executeOnFailure() {
                      isStopped = true;
                      appStateService.setCurrentSessionIsStopping(true);

                      // clear cached session id.
                      appStateService.clearCachedSessionPreferences();

                      stopSensorReadingService();

                      ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(getApplicationContext());
                    }
                  });
                }
              })
              .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                  isStopped = true;
                  appStateService.setCurrentSessionIsStopping(true);

                  // clear cached session id.
                  appStateService.clearCachedSessionPreferences();

                  stopSensorReadingService();

                  new AlertDialog.Builder(SessionActivity.this)
                          .setTitle("Session finished")
                          .setMessage("Please upload your session data once you are connected through WiFi ")
                          .setIcon(android.R.drawable.ic_dialog_alert)
                          .setCancelable(false)
                          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                              ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(getApplicationContext());
                            }
                          })
                          .show();
                }
              }).show();
    }
    else {
      ModSessionActions.SendBroadcastToFinishSessionActivityAndNavigate(this);
    }
  }

  private void pauseSensorReadings() {
    this.isRunning = false;
    this.appStateService.setCurrentSessionIsRunning(false);
  }

  private void resumeSensorReadings() {
    this.isRunning = true;
    this.appStateService.setCurrentSessionIsRunning(true);
  }

  private void stopSensorReadingService() {
    /*AlarmManager alarmservice = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    Intent intent = new Intent(this, StartSessionSensorRecorderSrvReceiver.class);
    PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT);
    alarmservice.cancel(pending);*/

    // no need to cancel
    //((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(this.appStateService.getScheduledSensorReadingPendingIntent());

    Intent service = new Intent(getApplicationContext(), SessionSensorRecorderSrv.class);
    getApplicationContext().stopService(service);
  }

  private String getDisplayNameForExperiment(String alias, String experimentId) {
    String display = alias;
    if(display == null || display.isEmpty()) {
      display = experimentId;
    }

    return display;
  }
}
