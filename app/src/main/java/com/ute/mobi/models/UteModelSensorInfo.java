package com.ute.mobi.models;

import java.util.HashMap;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class UteModelSensorInfo {

  public final static String KEY_TIMESTAMP = "t";

  public Double accelerometer_acceleration_x;
  public Double accelerometer_acceleration_y;
  public Double accelerometer_acceleration_z;
  public Double motion_gravity_x;
  public Double motion_gravity_y;
  public Double motion_gravity_z;
  public Double motion_user_acceleration_x;
  public Double motion_user_acceleration_y;
  public Double motion_user_acceleration_z;
  public Double motion_attitude_yaw;
  public Double motion_attitude_pitch;
  public Double motion_attitude_roll;
  public Double gyroscope_rotationrate_x;
  public Double gyroscope_rotationrate_y;
  public Double gyroscope_rotationrate_z;
  public Double motion_rotationrate_x;
  public Double motion_rotationrate_y;
  public Double motion_rotationrate_z;
  public Double magnetic_heading_x;
  public Double magnetic_heading_y;
  public Double magnetic_heading_z;
  public Double calibrated_magnetic_field_x;
  public Double calibrated_magnetic_field_y;
  public Double calibrated_magnetic_field_z;
  public Double calibrated_magnetic_field_accuracy;
  public Double magnetometer_x;
  public Double magnetometer_y;
  public Double magnetometer_z;
  public Double location_latitude;
  public Double location_longitude;
  public Double gps_accuracy;
  public Double gps_speed;
  public Double noise_level;
  public Double pressure;
  public Double altitude;
  public Double timestamp;

  public HashMap<String, Object> toHashMap() {
    HashMap<String, Object> hashmap = new HashMap<String, Object>();

    hashmap.put("aa_x", this.accelerometer_acceleration_x);
    hashmap.put("aa_y", this.accelerometer_acceleration_y);
    hashmap.put("aa_z", this.accelerometer_acceleration_z);

    hashmap.put("mg_x", this.motion_gravity_x);
    hashmap.put("mg_y", this.motion_gravity_y);
    hashmap.put("mg_z", this.motion_gravity_z);

    hashmap.put("mua_x", this.motion_user_acceleration_x);
    hashmap.put("mua_y", this.motion_user_acceleration_y);
    hashmap.put("mua_z", this.motion_user_acceleration_z);

    hashmap.put("ma_y", this.motion_attitude_yaw);
    hashmap.put("ma_p", this.motion_attitude_pitch);
    hashmap.put("ma_r", this.motion_attitude_roll);

    hashmap.put("grr_x", this.gyroscope_rotationrate_x);
    hashmap.put("grr_y", this.gyroscope_rotationrate_y);
    hashmap.put("grr_z", this.gyroscope_rotationrate_z);

    hashmap.put("mrr_x", this.motion_rotationrate_x);
    hashmap.put("mrr_y", this.motion_rotationrate_y);
    hashmap.put("mrr_z", this.motion_rotationrate_z);

    hashmap.put("mh_x", this.magnetic_heading_x);
    hashmap.put("mh_y", this.magnetic_heading_y);
    hashmap.put("mh_z", this.magnetic_heading_z);

    hashmap.put("cmf_x", this.calibrated_magnetic_field_x);
    hashmap.put("cmf_y", this.calibrated_magnetic_field_y);
    hashmap.put("cmf_z", this.calibrated_magnetic_field_z);
    hashmap.put("cmf_a", this.calibrated_magnetic_field_accuracy);

    hashmap.put("mm_x", this.magnetometer_x);
    hashmap.put("mm_y", this.magnetometer_y);
    hashmap.put("mm_z", this.magnetometer_z);

    hashmap.put("lat", this.location_latitude);
    hashmap.put("lon", this.location_longitude);
    hashmap.put("gps_a", this.gps_accuracy);
    hashmap.put("gps_s", this.gps_speed);
    hashmap.put("n_l", this.noise_level);

    hashmap.put("pres", this.pressure);
    hashmap.put("alt", this.altitude);

    hashmap.put("t", this.timestamp);

    return hashmap;
  }
}
