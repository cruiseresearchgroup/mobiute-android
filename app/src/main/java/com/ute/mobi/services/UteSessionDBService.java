package com.ute.mobi.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.ute.mobi.models.UteModelBluetoothInfo;
import com.ute.mobi.models.UteModelCellInfo;
import com.ute.mobi.models.UteModelIntervalLabels;
import com.ute.mobi.models.UteModelSensorInfo;
import com.ute.mobi.models.UteModelWifiInfo;
import com.ute.mobi.utilities.SqliteQueryConstructor;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class UteSessionDBService {

  private SQLiteDatabase database;
  private UteDatabaseHelper dbHelper;
  private Context context;
  private String databaseFileName;
  private String latestMessage;

  private Double lastLat;
  private Double lastLong;

  public UteSessionDBService(Context context, String databaseFileName, String sessionId, String experimentId) {
    this.context = context.getApplicationContext();
    this.databaseFileName = databaseFileName;
    dbHelper = new UteDatabaseHelper(context, AppStateService.getInstance().getSessionDatabasesLocationFolder() + databaseFileName, sessionId, experimentId);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public File getDbFile() {
    File f = new File(AppStateService.getInstance().getSessionDatabasesLocationFolder() + databaseFileName);
    return f;
  }

  public void close() {
    dbHelper.close();
  }

  public void destroy() {
    dbHelper.close();
    context.deleteDatabase(AppStateService.getInstance().getSessionDatabasesLocationFolder() + this.databaseFileName);
  }

  public void updateSessionId(String sessionId) {
    dbHelper.sessionid = sessionId;
    ContentValues values = new ContentValues();
    values.put("value", sessionId);
    database.update("db_definitions", values, "name='"+dbHelper.KEY_DB_DEFS_SESSIONID+"'", null);
  }

  public void insertSensorInfo(UteModelSensorInfo sensorInfo) {
    ContentValues values = new ContentValues();
    values.put("accelerometer_acceleration_x", sensorInfo.accelerometer_acceleration_x);
    values.put("accelerometer_acceleration_y", sensorInfo.accelerometer_acceleration_y);
    values.put("accelerometer_acceleration_z", sensorInfo.accelerometer_acceleration_z);
    values.put("motion_gravity_x", sensorInfo.motion_gravity_x);
    values.put("motion_gravity_y", sensorInfo.motion_gravity_y);
    values.put("motion_gravity_z", sensorInfo.motion_gravity_z);
    values.put("motion_user_acceleration_x", sensorInfo.motion_user_acceleration_x);
    values.put("motion_user_acceleration_y", sensorInfo.motion_user_acceleration_y);
    values.put("motion_user_acceleration_z", sensorInfo.motion_user_acceleration_z);
    values.put("motion_attitude_yaw", sensorInfo.motion_attitude_yaw);
    values.put("motion_attitude_pitch", sensorInfo.motion_attitude_pitch);
    values.put("motion_attitude_roll", sensorInfo.motion_attitude_roll);
    values.put("gyroscope_rotationrate_x", sensorInfo.gyroscope_rotationrate_x);
    values.put("gyroscope_rotationrate_y", sensorInfo.gyroscope_rotationrate_y);
    values.put("gyroscope_rotationrate_z", sensorInfo.gyroscope_rotationrate_z);
    values.put("motion_rotationrate_x", sensorInfo.motion_rotationrate_x);
    values.put("motion_rotationrate_y", sensorInfo.motion_rotationrate_y);
    values.put("motion_rotationrate_z", sensorInfo.motion_rotationrate_z);
    values.put("magnetic_heading_x", sensorInfo.magnetic_heading_x);
    values.put("magnetic_heading_y", sensorInfo.magnetic_heading_y);
    values.put("magnetic_heading_z", sensorInfo.magnetic_heading_z);
    values.put("calibrated_magnetic_field_x", sensorInfo.calibrated_magnetic_field_x);
    values.put("calibrated_magnetic_field_y", sensorInfo.calibrated_magnetic_field_y);
    values.put("calibrated_magnetic_field_z", sensorInfo.calibrated_magnetic_field_z);
    values.put("calibrated_magnetic_field_accuracy", sensorInfo.calibrated_magnetic_field_accuracy);
    values.put("magnetometer_x", sensorInfo.magnetometer_x);
    values.put("magnetometer_y", sensorInfo.magnetometer_y);
    values.put("magnetometer_z", sensorInfo.magnetometer_z);
    values.put("location_latitude", sensorInfo.location_latitude);
    values.put("location_longitude", sensorInfo.location_longitude);
    values.put("gps_accuracy", sensorInfo.gps_accuracy);
    values.put("gps_speed", sensorInfo.gps_speed);
    values.put("noise_level", sensorInfo.noise_level);
    values.put("pressure", sensorInfo.pressure);
    values.put("altitude", sensorInfo.altitude);
    values.put("logged_at", sensorInfo.timestamp);
    database.insert(UteDatabaseHelper.TABLE_NAME_SENSOR_INFOS, null, values);

    if(sensorInfo.location_latitude != null && sensorInfo.location_longitude != null) {
      if((this.lastLat == null && this.lastLong == null)
        || (this.lastLat.doubleValue() != sensorInfo.location_latitude.doubleValue() && this.lastLong.doubleValue() != sensorInfo.location_longitude.doubleValue())) {
        this.lastLat = sensorInfo.location_latitude;
        this.lastLong = sensorInfo.location_longitude;
      }
    }
  }

  public void insertBluetoothInfo(UteModelBluetoothInfo bluetoothInfo) {
    ContentValues values = new ContentValues();
    values.put("uuid", bluetoothInfo.uuid);
    values.put("name", bluetoothInfo.name);
    values.put("rssi", bluetoothInfo.rssi);
    values.put("logged_at", bluetoothInfo.timestamp);
    database.insert(UteDatabaseHelper.TABLE_NAME_BLUETOOTH_INFOS, null, values);
  }

  public void insertWifiInfo(UteModelWifiInfo wifiInfo) {
    ContentValues values = new ContentValues();
    values.put("ssid", wifiInfo.ssid);
    values.put("bssid", wifiInfo.bssid);
    values.put("capabilities", wifiInfo.capabilities);
    values.put("channel_width", wifiInfo.channel_width);
    values.put("dual_channel", wifiInfo.dual_channel ? 1 : 0);
    values.put("freq20", wifiInfo.freq20);
    values.put("freq_center", wifiInfo.freq_center);
    values.put("freq_center_2", wifiInfo.freq_center_2);
    values.put("venue_name", wifiInfo.venue_name);
    values.put("rssi", wifiInfo.rssi);
    values.put("logged_at", wifiInfo.timestamp);
    long id = database.insert(UteDatabaseHelper.TABLE_NAME_WIFI_INFOS, null, values);
    System.out.println("Added new entry: " + id);
  }

  public void insertCellInfo(UteModelCellInfo cellInfo) {
    ContentValues values = new ContentValues();
    values.put("cid", cellInfo.cid);
    values.put("lac", cellInfo.lac);
    values.put("ss", cellInfo.ss);
    values.put("logged_at", cellInfo.timestamp);
    long id = database.insert(UteDatabaseHelper.TABLE_NAME_CELL_INFOS, null, values);
  }

  public void insertSensorIntervalLabel(UteModelIntervalLabels intervalLabels) {
    ContentValues values = new ContentValues();
    values.put("start_date", intervalLabels.start_date);
    values.put("end_date", intervalLabels.end_date);
    values.put("labels", intervalLabels.labels);

    database.insert(UteDatabaseHelper.TABLE_NAME_SENSOR_INTERVAL_LABELS, null, values);
  }

  public Double getMinimumTimestamp() {
    Cursor cursor = this.database.rawQuery("SELECT MIN(logged_at) as minTimestamp FROM "+UteDatabaseHelper.TABLE_NAME_SENSOR_INFOS, null);
    if (cursor.moveToFirst()) {
      do {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<cursor.getColumnCount();i++)
        {
          map.put(cursor.getColumnName(i), i);
        }

        return cursor.getDouble(map.get("minTimestamp"));
      }while (cursor.moveToNext());
    }

    return null;
  }

  public Double getLastTimestamp() {
    Cursor cursor = this.database.rawQuery("SELECT MAX(logged_at) as endTimestamp FROM sensor_infos", null);
    if (cursor.moveToFirst()) {
      do {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<cursor.getColumnCount();i++)
        {
          map.put(cursor.getColumnName(i), i);
        }

        return cursor.getDouble(map.get("endTimestamp"));
      }while (cursor.moveToNext());
    }

    return null;
  }

  public ArrayList<HashMap<String, Object>> fetchSessionInfosByLimit(int recordsLimit) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;
    Cursor cursor = this.database.rawQuery("SELECT * FROM sensor_infos ORDER BY logged_at LIMIT ?", new String[] { String.valueOf(recordsLimit) });

    if (cursor.moveToFirst()) {
      do {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<cursor.getColumnCount();i++)
        {
          map.put(cursor.getColumnName(i), i);
        }

        UteModelSensorInfo model = new UteModelSensorInfo();
        model.accelerometer_acceleration_x = cursor.getDouble(map.get("accelerometer_acceleration_x"));
        model.accelerometer_acceleration_y = cursor.getDouble(map.get("accelerometer_acceleration_y"));
        model.accelerometer_acceleration_z = cursor.getDouble(map.get("accelerometer_acceleration_z"));

        model.motion_gravity_x = cursor.getDouble(map.get("motion_gravity_x"));
        model.motion_gravity_y = cursor.getDouble(map.get("motion_gravity_y"));
        model.motion_gravity_z = cursor.getDouble(map.get("motion_gravity_z"));

        model.motion_user_acceleration_x = cursor.getDouble(map.get("motion_user_acceleration_x"));
        model.motion_user_acceleration_y = cursor.getDouble(map.get("motion_user_acceleration_y"));
        model.motion_user_acceleration_z = cursor.getDouble(map.get("motion_user_acceleration_z"));

        model.motion_attitude_yaw = cursor.getDouble(map.get("motion_attitude_yaw"));
        model.motion_attitude_pitch = cursor.getDouble(map.get("motion_attitude_pitch"));
        model.motion_attitude_roll = cursor.getDouble(map.get("motion_attitude_roll"));

        model.gyroscope_rotationrate_x = cursor.getDouble(map.get("gyroscope_rotationrate_x"));
        model.gyroscope_rotationrate_y = cursor.getDouble(map.get("gyroscope_rotationrate_y"));
        model.gyroscope_rotationrate_z = cursor.getDouble(map.get("gyroscope_rotationrate_z"));

        model.motion_rotationrate_x = cursor.getDouble(map.get("motion_rotationrate_x"));
        model.motion_rotationrate_y = cursor.getDouble(map.get("motion_rotationrate_y"));
        model.motion_rotationrate_z = cursor.getDouble(map.get("motion_rotationrate_z"));

        model.magnetic_heading_x = cursor.getDouble(map.get("magnetic_heading_x"));
        model.magnetic_heading_y = cursor.getDouble(map.get("magnetic_heading_y"));
        model.magnetic_heading_z = cursor.getDouble(map.get("magnetic_heading_z"));

        model.calibrated_magnetic_field_x = cursor.getDouble(map.get("calibrated_magnetic_field_x"));
        model.calibrated_magnetic_field_y = cursor.getDouble(map.get("calibrated_magnetic_field_y"));
        model.calibrated_magnetic_field_z = cursor.getDouble(map.get("calibrated_magnetic_field_z"));
        model.calibrated_magnetic_field_accuracy = cursor.getDouble(map.get("calibrated_magnetic_field_accuracy"));

        model.magnetometer_x = cursor.getDouble(map.get("magnetometer_x"));
        model.magnetometer_y = cursor.getDouble(map.get("magnetometer_y"));
        model.magnetometer_z = cursor.getDouble(map.get("magnetometer_z"));

        model.location_latitude = cursor.getDouble(map.get("location_latitude"));
        model.location_longitude = cursor.getDouble(map.get("location_longitude"));
        model.gps_accuracy = cursor.getDouble(map.get("gps_accuracy"));

        model.gps_speed = cursor.getDouble(map.get("gps_speed"));
        model.noise_level = cursor.getDouble(map.get("noise_level"));
        model.pressure = cursor.getDouble(map.get("pressure"));
        model.altitude = cursor.getDouble(map.get("altitude"));
        model.timestamp = cursor.getDouble(map.get("logged_at"));

        list.add(model.toHashMap());
      } while (cursor.moveToNext());
    }

    return list;
  }

  public ArrayList<HashMap<String, Object>> fetchSessionInfosBefore(double thetime) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;
    Cursor cursor = this.database.rawQuery("SELECT * FROM sensor_infos WHERE logged_at < ? ORDER BY logged_at", new String[] { String.valueOf(thetime) });

    if (cursor.moveToFirst()) {
      do {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<cursor.getColumnCount();i++)
        {
          map.put(cursor.getColumnName(i), i);
        }

        UteModelSensorInfo model = new UteModelSensorInfo();
        model.accelerometer_acceleration_x = cursor.getDouble(map.get("accelerometer_acceleration_x"));
        model.accelerometer_acceleration_y = cursor.getDouble(map.get("accelerometer_acceleration_y"));
        model.accelerometer_acceleration_z = cursor.getDouble(map.get("accelerometer_acceleration_z"));

        model.motion_gravity_x = cursor.getDouble(map.get("motion_gravity_x"));
        model.motion_gravity_y = cursor.getDouble(map.get("motion_gravity_y"));
        model.motion_gravity_z = cursor.getDouble(map.get("motion_gravity_z"));

        model.motion_user_acceleration_x = cursor.getDouble(map.get("motion_user_acceleration_x"));
        model.motion_user_acceleration_y = cursor.getDouble(map.get("motion_user_acceleration_y"));
        model.motion_user_acceleration_z = cursor.getDouble(map.get("motion_user_acceleration_z"));

        model.motion_attitude_yaw = cursor.getDouble(map.get("motion_attitude_yaw"));
        model.motion_attitude_pitch = cursor.getDouble(map.get("motion_attitude_pitch"));
        model.motion_attitude_roll = cursor.getDouble(map.get("motion_attitude_roll"));

        model.gyroscope_rotationrate_x = cursor.getDouble(map.get("gyroscope_rotationrate_x"));
        model.gyroscope_rotationrate_y = cursor.getDouble(map.get("gyroscope_rotationrate_y"));
        model.gyroscope_rotationrate_z = cursor.getDouble(map.get("gyroscope_rotationrate_z"));

        model.motion_rotationrate_x = cursor.getDouble(map.get("motion_rotationrate_x"));
        model.motion_rotationrate_y = cursor.getDouble(map.get("motion_rotationrate_y"));
        model.motion_rotationrate_z = cursor.getDouble(map.get("motion_rotationrate_z"));

        model.magnetic_heading_x = cursor.getDouble(map.get("magnetic_heading_x"));
        model.magnetic_heading_y = cursor.getDouble(map.get("magnetic_heading_y"));
        model.magnetic_heading_z = cursor.getDouble(map.get("magnetic_heading_z"));

        model.calibrated_magnetic_field_x = cursor.getDouble(map.get("calibrated_magnetic_field_x"));
        model.calibrated_magnetic_field_y = cursor.getDouble(map.get("calibrated_magnetic_field_y"));
        model.calibrated_magnetic_field_z = cursor.getDouble(map.get("calibrated_magnetic_field_z"));
        model.calibrated_magnetic_field_accuracy = cursor.getDouble(map.get("calibrated_magnetic_field_accuracy"));

        model.magnetometer_x = cursor.getDouble(map.get("magnetometer_x"));
        model.magnetometer_y = cursor.getDouble(map.get("magnetometer_y"));
        model.magnetometer_z = cursor.getDouble(map.get("magnetometer_z"));

        model.location_latitude = cursor.getDouble(map.get("location_latitude"));
        model.location_longitude = cursor.getDouble(map.get("location_longitude"));
        model.gps_accuracy = cursor.getDouble(map.get("gps_accuracy"));

        model.gps_speed = cursor.getDouble(map.get("gps_speed"));
        model.noise_level = cursor.getDouble(map.get("noise_level"));
        model.pressure = cursor.getDouble(map.get("pressure"));
        model.altitude = cursor.getDouble(map.get("altitude"));
        model.timestamp = cursor.getDouble(map.get("logged_at"));

        list.add(model.toHashMap());
      } while (cursor.moveToNext());
    }

    return list;
  }

  public ArrayList<HashMap<String, Object>> fetchBluetoothInfosByLimit(int recordsLimit) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;

    try{
      Cursor cursor = this.database.rawQuery("SELECT * FROM bluetooth_infos ORDER BY id, logged_at LIMIT ?", new String[] { String.valueOf(recordsLimit) });

      if (cursor.moveToFirst()) {
        do {
          HashMap<String, Integer> map = new HashMap<String, Integer>();
          for(int i=0; i<cursor.getColumnCount();i++)
          {
            map.put(cursor.getColumnName(i), i);
          }

          UteModelBluetoothInfo model = new UteModelBluetoothInfo();
          model.id = cursor.getLong(map.get("id"));
          model.uuid = cursor.getString(map.get("uuid"));
          model.name = cursor.getString(map.get("name"));
          model.rssi = cursor.getDouble(map.get("rssi"));
          model.timestamp = cursor.getDouble(map.get("logged_at"));

          list.add(model.toHashMap());
        } while (cursor.moveToNext());
      }
    }catch(android.database.sqlite.SQLiteException ex) {
      Log.e("Mobi-UTE", ex.getMessage());
    }

    return list;
  }

  public ArrayList<HashMap<String, Object>> fetchWifiInfosByLimit(int recordsLimit) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;

    try {
      Cursor cursor = this.database.rawQuery("SELECT * FROM "+ UteDatabaseHelper.TABLE_NAME_WIFI_INFOS +" ORDER BY id, logged_at LIMIT ?", new String[] { String.valueOf(recordsLimit) });

      if (cursor.moveToFirst()) {
        do {
          HashMap<String, Integer> map = new HashMap<String, Integer>();
          for(int i=0; i<cursor.getColumnCount();i++)
          {
            map.put(cursor.getColumnName(i), i);
          }

          UteModelWifiInfo model = new UteModelWifiInfo();
          model.id = cursor.getLong(map.get("id"));
          model.ssid = cursor.getString(map.get("ssid"));
          model.bssid = cursor.getString(map.get("bssid"));
          model.capabilities = cursor.getString(map.get("capabilities"));
          if(!cursor.isNull(map.get("channel_width")))
            model.channel_width = cursor.getInt(map.get("channel_width"));
          if(!cursor.isNull(map.get("dual_channel")))
            model.dual_channel = cursor.getInt(map.get("dual_channel")) == 1;
          if(!cursor.isNull(map.get("freq20")))
            model.freq20 = cursor.getInt(map.get("freq20"));
          if(!cursor.isNull(map.get("freq_center")))
            model.freq_center = cursor.getInt(map.get("freq_center"));
          if(!cursor.isNull(map.get("freq_center_2")))
            model.freq_center_2 = cursor.getInt(map.get("freq_center_2"));
          if(!cursor.isNull(map.get("venue_name")))
            model.venue_name = cursor.getString(map.get("venue_name"));

          model.rssi = cursor.getDouble(map.get("rssi"));
          model.timestamp = cursor.getDouble(map.get("logged_at"));

          list.add(model.toHashMap());
        } while (cursor.moveToNext());
      }
    }catch(android.database.sqlite.SQLiteException ex) {
      Log.e("Mobi-UTE", ex.getMessage());
    }

    return list;
  }

  public ArrayList<HashMap<String, Object>> fetchCellInfosByLimit(int recordsLimit) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;

    try {
      Cursor cursor = this.database.rawQuery("SELECT * FROM "+ UteDatabaseHelper.TABLE_NAME_CELL_INFOS +" ORDER BY id, logged_at LIMIT ?", new String[] { String.valueOf(recordsLimit) });

      if (cursor.moveToFirst()) {
        do {
          HashMap<String, Integer> map = new HashMap<String, Integer>();
          for(int i=0; i<cursor.getColumnCount();i++)
          {
            map.put(cursor.getColumnName(i), i);
          }

          UteModelCellInfo model = new UteModelCellInfo();
          model.id = cursor.getLong(map.get("id"));
          model.cid = cursor.getInt(map.get("cid"));
          model.lac = cursor.getInt(map.get("lac"));
          model.ss = cursor.getInt(map.get("ss"));
          model.timestamp = cursor.getDouble(map.get("logged_at"));
          list.add(model.toHashMap());
        } while (cursor.moveToNext());
      }
    }catch(android.database.sqlite.SQLiteException ex) {
      Log.e("Mobi-UTE", ex.getMessage());
    }

    return list;
  }

  public ArrayList<HashMap<String, Object>> fetchSessionIntervalLabelsByLimit(int recordsLimit) {
    ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
    this.latestMessage = null;
    Cursor cursor = this.database.rawQuery("SELECT * FROM "+UteDatabaseHelper.TABLE_NAME_SENSOR_INTERVAL_LABELS+" ORDER BY start_date LIMIT ?", new String[] { String.valueOf(recordsLimit) });
    if (cursor.moveToFirst()) {
      do {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for(int i=0; i<cursor.getColumnCount();i++)
        {
          map.put(cursor.getColumnName(i), i);
        }

        UteModelIntervalLabels model = new UteModelIntervalLabels();
        model.start_date = cursor.getDouble(map.get("start_date"));
        model.end_date = cursor.getDouble(map.get("end_date"));
        model.labels = cursor.getString(map.get("labels"));

        list.add(model.toHashMap());
      } while (cursor.moveToNext());
    }

    return list;
  }

  public void deleteSessionInfosBefore(double thetime) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_SENSOR_INFOS, "logged_at < ?", new String[] { String.valueOf(thetime) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public void deleteSessionInfosBeforeAndEqualTo(double thetime) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_SENSOR_INFOS, "logged_at <= ?", new String[] { String.valueOf(thetime) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public void deleteBluetoothInfosBeforeAndEqualTo(double thetime, long lastId) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_BLUETOOTH_INFOS, "logged_at <= ? AND id <= ?", new String[] { String.valueOf(thetime), String.valueOf(lastId) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public void deleteWifiInfosBeforeAndEqualTo(double thetime, long lastId) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_WIFI_INFOS, "logged_at <= ? AND id <= ?", new String[] { String.valueOf(thetime), String.valueOf(lastId) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public void deleteCellInfosBeforeAndEqualTo(double thetime, long lastId) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_CELL_INFOS, "logged_at <= ? AND id <= ?", new String[] { String.valueOf(thetime), String.valueOf(lastId) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public void deleteSessionIntervalLabelsBeforeAndEqualTo(double thetime) {
    int deleteSuccess = database.delete(UteDatabaseHelper.TABLE_NAME_SENSOR_INTERVAL_LABELS, "start_date <= ?", new String[] { String.valueOf(thetime) });
    if(deleteSuccess > 0) {
      // log how many deleted
    }
  }

  public String getLatestMessage() {
    return this.latestMessage;
  }

  public String get64EncodedDbFile() throws IOException {
    byte[] bytes = loadFile(this.getDbFile());
    return Base64.encodeToString(bytes, Base64.DEFAULT);
  }

  private static byte[] loadFile(File file) throws IOException {
    InputStream is = new FileInputStream(file);

    long length = file.length();
    if (length > Integer.MAX_VALUE) {
      // File is too large
    }
    byte[] bytes = new byte[(int) length];

    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }

    if (offset < bytes.length) {
      throw new IOException("Could not completely read file " + file.getName());
    }

    is.close();
    return bytes;
  }

  private class UteDatabaseHelper extends SQLiteOpenHelper {

    protected final static String KEY_DB_DEFS_SESSIONID = "session_id";
    protected final static String KEY_DB_DEFS_EXPERIMENTID = "experiment_id";

    protected final static String TABLE_NAME_SENSOR_INFOS = "sensor_infos";
    protected final static String TABLE_NAME_BLUETOOTH_INFOS = "bluetooth_infos";
    protected final static String TABLE_NAME_WIFI_INFOS = "wifi_infos";
    protected final static String TABLE_NAME_CELL_INFOS = "cell_infos";
    protected final static String TABLE_NAME_SENSOR_INTERVAL_LABELS = "sensor_interval_labels";

    private static final int DATABASE_VERSION = 1;
    private String sessionid;
    private String experimentId;

    public UteDatabaseHelper(Context context, String databaseFileName, String sessionId, String experimentId) {
      super(context, databaseFileName, null, DATABASE_VERSION);
      this.sessionid = sessionId;
      this.experimentId = experimentId;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_NAME_SENSOR_INFOS+" (id integer primary key autoincrement, accelerometer_acceleration_x REAL, accelerometer_acceleration_y REAL, accelerometer_acceleration_z REAL, motion_gravity_x REAL, motion_gravity_y REAL, motion_gravity_z REAL, motion_user_acceleration_x REAL, motion_user_acceleration_y REAL, motion_user_acceleration_z REAL, motion_attitude_yaw REAL, motion_attitude_pitch REAL, motion_attitude_roll REAL, gyroscope_rotationrate_x REAL, gyroscope_rotationrate_y REAL, gyroscope_rotationrate_z REAL, motion_rotationrate_x REAL, motion_rotationrate_y REAL, motion_rotationrate_z REAL, magnetic_heading_x REAL, magnetic_heading_y REAL, magnetic_heading_z REAL, calibrated_magnetic_field_x REAL, calibrated_magnetic_field_y REAL, calibrated_magnetic_field_z REAL, calibrated_magnetic_field_accuracy REAL, magnetometer_x REAL, magnetometer_y REAL, magnetometer_z REAL, location_latitude REAL, location_longitude REAL, gps_accuracy REAL, gps_speed REAL, noise_level REAL, pressure REAL, altitude REAL, logged_at REAL DEFAULT CURRENT_TIMESTAMP NOT NULL)");
      db.execSQL("CREATE INDEX index_logged_at ON "+TABLE_NAME_SENSOR_INFOS+" (logged_at);");
      db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_NAME_BLUETOOTH_INFOS+" (id integer primary key autoincrement, uuid TEXT, name TEXT, rssi REAL, logged_at REAL DEFAULT CURRENT_TIMESTAMP NOT NULL)");
      db.execSQL("CREATE INDEX index_bluetooth_logged_at ON "+TABLE_NAME_BLUETOOTH_INFOS+" (logged_at);");
      db.execSQL(SqliteQueryConstructor.CREATETABLE_IFNOTEXIST(TABLE_NAME_WIFI_INFOS,
              new SqliteQueryConstructor.SqliteColumnDesc("id", "INTEGER", "primary key autoincrement", true),
              new SqliteQueryConstructor.SqliteColumnDesc("ssid", "TEXT", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("bssid", "TEXT", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("capabilities", "TEXT", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("channel_width", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("dual_channel", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("freq20", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("freq_center", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("freq_center_2", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("venue_name", "TEXT", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("rssi", "REAL", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("logged_at", "REAL", "DEFAULT CURRENT_TIMESTAMP", false)
      ));
      db.execSQL("CREATE INDEX index_wifi_logged_at ON "+TABLE_NAME_WIFI_INFOS+" (logged_at);");
      db.execSQL(SqliteQueryConstructor.CREATETABLE_IFNOTEXIST(TABLE_NAME_CELL_INFOS,
              new SqliteQueryConstructor.SqliteColumnDesc("id", "INTEGER", "primary key autoincrement", true),
              new SqliteQueryConstructor.SqliteColumnDesc("cid", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("lac", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("ss", "INTEGER", "", true),
              new SqliteQueryConstructor.SqliteColumnDesc("logged_at", "REAL", "DEFAULT CURRENT_TIMESTAMP", false)
      ));
      db.execSQL("CREATE INDEX index_cell_logged_at ON "+TABLE_NAME_CELL_INFOS+" (logged_at);");
      db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_NAME_SENSOR_INTERVAL_LABELS+"(id integer primary key autoincrement, start_date REAL NOT NULL, end_date REAL NOT NULL, labels TEXT);");
      db.execSQL("CREATE INDEX index_start_date ON "+TABLE_NAME_SENSOR_INTERVAL_LABELS+" (start_date);");
      db.execSQL("CREATE TABLE IF NOT EXISTS db_definitions (id integer primary key autoincrement, name TEXT, value TEXT, data_type TEXT);");
      db.execSQL("INSERT INTO db_definitions (name, value, data_type) VALUES ('" + KEY_DB_DEFS_SESSIONID + "', '" + this.sessionid +"', 'string');");
      db.execSQL("INSERT INTO db_definitions (name, value, data_type) VALUES ('" + KEY_DB_DEFS_EXPERIMENTID + "', '" + this.experimentId +"', 'string');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
  }
}
