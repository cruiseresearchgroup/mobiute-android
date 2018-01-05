package com.ute.mobi.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ute.mobi.R;
import com.ute.mobi.activities.helpers.SessionConnectExperimentActivityHelpers;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jonathanliono on 28/01/2016.
 */
public class SessionConnectExperimentActivity extends AppCompatActivity {

  public final static String INTENT_RESULT_ACTION_CONNECTED_UNIQUE_ID = "com.action.connected.uniqueid";
  public final static String INTENT_RESULT_ACTION_CONNECTED_SESSION_ID = "com.action.connected.sessionid";
  public final static String INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ID = "com.action.connected.experimentid";
  public final static String INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ALIAS = "com.action.connected.experimentalias";
  public final static String INTENT_RESULT_ACTION_CONNECTED_SRV_START_TIME = "com.action.connected.server.time";
  public final static String INTENT_RESULT_ACTION_CONNECTED_ROLE = "com.action.connected.role";
  public final static String INTENT_RESULT_ACTION_CONNECTED_IS_INITIATOR = "com.action.connected.isinitiator";

  public final static String INTENT_EXTRA_ACTION_ESTABLISHSESSION = "com.ute.action.establishsession";
  public final static int INTENT_EXTRA_ACTION_CREATESESSION = 1;
  public final static int INTENT_EXTRA_ACTION_CONNECTSESSION = 2;

  public final static int ACTIVITY_RESULT_SESSIONCREATED = 101;
  public final static int ACTIVITY_RESULT_SESSIONCONNECTED = 102;

  public final static int LIST_DISPLAYMODE_EXPERIMENTS = 1101;
  public final static int LIST_DISPLAYMODE_SESSIONS = 1102;

  @BindView(R.id.swipe_container)
  SwipeRefreshLayout swipeContainer;

  @BindView(R.id.listView_exsessionList)
  SwipeMenuListView listView;

  @BindView(R.id.empty_list_pull_to_refresh)
  TextView emptyLabel;

  private List<SessionConnectExperimentActivityHelpers.ExperimentListItem> connectListItems;
  private List<String> connectListDisplay;

  private int listDisplayMode;
  private ServerSettingsService settingsService;
  private AppStateService appStateService;

  private int activityIntention;

  private String choosenExperimentId;
  private String choosenExperimentAlias;
  private String choosenSessionCode;

  private SessionConnectExperimentActivityHelpers helpers;
  private ExSessAdapter adp;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_experiment_listing_to_connect);

    // init local variables
    this.settingsService = new ServerSettingsService(this);
    this.appStateService = AppStateService.getInstance();
    this.helpers = new SessionConnectExperimentActivityHelpers(this, this.settingsService, this.appStateService);

    Intent intent = getIntent();
    this.activityIntention = intent.getIntExtra(INTENT_EXTRA_ACTION_ESTABLISHSESSION, 0);

    // inject all views into properties of this activity.
    ButterKnife.bind(this);
    this.setupUI();
  }

  private void setupUI() {
    setupSwipeActionUILogic();

    setupUITitleExperiments();

    this.swipeContainer.setEnabled(false);
    this.connectListItems = new ArrayList<SessionConnectExperimentActivityHelpers.ExperimentListItem>();
    this.connectListDisplay = new ArrayList<String>();
    this.adp = new ExSessAdapter(this, android.R.layout.simple_list_item_1, this.connectListDisplay);
    this.listView.setAdapter(adp);

        /*View empty = getLayoutInflater().inflate(R.layout.activity_session_listing_view_when_empty, null, false);
        empty.setBackgroundColor(Color.TRANSPARENT);
        addContentView(empty, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        this.listView.setEmptyView(empty);*/

    this.listDisplayMode = LIST_DISPLAYMODE_EXPERIMENTS;
    setListViewListener();
    helpers.requestLatestExperimentListHttp(); // immediately request latest experiment list.
  }

  class ExSessAdapter extends ArrayAdapter<String> {
    public ExSessAdapter(Context context, int resource, List<String> objects) {
      super(context, resource, objects);
    }

    @Override
    public int getViewTypeCount() {
      // menu type count
      return 4;
    }

    @Override
    public int getItemViewType(int position) {
      // current menu type
      if(SessionConnectExperimentActivity.this.listDisplayMode == LIST_DISPLAYMODE_EXPERIMENTS) {
        SessionConnectExperimentActivityHelpers.ExperimentListItem experiment = SessionConnectExperimentActivity.this.connectListItems.get(position);
        if(experiment.cached) {
          // uncache button is enabled
          return 3;
        } else {
          if(experiment.is_cacheable) {
            // cache button is enabled
            return 1;
          } else {
            // cache button disabled
            return 2;
          }
        }
      }

      // no action button.
      return 0;
    }
  }

  private void setupSwipeActionUILogic() {
    SwipeMenuCreator creator = new SwipeMenuCreator() {

      @Override
      public void create(SwipeMenu menu) {
        // Create different menus depending on the view type
        switch (menu.getViewType()) {
          case 1: CreateSwipeButtonCache(menu, true); break;
          case 2: CreateSwipeButtonCache(menu, false); break;
          case 3: CreateSwipeButtonUncache(menu); break;
          default: break;
        }
      }

      private void CreateSwipeButtonCache(SwipeMenu menu, boolean enabled) {
        // create "cache" item
        SwipeMenuItem cacheItem = new SwipeMenuItem(
                getApplicationContext());
        // set item background
        if(enabled)
          cacheItem.setBackground(new ColorDrawable(0xFF4285F4));
        else
          cacheItem.setBackground(new ColorDrawable(0xFF9E9E9E));

        // set item width
        cacheItem.setWidth(TransformerUtilities.dp2px(getApplicationContext(), 90));
        // set item title
        cacheItem.setTitle("Cache");
        // set item title fontsize
        cacheItem.setTitleSize(18);
        // set item title font color
        cacheItem.setTitleColor(Color.WHITE);
        // add to menu
        menu.addMenuItem(cacheItem);
      }

      private void CreateSwipeButtonUncache(SwipeMenu menu) {
        // create "cache" item
        SwipeMenuItem cacheItem = new SwipeMenuItem(
                getApplicationContext());
        // set item background
        cacheItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                0x3F, 0x25)));

        // set item width
        cacheItem.setWidth(TransformerUtilities.dp2px(getApplicationContext(), 90));
        // set item title
        cacheItem.setTitle("Uncache");
        // set item title fontsize
        cacheItem.setTitleSize(18);
        // set item title font color
        cacheItem.setTitleColor(Color.WHITE);
        // add to menu
        menu.addMenuItem(cacheItem);
      }
    };

    // set creator
    listView.setMenuCreator(creator);

    listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
        if(SessionConnectExperimentActivity.this.listDisplayMode != LIST_DISPLAYMODE_EXPERIMENTS) {
          return false;
        }

        final SessionConnectExperimentActivityHelpers.ExperimentListItem experiment = connectListItems.get(position);
        if(experiment.cached) {
          if(index == 0) {
            // uncache action
            new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                    .setTitle("Uncache - "+ getDisplayNameForExperiment(experiment.talias, experiment.experiment_id))
                    .setMessage("Are you sure you want to uncache this experiment?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        // request role to be specified before connect to session
                        helpers.requestToUncacheExperiment(experiment.experiment_id, experiment.uid);
                      }})
                    .setNegativeButton(android.R.string.no, null).show();
          }
        } else {
          if(experiment.is_cacheable) {
            if(index == 0) {
              // cache action
              AlertDialog.Builder builder = new AlertDialog.Builder(SessionConnectExperimentActivity.this);
              builder.setTitle("Caching Experiment - OTP");
              builder.setMessage("To cache this experiment, please provide the OTP");
              // Set up the input
              final EditText input = new EditText(SessionConnectExperimentActivity.this);
              input.setHint("OTP");
              // Specify the type of input expected; this
              input.setInputType(InputType.TYPE_CLASS_TEXT);
              input.setLayoutParams(new LinearLayout.LayoutParams(
                      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
              LinearLayout linearLayout = new LinearLayout(SessionConnectExperimentActivity.this);
              linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
              int padding = TransformerUtilities.dp2px(getApplicationContext(), 10);
              int sidepadding = TransformerUtilities.dp2px(getApplicationContext(), 20);
              linearLayout.setPadding(sidepadding, padding, sidepadding, padding);
              linearLayout.addView(input);
              builder.setView(linearLayout);
              // Set up the buttons
              builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  String otp = input.getText().toString();
                  UteCachedExperiment cache = new UteCachedExperiment();
                  cache.experiment_id = experiment.experiment_id;
                  cache.experiment_alias = experiment.talias;
                  cache.title = experiment.title;
                  cache.description = experiment.description;
                  helpers.requestToCacheExperiment(otp, cache);
                }
              });
              builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.cancel();
                }
              });
              builder.show();

              return false;
            }
          } else {
            // do nothing, still opening the swipe menu.
            return true;
          }
        }

        // false : close the menu; true : not close the menu
        return false;
      }
    });
  }

  private void setupUITitleExperiments() {
    String masterTitle  = "UTE - ";
    if(this.activityIntention == INTENT_EXTRA_ACTION_CREATESESSION) {
      masterTitle += "Create session: ";
    } else if(this.activityIntention == INTENT_EXTRA_ACTION_CONNECTSESSION){
      masterTitle += "Connect: ";
    }

    String title = masterTitle + "Experiment List";
    ActionBar actionBar = this.getSupportActionBar();
    actionBar.setTitle(title);
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  private void setupUITitleSessions() {
    String masterTitle  = "UTE - ";
    if(this.activityIntention == INTENT_EXTRA_ACTION_CREATESESSION) {
      masterTitle += "Create session: ";
    } else if(this.activityIntention == INTENT_EXTRA_ACTION_CONNECTSESSION){
      masterTitle += "Connect: ";
    }

    String title = masterTitle + "Session List";
    ActionBar actionBar = this.getSupportActionBar();
    actionBar.setTitle(title);
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  public void setListViewListener() {
    if(this.listDisplayMode == LIST_DISPLAYMODE_EXPERIMENTS) {
      this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          responseToChoosenExperiment(connectListItems.get(position));
        }
      });

      this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
          SessionConnectExperimentActivity.this.swipeContainer.setRefreshing(true);
          ( new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
              // request.
              helpers.requestLatestExperimentListHttp();
            }
          }, 3000);
        }
      });

    } else if(this.listDisplayMode == LIST_DISPLAYMODE_SESSIONS){
      this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          final String sessionId = connectListDisplay.get(position);
          new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                  .setTitle("Connect - "+(getDisplayNameForExperiment(choosenExperimentAlias, choosenExperimentId))+":"+sessionId)
                  //.setMessage("Are you sure you want to connect to session " + sessionId + "?")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  //.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                  .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                      // request role to be specified before connect to session
                      requestRoleBeforeConnectToSession(choosenExperimentAlias, choosenExperimentId, sessionId);
                    }})
                  .setNegativeButton(android.R.string.no, null).show();

        }
      });

      this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
          SessionConnectExperimentActivity.this.swipeContainer.setRefreshing(true);
          ( new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
              // request.
              helpers.requestLatestSessionListHttp(adp, choosenExperimentId);
            }
          }, 3000);
        }
      });
    }

    this.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(AbsListView absListView, int i) {

      }

      @Override
      public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int topRowVerticalPosition =
                (listView == null || listView.getChildCount() == 0) ?
                        0 : listView.getChildAt(0).getTop();
        SessionConnectExperimentActivity.this.swipeContainer.setEnabled(topRowVerticalPosition >= 0);
      }
    });

  }

  private void requestRoleBeforeConnectToSession(final String choosenExperimentAlias, final String choosenExperimentId, final String sessionId) {
    new AlertDialog.Builder(SessionConnectExperimentActivity.this)
            .setTitle("Connect - "+getDisplayNameForExperiment(choosenExperimentAlias, choosenExperimentId)+":"+sessionId)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sensing", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // logic for role selection
                choosenSessionCode = sessionId;
                connectToSession(choosenSessionCode, choosenExperimentId, choosenExperimentAlias, SessionRoleSettings.ROLE_SENSING);
              }})
            .setNegativeButton("Labeling", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // logic for role selection
                choosenSessionCode = sessionId;
                connectToSession(choosenSessionCode, choosenExperimentId, choosenExperimentAlias, SessionRoleSettings.ROLE_LABELING);
              }})
            .setNeutralButton(android.R.string.no, null).show();
  }

  public void responseToChoosenExperiment(final SessionConnectExperimentActivityHelpers.ExperimentListItem experimentListItem) {
    String titleToDisplay = activityIntention == INTENT_EXTRA_ACTION_CREATESESSION ? "Create " : "Connect ";
    if(experimentListItem.cached) {
      titleToDisplay += "[Cached] ";
      if(activityIntention == INTENT_EXTRA_ACTION_CREATESESSION) {
        titleToDisplay += "[Initiator]";
      }
    }

    String messageToDisplay = "Do you want to join this experiment?\n";
    if(experimentListItem.title != null && experimentListItem.title.isEmpty() == false) {
      messageToDisplay += "\nAbout: \n" +
              experimentListItem.title + "\n";
    }

    if(experimentListItem.description != null && experimentListItem.description.isEmpty() == false) {
      messageToDisplay += "\nDetails: \n" +
              experimentListItem.description;
    }

    new AlertDialog.Builder(SessionConnectExperimentActivity.this)
            .setTitle(titleToDisplay)
            .setMessage(messageToDisplay)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                choosenExperimentId = experimentListItem.experiment_id;
                choosenExperimentAlias = experimentListItem.talias;

                boolean cached = experimentListItem.cached;
                if(activityIntention == INTENT_EXTRA_ACTION_CREATESESSION || cached) {
                  //requestRoleBeforeCreateSession(choosenExperimentId);
                  // by default role creation is for sensing device.
                  int role = activityIntention == INTENT_EXTRA_ACTION_CREATESESSION ? SessionRoleSettings.ROLE_SENSING : 0;
                  helpers.createNewSession(choosenExperimentId, choosenExperimentAlias, role, activityIntention, cached);
                } else {
                  // try to change list to session list.
                  SessionConnectExperimentActivity.this.listDisplayMode = LIST_DISPLAYMODE_SESSIONS;
                  setListViewListener();
                  setupUITitleSessions();
                  helpers.requestLatestSessionListHttp(adp, choosenExperimentId);
                }
              }})
            .setNegativeButton(android.R.string.no, null).show();
  }

  /*private void requestRoleBeforeCreateSession(final String choosenExperimentId) {
    new AlertDialog.Builder(SessionConnectExperimentActivity.this)
            .setTitle("Create Session - "+choosenExperimentId)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Sensing", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // logic for role selection
                helpers.createNewSession(choosenExperimentId, SessionRoleSettings.ROLE_SENSING);
              }})
            .setNeutralButton("Labeling", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // logic for role selection
                helpers.createNewSession(choosenExperimentId, SessionRoleSettings.ROLE_LABELING);
              }})
            .setNegativeButton(android.R.string.no, null).show();
  }*/

  public void onSessionCreated(String uniqueId, String sessionId, String experimentId, String experimentAlias, Double created_at, int role, boolean isInitiator) {
    Intent returnIntent = new Intent();
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_UNIQUE_ID, uniqueId);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_SESSION_ID, sessionId);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ID, experimentId);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ALIAS, experimentAlias);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_SRV_START_TIME, created_at != null ? created_at.doubleValue() : 0);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_ROLE, role);
    returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_IS_INITIATOR, isInitiator);
    SessionConnectExperimentActivity.this.setResult(ACTIVITY_RESULT_SESSIONCREATED, returnIntent);
    SessionConnectExperimentActivity.this.finish();
  }

  public void updateConnectListForExperiments(List<SessionConnectExperimentActivityHelpers.ExperimentListItem> experiments, int displaymode, boolean isOnline) {
    this.connectListItems.clear();
    // load data from cached experiment
    List<UteCachedExperiment> cachedExperiments = appStateService.getCachedExperiments();
    for(int i = 0; i < cachedExperiments.size(); i++) {
      UteCachedExperiment cached = cachedExperiments.get(i);
      SessionConnectExperimentActivityHelpers.ExperimentListItem experimentListItem = helpers.constructCachedExperimentListItem();
      experimentListItem.experiment_id = cached.experiment_id;
      experimentListItem.uid = cached.uid;
      experimentListItem.talias = cached.experiment_alias;
      experimentListItem.title = cached.title;
      experimentListItem.description = cached.description;
      this.connectListItems.add(experimentListItem);
    }
    if(experiments != null) {
      for(int i = 0; i < experiments.size(); i++) {
        SessionConnectExperimentActivityHelpers.ExperimentListItem experiment = experiments.get(i);
        if(containsExperimentId(this.connectListItems, experiment.experiment_id) == false) {
          this.connectListItems.add(experiment);
        }
      }
    }

    this.connectListDisplay.clear();
    for(int i = 0; i < this.connectListItems.size(); i++) {
      SessionConnectExperimentActivityHelpers.ExperimentListItem experiment = this.connectListItems.get(i);
      this.connectListDisplay.add(this.getDisplayNameForExperiment(experiment.talias, experiment.experiment_id));
    }

    emptyLabel.setVisibility(connectListDisplay.size() > 0 ? View.GONE : View.VISIBLE);
    SessionConnectExperimentActivity.this.swipeContainer.setRefreshing(false);
    this.adp.notifyDataSetChanged();
    this.listDisplayMode = displaymode;
  }

  public static boolean containsExperimentId(Collection<SessionConnectExperimentActivityHelpers.ExperimentListItem> c, String experimentId) {
    for(SessionConnectExperimentActivityHelpers.ExperimentListItem o : c) {
      if(o != null && o.experiment_id.equalsIgnoreCase(experimentId)) {
        return true;
      }
    }
    return false;
  }

  public void updateConnectListForSession(final ArrayAdapter<String> adp, List<String> sessions, int displaymode) {
    this.connectListDisplay.clear();
    this.connectListDisplay.addAll(sessions);

    emptyLabel.setVisibility(connectListDisplay.size() > 0 ? View.GONE : View.VISIBLE);
    SessionConnectExperimentActivity.this.swipeContainer.setRefreshing(false);
    adp.notifyDataSetChanged();
    this.listDisplayMode = displaymode;
  }

  private void connectToSession(final String sessionId, final String experimentId, final String experimentAlias, final int role) {
    // request http to connect then redirect to main activity.
    URL requestUrl;
    try {
      requestUrl = new URL(new URL(this.settingsService.getServerBaseUrl()), "api/experiment/session/connect");
    } catch (MalformedURLException e) {
      return;
    }

    final JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("did", appStateService.getDeviceId());
    jsonObject.addProperty("model", settingsService.getDeviceModel());
    jsonObject.addProperty("dtype", "Android");
    jsonObject.addProperty("session_id", sessionId);
    jsonObject.addProperty("experiment_id", experimentId);
    HttpAsyncTask task = new HttpAsyncTask(this, HttpAsyncTask.Method.POST, new HashMap<String, Object>(){{
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

        // generate unique uuid
        String uniqueId = UUID.randomUUID().toString();

        appStateService.setUniqueIdCache(uniqueId);
        appStateService.setExperimentIdCache(experimentId);
        appStateService.setSessionIdCache(sessionId);
        appStateService.setRoleCache(role);
        appStateService.setIsInitiatorCache(false);
        appStateService.setSessionServerStartTime(resultObject.created_at != null ? resultObject.created_at.doubleValue() : 0);

        if(resultObject.settings != null) {
          appStateService.setSessionSetupSettings(resultObject.settings);
        }

        appStateService.addSessionRecord(uniqueId, experimentId, experimentAlias, sessionId, uniqueId + TransformerUtilities.FILE_EXTENSION_SQLITE, false, appStateService.getCurrentTimeStamp(), false);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_UNIQUE_ID, uniqueId);
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_SESSION_ID, sessionId);
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ID, experimentId);
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_EXPERIMENT_ALIAS, experimentAlias);
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_SRV_START_TIME, resultObject.created_at != null ? resultObject.created_at.doubleValue() : 0);
        returnIntent.putExtra(INTENT_RESULT_ACTION_CONNECTED_ROLE, role);

        SessionConnectExperimentActivity.this.setResult(ACTIVITY_RESULT_SESSIONCONNECTED, returnIntent);
        SessionConnectExperimentActivity.this.finish();
      }

      @Override
      public void onErrorUnauthorized() {
        if(isActivityStillActive()) {
          new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                  .setTitle("Error")
                  .setMessage("Unauthorized request")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      }

      @Override
      public void onErrorGeneralRequest(int statusCode) {
        if(isActivityStillActive()) {
          new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                  .setTitle("Error")
                  .setMessage("Error sending request")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      }

      @Override
      public void onNoNetworkAvailable() {
        if(isActivityStillActive()) {
          new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                  .setTitle("Error")
                  .setMessage("No internet connection available")
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      }

      @Override
      public void onExceptionThrown(Exception e) {
        if(isActivityStillActive()) {
          new AlertDialog.Builder(SessionConnectExperimentActivity.this)
                  .setTitle("Error")
                  .setMessage(e.getLocalizedMessage())
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(android.R.string.ok, null)
                  .show();
        }
      }
    }){
      ProgressDialog dialog;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if(isActivityStillActive()) {
          dialog = new ProgressDialog(SessionConnectExperimentActivity.this);
          dialog.setTitle("Connecting to session.");
          dialog.setMessage("Loading...");
          dialog.setIndeterminate(true);
          dialog.setCancelable(false);
          dialog.show();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        if(isActivityStillActive()) {
          dialog.dismiss();
        }
        super.onPostExecute(result);
      }
    };
    task.setAcceptHeader("application/json");
    task.execute(requestUrl.toString());
  }

  @Override
  public void onBackPressed() {
    if(this.listDisplayMode == LIST_DISPLAYMODE_EXPERIMENTS) {
      super.onBackPressed();
    } else if(this.listDisplayMode == LIST_DISPLAYMODE_SESSIONS) {
      this.choosenSessionCode = null;
      SessionConnectExperimentActivity.this.listDisplayMode = LIST_DISPLAYMODE_EXPERIMENTS;
      setListViewListener();
      this.setupUITitleExperiments();
      helpers.requestLatestExperimentListHttp();
    }
  }

  public boolean isActivityStillActive() {
    if(!this.isFinishing())
    {
      return true;
    }

    return false;
  }

  private String getDisplayNameForExperiment(String alias, String experimentId) {
    String display = alias;
    if(display == null || display.isEmpty()) {
      display = experimentId;
    }

    return display;
  }
}
