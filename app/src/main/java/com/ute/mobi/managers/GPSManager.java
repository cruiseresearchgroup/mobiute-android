package com.ute.mobi.managers;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by jonathanliono on 9/01/15.
 */
public class GPSManager extends Service implements LocManager {
  private final Context mContext;

  //flag for GPS Status
  boolean isGPSEnabled = false;

  //flag for network status
  boolean isNetworkEnabled = false;

  boolean canGetLocation = false;

  Location location;
  //double latitude;
  //double longitude;

  //The minimum distance to change updates in metters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 meters

  //The minimum time between updates in milliseconds
  //private static final long MIN_TIME_BW_UPDATES = 2 * 1000; //5 seconds

  //Declaring a Location Manager
  protected LocationManager locationManager;
  private Location networkLocation;
  private Location gpsLocation;
  private double lastGpsLocationMillis;
  private long MIN_TIME_BW_UPDATES;

  private NetworkLocationListener networkLocationListener;
  private GPSLocationListener gpsLocationListener;

  public GPSManager(Context context, long millisec) {
    this.mContext = context;
    this.networkLocationListener = new NetworkLocationListener(this);
    this.gpsLocationListener = new GPSLocationListener(this);
    this.MIN_TIME_BW_UPDATES = millisec;
    getLocation();
  }

  public Location getLocation() {
    try {
      locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

      //getting GPS status
      isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

      //getting network status
      //isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!isGPSEnabled && !isNetworkEnabled) {
        // no network provider is enabled
      } else {
        this.canGetLocation = true;

        //if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled) {
          if(checkPermission(this.mContext) == false)
            return null;

          locationManager.requestLocationUpdates(
                  LocationManager.GPS_PROVIDER,
                  MIN_TIME_BW_UPDATES,
                  MIN_DISTANCE_CHANGE_FOR_UPDATES, this.gpsLocationListener);

          if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            this.gpsLocationListener.updateCoordinates();
          }
        }

        //get location from Network Provider
        if (isNetworkEnabled) {
          long currentMillis = System.currentTimeMillis();
          if(this.location == null
                  || (this.location != null && (currentMillis - this.lastGpsLocationMillis) > (3*MIN_TIME_BW_UPDATES))) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this.networkLocationListener);

            if (locationManager != null) {
              location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
              this.networkLocationListener.updateCoordinates();
            }
          }
        }
      }
    } catch (Exception e) {
      //e.printStackTrace();
      Log.e("Error : Location", "Impossible to connect to LocationManager", e);
    }

    return this.location;
  }

  /**
   * Stop using GPS listener
   * Calling this function will stop using GPS in your app
   */

  public void stopUsingGPS() {
    if (checkPermission(this.mContext) && locationManager != null) {
      locationManager.removeUpdates(this.networkLocationListener);
      locationManager.removeUpdates(this.gpsLocationListener);
    }
  }

  /**
   * Function to get GPS speed
   */
  public double getSpeed() {
    if (this.location != null) {
      return this.location.getSpeed();
    }

    return 0;
  }

  /**
   * Function to check GPS/wifi enabled
   */
  public boolean canGetLocation() {
    return this.canGetLocation;
  }

  /**
   * Function to show settings alert dialog
   */
    /*public void showSettingsAlert()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        //Setting Dialog Title
        alertDialog.setTitle(R.string.GPSAlertDialogTitle);

        //Setting Dialog Message
        alertDialog.setMessage(R.string.GPSAlertDialogMessage);

        //On Pressing Setting button
        alertDialog.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        //On pressing cancel button
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }*/

  /**
   * Get list of address by latitude and longitude
   *
   * @return null or List<Address>
   */
  public List<Address> getGeocoderAddress(Context context) {
    if (this.location != null) {
      Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
      try {
        List<Address> addresses = geocoder.getFromLocation(this.location.getLatitude(), this.location.getLongitude(), 1);
        return addresses;
      } catch (IOException e) {
        //e.printStackTrace();
        Log.e("Error : Geocoder", "Impossible to connect to Geocoder", e);
      }
    }

    return null;
  }

  /**
   * Try to get AddressLine
   *
   * @return null or addressLine
   */
  public String getAddressLine(Context context) {
    List<Address> addresses = getGeocoderAddress(context);
    if (addresses != null && addresses.size() > 0) {
      Address address = addresses.get(0);
      String addressLine = address.getAddressLine(0);

      return addressLine;
    } else {
      return null;
    }
  }

  /**
   * Try to get Locality
   *
   * @return null or locality
   */
  public String getLocality(Context context) {
    List<Address> addresses = getGeocoderAddress(context);
    if (addresses != null && addresses.size() > 0) {
      Address address = addresses.get(0);
      String locality = address.getLocality();

      return locality;
    } else {
      return null;
    }
  }

  /**
   * Try to get Postal Code
   *
   * @return null or postalCode
   */
  public String getPostalCode(Context context) {
    List<Address> addresses = getGeocoderAddress(context);
    if (addresses != null && addresses.size() > 0) {
      Address address = addresses.get(0);
      String postalCode = address.getPostalCode();

      return postalCode;
    } else {
      return null;
    }
  }

  /**
   * Try to get CountryName
   *
   * @return null or postalCode
   */
  public String getCountryName(Context context) {
    List<Address> addresses = getGeocoderAddress(context);
    if (addresses != null && addresses.size() > 0) {
      Address address = addresses.get(0);
      String countryName = address.getCountryName();

      return countryName;
    } else {
      return null;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void setNetworkLocation(Location location) {
    this.networkLocation = location;
  }

  @Override
  public void setGPSLocation(Location location) {
    this.gpsLocation = location;
    this.lastGpsLocationMillis = System.currentTimeMillis();
  }

  private class NetworkLocationListener implements LocationListener {

    private Location networkLocation;
    private LocManager locManager;
    double networkLocationLatitude;
    double networkLocationLongitude;

    public NetworkLocationListener(LocManager locManager) {
      this.locManager = locManager;
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
      if (this.networkLocation != null) {
        this.networkLocationLatitude = this.networkLocation.getLatitude();
      }

      return networkLocationLatitude;
    }

    public void updateCoordinates() {
      if (this.networkLocation != null) {
        this.networkLocationLatitude = this.networkLocation.getLatitude();
        this.networkLocationLongitude = this.networkLocation.getLongitude();
      }
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
      if (this.networkLocation != null) {
        this.networkLocationLongitude = this.networkLocation.getLongitude();
      }

      return this.networkLocationLongitude;
    }

    @Override
    public void onLocationChanged(Location location) {
      this.networkLocation = location;
      getLatitude();
      getLongitude();
      this.locManager.setNetworkLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
  }

  private class GPSLocationListener implements LocationListener {

    private Location gpsLocation;
    private LocManager locManager;
    double gpsLocationLatitude;
    double gpsLocationLongitude;

    public GPSLocationListener(LocManager locManager) {
      this.locManager = locManager;
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
      if (this.gpsLocation != null) {
        this.gpsLocationLatitude = this.gpsLocation.getLatitude();
      }

      return gpsLocationLatitude;
    }

    public void updateCoordinates() {
      if (this.gpsLocation != null) {
        this.gpsLocationLatitude = this.gpsLocation.getLatitude();
        this.gpsLocationLongitude = this.gpsLocation.getLongitude();
      }
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
      if (this.gpsLocation != null) {
        this.gpsLocationLongitude = this.gpsLocation.getLongitude();
      }

      return this.gpsLocationLongitude;
    }

    @Override
    public void onLocationChanged(Location location) {
      this.gpsLocation = location;
      getLatitude();
      getLongitude();
      this.locManager.setGPSLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
  }

  public static boolean checkPermission(final Context context) {
    return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }
}
