package com.ute.mobi.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;
import com.ute.mobi.R;
import com.ute.mobi.activities.tasks.UploadSessionFile;
import com.ute.mobi.models.UteSessionRecording;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.SensorInfosUploadService;
import com.ute.mobi.services.ServerSettingsService;
import com.ute.mobi.services.UteCurrentSessionDBService;
import com.ute.mobi.services.UteSessionDBService;
import com.ute.mobi.utilities.AndroidLogger;
import com.ute.mobi.utilities.GeneralCallBackActions;
import com.ute.mobi.utilities.HttpAsyncTask;
import com.ute.mobi.utilities.HttpAsyncTaskCallbacks;
import com.ute.mobi.utilities.NetworksUtilities;
import com.ute.mobi.utilities.TransformerUtilities;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SessionInfoListForUpload extends AppCompatActivity {

  private ServerSettingsService settingsService;
  private AppStateService appStateService;

  private List<UteSessionRecording> sessionRecordings;
  private List<String> recordingList;
  private ArrayAdapter<String> adp;

  @BindView(R.id.sensor_info_list)
  SwipeMenuListView sensorInfoList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.settingsService = new ServerSettingsService(this);
    this.appStateService = AppStateService.getInstance();

    setupUI();
  }

  @Override
  public void onBackPressed() {
    finishCurrActv();
  }

  private void finishCurrActv() {
    this.finish();
    // Use exiting animations specified by the parent activity if given
    // Translate left if not specified.
    overridePendingTransition(R.anim.down_in, R.anim.down_out);
  }

  private void setupUI() {
    setContentView(R.layout.activity_session_info_list_for_upload);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportActionBar().setTitle("Session List: Swipe left for options");

    ButterKnife.bind(this);

    SwipeMenuCreator creator = new SwipeMenuCreator() {

      @Override
      public void create(SwipeMenu menu) {
        // create "open" item
        SwipeMenuItem uploadItem = new SwipeMenuItem(
                getApplicationContext());
        // set item background
        //openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                //0xCE)));
        uploadItem.setBackground(new ColorDrawable(0xFF4285F4));
        // set item width
        uploadItem.setWidth(TransformerUtilities.dp2px(getApplicationContext(), 90));
        // set item title
        uploadItem.setTitle("Upload");
        // set item title fontsize
        uploadItem.setTitleSize(18);
        // set item title font color
        uploadItem.setTitleColor(Color.WHITE);
        // add to menu
        menu.addMenuItem(uploadItem);

        // create "delete" item
        SwipeMenuItem deleteItem = new SwipeMenuItem(
                getApplicationContext());
        // set item background
        deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                0x3F, 0x25)));
        // set item width
        deleteItem.setWidth(TransformerUtilities.dp2px(getApplicationContext(), 90));
        // set a icon
        deleteItem.setIcon(android.R.drawable.ic_menu_delete);
        // add to menu
        menu.addMenuItem(deleteItem);
      }
    };

    // set creator
    sensorInfoList.setMenuCreator(creator);

    sensorInfoList.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
        final UteSessionRecording recording = sessionRecordings.get(position);
        if(recording == null) {
          return false;
        }

        switch (index) {
          case 0:
            // upload
            if(NetworksUtilities.isOnWifi(SessionInfoListForUpload.this)) {
              sensorInfoList.setEnabled(false);

              String filename = UteCurrentSessionDBService.getDatabaseFileName(recording.unique_id) + TransformerUtilities.FILE_EXTENSION_SQLITE;
              final UteSessionDBService sessionInstance = new UteSessionDBService(getApplicationContext(), filename, recording.session_id, recording.experiment_id);
              SensorInfosUploadService uploadService = new SensorInfosUploadService(
                      SessionInfoListForUpload.this,
                      settingsService,
                      appStateService,
                      sessionInstance,
                      recording.unique_id,
                      recording.session_id,
                      recording.experiment_id,
                      recording.is_initiator);
              sessionInstance.open();
              uploadService.sendSessionData(new SensorInfosUploadService.OnFinishSubmitListener() {
                @Override
                public void executeOnSuccess() {
                  Context currentContext = SessionInfoListForUpload.this;
                  sessionInstance.destroy();
                  appStateService.deleteSessionRecordSynced(recording.unique_id);
                  sensorInfoList.setEnabled(true);
                  refreshFileList("File has been successfully uploaded");
                }

                @Override
                public void executeOnFailure() {
                  sensorInfoList.setEnabled(true);
                  refreshFileList("File has been successfully uploaded"); //"File has been successfully removed"
                  Snackbar.make(SessionInfoListForUpload.this.getWindow().getCurrentFocus(), "Failed in upload process", Snackbar.LENGTH_LONG)
                          .setAction("Action", null).show();
                }
              });

            }
            else {
              Snackbar.make(SessionInfoListForUpload.this.getWindow().getCurrentFocus(), "To upload the file, please connect through WiFi connection", Snackbar.LENGTH_LONG)
                      .setAction("Action", null).show();
            }

            break;
          case 1:
            // remove connection

            String sessionDisplay = recording.session_id;
              if (sessionDisplay != null && sessionDisplay.isEmpty()) {
              sessionDisplay = "Cached Session";
            }

            String message = "Are you sure you want to delete this data?\n";
            message += "Experiment: " + (getDisplayNameForExperiment(recording.experiment_alias, recording.experiment_id)) + "\n";
            message += "Session: " + sessionDisplay + "\n";

            File file = new File(appStateService.getSessionDatabasesLocationFolder(), recording.unique_id + TransformerUtilities.FILE_EXTENSION_SQLITE);
            if(file.exists()) {
              message += "Size: " + size(file) + "\n";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (HH:mm)");

            if(recording.created_at != null) {
              message += "Created on: " + sdf.format(new Date((long) (recording.created_at * 1000)));
            }

            // pop up confirmation box to delete
            new AlertDialog.Builder(SessionInfoListForUpload.this)
                    .setTitle("Delete Session Data")
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        Context currentContext = SessionInfoListForUpload.this;
                        String filename = UteCurrentSessionDBService.getDatabaseFileName(recording.unique_id) + TransformerUtilities.FILE_EXTENSION_SQLITE;
                        UteSessionDBService sessionInstance = new UteSessionDBService(currentContext.getApplicationContext(), filename, null, null);
                        sessionInstance.destroy();
                        appStateService.deleteSessionRecordSynced(recording.unique_id);
                        sessionInstance = null;

                        if(recording.session_id != null && recording.session_id.isEmpty() == false) {
                          SensorInfosUploadService uploadService = new SensorInfosUploadService(
                                  SessionInfoListForUpload.this,
                                  settingsService,
                                  appStateService,
                                  sessionInstance,
                                  recording.unique_id,
                                  recording.session_id,
                                  recording.experiment_id,
                                  recording.is_initiator);
                          uploadService.closeSessionConnection(new SensorInfosUploadService.OnFinishSubmitListener() {
                            @Override
                            public void executeOnSuccess() {
                              refreshFileList("File has been successfully removed");
                            }

                            @Override
                            public void executeOnFailure() {
                              refreshFileList("File has been successfully removed");
                            }
                          });
                        } else {
                          refreshFileList("File has been successfully removed");
                        }
                      }
                    })
                    .setNegativeButton(android.R.string.no, null).show();

            break;
        }
        // false : close the menu; true : not close the menu
        return false;
      }
    });

    // Left
    sensorInfoList.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);

    this.refreshFileList();

    adp = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.recordingList);
    sensorInfoList.setAdapter(adp);
  }

  private void refreshFileList(String msgToDisplay) {
    refreshFileList();
    if(recordingList.size() > 0) {
      adp.notifyDataSetChanged();
      Snackbar.make(SessionInfoListForUpload.this.getWindow().getCurrentFocus(), msgToDisplay, Snackbar.LENGTH_LONG)
              .setAction("Action", null).show();
    }
    else {
      finishCurrActv();
      Toast.makeText(SessionInfoListForUpload.this.getApplicationContext(), msgToDisplay, Toast.LENGTH_SHORT).show();
    }
  }

  private void refreshFileList() {
    if(this.recordingList == null) {
      this.recordingList = new ArrayList<String>();
    } else {
      this.recordingList.clear();
    }

    this.sessionRecordings = appStateService.getSessionRecords();
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy (HH:mm)");

    for(int i = 0; i < this.sessionRecordings.size(); i++) {
      UteSessionRecording entry = sessionRecordings.get(i);
      String message = this.getDisplayNameForExperiment(entry.experiment_alias, entry.experiment_id);
      if(entry.created_at != null) {
        message += "\n[" + sdf.format(new Date((long) (entry.created_at * 1000))) + "]";
      }
      this.recordingList.add(message);
    }

    /*File parentDir = new File(appStateService.getSessionDatabasesLocationFolder());
    AndroidLogger.w("UbiQSenseDBList", "Displaying List:");
    File[] files = parentDir.listFiles();
    if(files != null) {
      for (File file : files) {
        if(file.getName().endsWith(UploadSessionFile.FILE_EXTENSION_SQLITE)){
          long endTime = (long)(UploadSessionFile.ReadEndTimeFromSqliteFile(this, file)*1000);

          this.sensorInfoSqliteList
              .add(file.getName().replace(UploadSessionFile.FILE_EXTENSION_SQLITE, "")
                  + "[Size: " + this.size(file) + "]"
                  + "\n[Ended: "
                  + sdf.format(new Date(endTime))
                  +"]");
          this.sensorInfosSqliteFiles.add(file);
        }
      }
    }*/
  }

  public static long getFolderSize(File f) {
    long size = 0;
    if (f.isDirectory()) {
      for (File file : f.listFiles()) {
        size += getFolderSize(file);
      }
    } else {
      size=f.length();
    }
    return size;
  }

  public String size(File file){
    String value=null;
    long Filesize=getFolderSize(file)/1024;//call function and convert bytes into Kb
    if(Filesize>=1024)
      value=Filesize/1024+" Mb";
    else
      value=Filesize+" Kb";

    return value;
  }

  /*class AppAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return mAppList.size();
    }

    @Override
    public ApplicationInfo getItem(int position) {
      return mAppList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = View.inflate(getApplicationContext(),
                android.R.layout.simple_list_item_1, null);
        new ViewHolder(convertView);
      }
      ViewHolder holder = (ViewHolder) convertView.getTag();
      ApplicationInfo item = getItem(position);
      holder.iv_icon.setImageDrawable(item.loadIcon(getPackageManager()));
      holder.tv_name.setText(item.loadLabel(getPackageManager()));
      holder.iv_icon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Toast.makeText(SessionInfoListForUpload.this, "iv_icon_click", Toast.LENGTH_SHORT).show();
        }
      });
      holder.tv_name.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Toast.makeText(SessionInfoListForUpload.this,"iv_icon_click", Toast.LENGTH_SHORT).show();
        }
      });
      return convertView;
    }

    class ViewHolder {
      ImageView iv_icon;
      TextView tv_name;

      public ViewHolder(View view) {
        iv_icon = (ImageView) view.findViewById(R.id.iv_icon);
        tv_name = (TextView) view.findViewById(R.id.tv_name);
        view.setTag(this);
      }
    }

    @Override
    public boolean getSwipEnableByPosition(int position) {
      if(position % 2 == 0){
        return false;
      }
      return true;
    }
  }*/

  private String getDisplayNameForExperiment(String alias, String experimentId) {
    String display = alias;
    if(display == null || display.isEmpty()) {
      display = experimentId;
    }

    return display;
  }
}
