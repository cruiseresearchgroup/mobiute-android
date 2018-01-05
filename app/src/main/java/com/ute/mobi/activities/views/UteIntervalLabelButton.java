package com.ute.mobi.activities.views;

import android.content.Context;
import android.widget.Button;

/**
 * Created by jonathanliono on 26/05/2016.
 */

public class UteIntervalLabelButton extends Button {
  private boolean intervalLabelIsActivated;

  public UteIntervalLabelButton(Context context) {
    super(context);
  }

  public boolean isIntervalLabelActivated() {
    return this.intervalLabelIsActivated;
  }

  public void toggleIntervalLabelActivation() {
    if(this.intervalLabelIsActivated) {
      this.intervalLabelIsActivated = false;
    } else {
      this.intervalLabelIsActivated = true;
    }
  }
}
