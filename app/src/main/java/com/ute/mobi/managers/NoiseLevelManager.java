package com.ute.mobi.managers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.ute.mobi.activities.breceivers.NoiseLevelBroadcastReceiver;

import java.io.IOException;

/**
 * Created by jonathanliono on 8/11/2016.
 */

public class NoiseLevelManager {
  private static Context aContext=null;

  private static NoiseLevelListener listener;

  private static MediaRecorder mRecorder = null;

  private static Long MIN_TIME_TO_UPDATE_MILLISEC = null;

  /** indicates whether or not Microphone Sensor is supported */
  private static Boolean supported;
  /** indicates whether or not Microphone Sensor is running */
  private static boolean running = false;

  /**
   * Returns true if the manager is listening to orientation changes
   */
  public static boolean isListening() {
    return running;
  }

  /**
   * Unregisters listeners
   */
  public static void stopListening() {
    running = false;
    if (mRecorder != null) {
      mRecorder.stop();
      mRecorder.release();
      mRecorder = null;
      MIN_TIME_TO_UPDATE_MILLISEC = null;
    }
  }

  /**
   * Returns true if at least one Accelerometer sensor is available
   */
  public static boolean isSupported(Context context) {
    aContext = context;
    if (supported == null) {
      if (aContext != null) {
        if (aContext.getPackageManager().hasSystemFeature( PackageManager.FEATURE_MICROPHONE)) {
          //Microphone is present on the device
          return true;
        } else {
          return false;
        }
      } else {
        supported = Boolean.FALSE;
      }
    }
    return supported;
  }

  /**
   * Registers a listener and start listening
   * @param noiseLevelListener
   *             callback for accelerometer events
   */
  public static void startListening( NoiseLevelListener noiseLevelListener, long milliseconds)
  {
    if (mRecorder == null) {
      listener = noiseLevelListener;
      MIN_TIME_TO_UPDATE_MILLISEC = milliseconds;
      mRecorder = new MediaRecorder();
      mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mRecorder.setOutputFile("/dev/null/");

      try {
        mRecorder.prepare();
      } catch (IllegalStateException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      mRecorder.start();
      running = true;
      scheduleReadNoiseLevel(milliseconds);
    }
  }

  public static void scheduleReadNoiseLevel(long milliseconds) {
    if(aContext != null) {
      Context appContext = aContext.getApplicationContext();
      AlarmManager scheduler = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
      Intent i = new Intent(appContext, NoiseLevelBroadcastReceiver.class);
      PendingIntent scheduleNoiseLevelIntent = PendingIntent.getBroadcast(appContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

      scheduler.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + milliseconds, scheduleNoiseLevelIntent);
    }
  }

  public static void notifyNoiseLevelToUpdate() {
    if(isListening() && mRecorder != null && MIN_TIME_TO_UPDATE_MILLISEC != null) {
      int currentAmplitude = getAmplitude();
      listener.onAmplitudeChange(currentAmplitude);
      scheduleReadNoiseLevel(MIN_TIME_TO_UPDATE_MILLISEC);
    }
  }

  private static int getAmplitude() {
    if (mRecorder != null)
      return  (mRecorder.getMaxAmplitude());
    else
      return 0;
  }

}
