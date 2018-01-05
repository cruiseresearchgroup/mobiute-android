package com.ute.mobi.activities.helpers;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.widget.Button;

import com.ute.mobi.R;
import com.ute.mobi.activities.SessionActivity;
import com.ute.mobi.activities.views.UteIntervalLabelButton;
import com.ute.mobi.services.AppStateService;
import com.ute.mobi.services.ServerSettingsService;

import java.util.List;

/**
 * Created by jonathanliono on 26/05/2016.
 */

public class SessionActivityHelpers {
  private SessionActivity activity;
  private ServerSettingsService settingsService;
  private AppStateService appStateService;

  public SessionActivityHelpers(SessionActivity activity, ServerSettingsService settingsService, AppStateService appStateService) {
    this.activity = activity;
    this.settingsService = settingsService;
    this.appStateService = appStateService;
  }

  public void setButtonGenericStates(Button button) {
    button.setBackgroundResource(R.drawable.generic_button_states);
    if(Build.VERSION.SDK_INT <= 23) {
      button.setTextColor(this.activity.getResources().getColorStateList(R.color.generic_button_states_textcolor));
    } else {
      button.setTextColor(this.activity.getResources().getColorStateList(R.color.generic_button_states_textcolor, null));
    }
  }

  public void setButtonAlertStates(Button button) {
    button.setBackgroundResource(R.drawable.alert_button_states);
    if(Build.VERSION.SDK_INT <= 23) {
      button.setTextColor(this.activity.getResources().getColorStateList(R.color.alert_button_states_textcolor));
    } else {
      button.setTextColor(this.activity.getResources().getColorStateList(R.color.alert_button_states_textcolor, null));
    }
  }

  public void setButtonGenericColor(Button button) {
    button.setAlpha(0.6f);
    button.setBackgroundResource(R.color.generic_button_color);
    button.setTextColor(Color.WHITE);
  }

  public void setButtonActivatedColor(Button button) {
    button.setAlpha(0.6f);
    button.setBackgroundResource(R.color.label_activated_button_color);
    button.setTextColor(Color.WHITE);
  }

  /**
   * Get any button that is activated in list, assuming the list can only have one button activated.
   * @param buttonlist
   * @return
   */
  public UteIntervalLabelButton getAnyButtonInGroupIsActivated(List<UteIntervalLabelButton> buttonlist, UteIntervalLabelButton buttonExcepted) {
    if(buttonlist == null || buttonlist.isEmpty())
      return null;

    for(int i = 0; i < buttonlist.size(); i++) {
      UteIntervalLabelButton intervalLabelButton = buttonlist.get(i);

      if(intervalLabelButton == buttonExcepted)
        continue;
      if(intervalLabelButton.isIntervalLabelActivated()) {
        return intervalLabelButton;
      }
    }

    return null;
  }

  public void triggerIntervalLabelButtonActivation(UteIntervalLabelButton intervalLabelButton) {
    intervalLabelButton.toggleIntervalLabelActivation();
    if(intervalLabelButton.isIntervalLabelActivated()) {
      this.setButtonActivatedColor(intervalLabelButton);
    } else {
      this.setButtonGenericColor(intervalLabelButton);
    }
  }

  public String getActiveLabels(List<UteIntervalLabelButton> buttonlist) {

    String labels = null;

    for(int i = 0; i < buttonlist.size(); i++) {
      UteIntervalLabelButton intervalLabelButton = buttonlist.get(i);
      if(intervalLabelButton.isIntervalLabelActivated()) {
        String labelText = intervalLabelButton.getText().toString();
        if(labels != null) {
          labels += "+";
          labels += labelText;
        } else {
          labels = labelText;
        }
      }
    }

    return labels;
  }
}
