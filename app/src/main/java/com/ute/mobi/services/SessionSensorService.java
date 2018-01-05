package com.ute.mobi.services;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Location;

import com.ute.mobi.managers.AccelerometerListener;
import com.ute.mobi.managers.AccelerometerManager;
import com.ute.mobi.managers.BarometerListener;
import com.ute.mobi.managers.BarometerManager;
import com.ute.mobi.managers.BluetoothListener;
import com.ute.mobi.managers.BluetoothManager;
import com.ute.mobi.managers.CellPhoneListener;
import com.ute.mobi.managers.CellPhoneManager;
import com.ute.mobi.managers.GPSManager;
import com.ute.mobi.managers.GyroscopeListener;
import com.ute.mobi.managers.GyroscopeManager;
import com.ute.mobi.managers.MagneticFieldCalibratedListener;
import com.ute.mobi.managers.MagneticFieldCalibratedManager;
import com.ute.mobi.managers.MagnetometerListener;
import com.ute.mobi.managers.MagnetometerManager;
import com.ute.mobi.managers.MotionAttitudeListener;
import com.ute.mobi.managers.MotionAttitudeManager;
import com.ute.mobi.managers.MotionCalibratedGyroscopeListener;
import com.ute.mobi.managers.MotionCalibratedGyroscopeManager;
import com.ute.mobi.managers.MotionGravityListener;
import com.ute.mobi.managers.MotionGravityManager;
import com.ute.mobi.managers.NoiseLevelListener;
import com.ute.mobi.managers.NoiseLevelManager;
import com.ute.mobi.managers.UserAccelerationListener;
import com.ute.mobi.managers.UserAccelerationManager;
import com.ute.mobi.managers.UteWifiListener;
import com.ute.mobi.managers.UteWifiManager;
import com.ute.mobi.models.UteModelBluetoothInfo;
import com.ute.mobi.models.UteModelCellInfo;
import com.ute.mobi.models.UteModelSensorInfo;
import com.ute.mobi.models.UteModelWifiInfo;
import com.ute.mobi.settings.SessionSetupSettings;
import com.ute.mobi.utilities.TransformerUtilities;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by jonathanliono on 9/01/15.
 */
public class SessionSensorService implements
        AccelerometerListener,
        MotionGravityListener,
        UserAccelerationListener,
        GyroscopeListener,
        MotionCalibratedGyroscopeListener,
        MotionAttitudeListener,
        MagneticFieldCalibratedListener,
        MagnetometerListener,
        NoiseLevelListener,
        BarometerListener,
        BluetoothListener,
        UteWifiListener,
        CellPhoneListener {

  private GPSManager gpsManager;

  private Double accelerometer_acceleration_x;
  private Double accelerometer_acceleration_y;
  private Double accelerometer_acceleration_z;

  private Double motion_gravity_x;
  private Double motion_gravity_y;
  private Double motion_gravity_z;

  private Double motion_user_acceleration_x;
  private Double motion_user_acceleration_y;
  private Double motion_user_acceleration_z;

  private Double motion_attitude_yaw;
  private Double motion_attitude_pitch;
  private Double motion_attitude_roll;

  private Double gyroscope_rotationrate_x;
  private Double gyroscope_rotationrate_y;
  private Double gyroscope_rotationrate_z;

  private Double motion_rotationrate_x;
  private Double motion_rotationrate_y;
  private Double motion_rotationrate_z;

  private Double calibrated_magnetic_field_x;
  private Double calibrated_magnetic_field_y;
  private Double calibrated_magnetic_field_z;
  private Double calibrated_magnetic_field_accuracy;

  private Double magnetometer_x;
  private Double magnetometer_y;
  private Double magnetometer_z;

  private Double noise_level;

  private Double pressure;
  private Double altitude;

  private List<UteModelBluetoothInfo> bluetooths;
  private List<UteModelWifiInfo> scannedwifis;
  private List<UteModelCellInfo> scannedCellInfos;

  private Context context;
  private AppStateService appStateService;

  public SessionSensorService(Context context, AppStateService appStateService) {
    this.appStateService = appStateService;

    SessionSetupSettings setupSettings = appStateService.getSessionSetupSettings();

    if (setupSettings != null) {
      if(setupSettings.sensors != null) {
        for(int i = 0; i < setupSettings.sensors.length; i++) {
          SessionSetupSettings.SettingsSensor setting = setupSettings.sensors[i];
          activateSensorBasedOnSetting(context, setting);
        }
      }
    }
  }

  private long getSensorReadingInterval(SessionSetupSettings.SettingsSensor setting) {
    if(setting.freq != null && setting.freq != 0) {
      return TransformerUtilities.convertHzToMillisec(setting.freq);
    } else if(setting.sec != null && setting.sec != 0) {
      return new Double(setting.sec * 1000).longValue();
    } else {
      return 0;
    }
  }

  private void activateSensorBasedOnSetting(Context context, SessionSetupSettings.SettingsSensor setting) {
    if ("accelerometer".equalsIgnoreCase(setting.name) && UserAccelerationManager.isSupported(context)) {
      UserAccelerationManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if (("accelerometer".equalsIgnoreCase(setting.name)) && MotionGravityManager.isSupported(context)) {
      MotionGravityManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("accelerometer".equalsIgnoreCase(setting.name) && AccelerometerManager.isSupported(context)) {
      AccelerometerManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("accelerometer".equalsIgnoreCase(setting.name) && MotionAttitudeManager.isSupported(context)) {
      MotionAttitudeManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("gyroscope".equalsIgnoreCase(setting.name) && GyroscopeManager.isSupported(context)) {
      GyroscopeManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("gyroscope".equalsIgnoreCase(setting.name) && MotionCalibratedGyroscopeManager.isSupported(context)) {
      MotionCalibratedGyroscopeManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("magnetometer".equalsIgnoreCase(setting.name) && MagneticFieldCalibratedManager.isSupported(context)) {
      MagneticFieldCalibratedManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("magnetometer".equalsIgnoreCase(setting.name) && MagnetometerManager.isSupported(context)) {
      MagnetometerManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("noise_level".equalsIgnoreCase(setting.name) && NoiseLevelManager.isSupported(context)) {
      NoiseLevelManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("barometer".equalsIgnoreCase(setting.name) && BarometerManager.isSupported(context)) {
      BarometerManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("gps".equalsIgnoreCase(setting.name)) {
      gpsManager = new GPSManager(context, this.getSensorReadingInterval(setting));
    }

    if ("bluetooth".equalsIgnoreCase(setting.name) && BluetoothManager.isSupported(context)) {
      BluetoothManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("wifi".equalsIgnoreCase(setting.name) && UteWifiManager.isSupported(context)) {
      UteWifiManager.startListening(this, this.getSensorReadingInterval(setting));
    }

    if ("cell".equalsIgnoreCase(setting.name) && CellPhoneManager.isSupported(context)) {
      CellPhoneManager.startListening(this, this.getSensorReadingInterval(setting));
    }
  }

  @Override
  public void onAccelerationChanged(float x, float y, float z) {
    this.accelerometer_acceleration_x = new Double(x);
    this.accelerometer_acceleration_y = new Double(y);
    this.accelerometer_acceleration_z = new Double(z);
  }

  @Override
  public void onMotionGravityChanged(float x, float y, float z) {
    this.motion_gravity_x = new Double(x);
    this.motion_gravity_y = new Double(y);
    this.motion_gravity_z = new Double(z);
  }

  @Override
  public void onUserAccelerationChanged(float x, float y, float z) {
    this.motion_user_acceleration_x = new Double(x);
    this.motion_user_acceleration_y = new Double(y);
    this.motion_user_acceleration_z = new Double(z);
  }

  @Override
  public void onShake(float force) {
  }

  @Override
  public void onMotionAttitudeChanged(float yaw, float pitch, float roll) {
    this.motion_attitude_yaw = new Double(yaw);
    this.motion_attitude_pitch = new Double(pitch);
    this.motion_attitude_roll = new Double(roll);
  }

  @Override
  public void onGyroscopeChanged(float x, float y, float z) {
    this.gyroscope_rotationrate_x = new Double(x);
    this.gyroscope_rotationrate_y = new Double(y);
    this.gyroscope_rotationrate_z = new Double(z);
  }

  @Override
  public void onMotionRotationRateChanged(float x, float y, float z) {
    this.motion_rotationrate_x = new Double(x);
    this.motion_rotationrate_y = new Double(y);
    this.motion_rotationrate_z = new Double(z);
  }

  @Override
  public void onCalibratedMagneticFieldChanged(float x, float y, float z, int accuracy) {
    this.calibrated_magnetic_field_x = new Double(x);
    this.calibrated_magnetic_field_y = new Double(y);
    this.calibrated_magnetic_field_z = new Double(z);
    this.calibrated_magnetic_field_accuracy = new Double(accuracy);
  }

  @Override
  public void onMagnetometerChanged(float x, float y, float z) {
    this.magnetometer_x = new Double(x);
    this.magnetometer_y = new Double(y);
    this.magnetometer_z = new Double(z);
  }

  @Override
  public void onAmplitudeChange(int amplitude) {
    double noiselvl = 20 * Math.log10(amplitude / 32767.0);
    boolean isinfinite = Double.isInfinite(noiselvl);
    if(isinfinite) {
      this.noise_level = null;
    } else {
      this.noise_level = noiselvl;
    }
  }

  @Override
  public void onBarometerSensorChanged(float pressure, float altitude) {
    // measurement of pressure is in millibar, convert to kPa (kilopascals) measurement.
    double barometerPressure = pressure/10.0;
    this.pressure = barometerPressure;
    this.altitude = new Double(altitude);
  }

  @Override
  public void onBluetoothChanged(List<UteModelBluetoothInfo> devices) {
    bluetooths = devices;
  }

  @Override
  public void onWifiChange(List<UteModelWifiInfo> wifis) {
    scannedwifis = wifis;
  }

  @Override
  public void onCellInfoChanged(List<UteModelCellInfo> cellInfos) {
    this.scannedCellInfos = cellInfos;
  }

  public UteModelSensorInfo readSensors() {
    UteModelSensorInfo sensorInfo = new UteModelSensorInfo();
    sensorInfo.accelerometer_acceleration_x = this.accelerometer_acceleration_x;
    sensorInfo.accelerometer_acceleration_y = this.accelerometer_acceleration_y;
    sensorInfo.accelerometer_acceleration_z = this.accelerometer_acceleration_z;

    sensorInfo.motion_gravity_x = this.motion_gravity_x;
    sensorInfo.motion_gravity_y = this.motion_gravity_y;
    sensorInfo.motion_gravity_z = this.motion_gravity_z;

    sensorInfo.motion_user_acceleration_x = this.motion_user_acceleration_x;
    sensorInfo.motion_user_acceleration_y = this.motion_user_acceleration_y;
    sensorInfo.motion_user_acceleration_z = this.motion_user_acceleration_z;

    sensorInfo.motion_attitude_yaw = this.motion_attitude_yaw;
    sensorInfo.motion_attitude_pitch = this.motion_attitude_pitch;
    sensorInfo.motion_attitude_roll = this.motion_attitude_roll;

    sensorInfo.gyroscope_rotationrate_x = this.gyroscope_rotationrate_x;
    sensorInfo.gyroscope_rotationrate_y = this.gyroscope_rotationrate_y;
    sensorInfo.gyroscope_rotationrate_z = this.gyroscope_rotationrate_z;

    sensorInfo.motion_rotationrate_x = this.motion_rotationrate_x;
    sensorInfo.motion_rotationrate_y = this.motion_rotationrate_y;
    sensorInfo.motion_rotationrate_z = this.motion_rotationrate_z;

    sensorInfo.calibrated_magnetic_field_x = this.calibrated_magnetic_field_x;
    sensorInfo.calibrated_magnetic_field_y = this.calibrated_magnetic_field_y;
    sensorInfo.calibrated_magnetic_field_z = this.calibrated_magnetic_field_z;
    sensorInfo.calibrated_magnetic_field_accuracy = this.calibrated_magnetic_field_accuracy;

    sensorInfo.magnetometer_x = this.magnetometer_x;
    sensorInfo.magnetometer_y = this.magnetometer_y;
    sensorInfo.magnetometer_z = this.magnetometer_z;

    sensorInfo.noise_level = this.noise_level;

    sensorInfo.pressure = this.pressure;
    sensorInfo.altitude = this.altitude;

    if (this.gpsManager != null && this.gpsManager.canGetLocation()) {
      Location location = this.gpsManager.getLocation();
      if (location != null) {
        sensorInfo.location_latitude = location.getLatitude();
        sensorInfo.location_longitude = location.getLongitude();
        sensorInfo.gps_accuracy = new Double(location.getAccuracy());
        sensorInfo.gps_speed = new Double(location.getSpeed());

        GeomagneticField geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis());

        sensorInfo.magnetic_heading_x = new Double(geoField.getX());
        sensorInfo.magnetic_heading_y = new Double(geoField.getY());
        sensorInfo.magnetic_heading_z = new Double(geoField.getZ());
      }
    }

    sensorInfo.timestamp = this.appStateService.getSynchronizedCurrentTime();

    return sensorInfo;
  }

  public List<UteModelBluetoothInfo> readBluetooths() {
    List<UteModelBluetoothInfo> tempBluetooth = this.bluetooths;

    if(tempBluetooth != null) {
      for(UteModelBluetoothInfo device: tempBluetooth) {
        device.timestamp = this.appStateService.getSynchronizedCurrentTime();
      }
    }

    this.bluetooths = new ArrayList<>();

    return tempBluetooth;
  }

  public List<UteModelWifiInfo> readWifis() {
    List<UteModelWifiInfo> tempWifis = this.scannedwifis;

    if(tempWifis != null) {
      for(UteModelWifiInfo wifi: tempWifis) {
        wifi.timestamp = this.appStateService.getSynchronizedCurrentTime();
      }
    }

    this.scannedwifis = new ArrayList<>();
    return tempWifis;
  }

  public List<UteModelCellInfo> readcellInfos() {
    List<UteModelCellInfo> tempCellInfos = this.scannedCellInfos;
    if(tempCellInfos != null) {
      for(UteModelCellInfo ci: tempCellInfos) {
        ci.timestamp = this.appStateService.getSynchronizedCurrentTime();
      }
    }

    this.scannedCellInfos = new ArrayList<>();
    return tempCellInfos;
  }

  public void stopSensors() {
    UserAccelerationManager.stopListening();
    MotionGravityManager.stopListening();
    AccelerometerManager.stopListening();
    MotionAttitudeManager.stopListening();
    GyroscopeManager.stopListening();
    MotionCalibratedGyroscopeManager.stopListening();
    MagneticFieldCalibratedManager.stopListening();
    MagnetometerManager.stopListening();
    NoiseLevelManager.stopListening();
    BarometerManager.stopListening();
    BluetoothManager.stopListening();
    UteWifiManager.stopListening();
    if(this.gpsManager != null) {
      this.gpsManager.stopUsingGPS();
      this.gpsManager = null;
    }

  }
}
