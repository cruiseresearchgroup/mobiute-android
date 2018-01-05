package com.ute.mobi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.ute.mobi.R;
import com.ute.mobi.utilities.AndroidLogger;

import butterknife.ButterKnife;

/**
 * Created by jonathanliono on 29/01/2016.
 */
public class SessionLabelingActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    Intent intent = getIntent();

    this.setupUI();
    AndroidLogger.w("SessionLabelingActivity", "Finished setup UI");
  }

  private void setupUI() {
    setContentView(R.layout.activity_session_labeling_inprogress);

    // inject all views into properties of this activity.
    ButterKnife.bind(this);

    getSupportActionBar().setTitle("Mobi-UTE: Labeling - S54993433");
  }
}
