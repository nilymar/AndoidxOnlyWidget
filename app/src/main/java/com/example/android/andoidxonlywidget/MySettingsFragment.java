package com.example.android.andoidxonlywidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import pub.devrel.easypermissions.EasyPermissions;
import static com.example.android.andoidxonlywidget.AppConstants.GPS_REQUEST;
import static com.example.android.andoidxonlywidget.AppConstants.SHARED_PREFERENCES;


public class MySettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener,
        EasyPermissions.PermissionCallbacks {
    private static final String FRAGMENT_TAG = "Setting_fragment";
    private boolean gpsRequestStatus = false;
    // Constant used in the location settings dialog.
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private double wayLatitude = 0.0;
    private double wayLongitude = 0.0;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    // to store the device location data
    private final String[] place = {""};
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SwitchPreference switchPreference;
    private TextAutoCompletePreference autoCompletePreference;
    private NumberPickerPreference numberPickerPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            widgetId = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        switchPreference = findPreference(
                getResources().getString(R.string.settings_switch_key));
        if (switchPreference != null) {
            bindPreferenceSummaryToValue(switchPreference);
        }
        autoCompletePreference = findPreference
                (getResources().getString(R.string.settings_city_key));
        if (autoCompletePreference != null) {
            bindPreferenceSummaryToValue(autoCompletePreference);
            autoCompletePreference.setSelectable(true);
        }
        numberPickerPreference = findPreference
                (getResources().getString(R.string.settings_forecast_days_key));
        if (numberPickerPreference != null) {
            bindPreferenceSummaryToValue(numberPickerPreference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof NumberPickerPreference) {
            String chosenNumber = newValue.toString();
            if (newValue.toString().isEmpty()) {
                int defaultValue = numberPickerPreference.getDefaultValue();
                numberPickerPreference.setSummary(defaultValue);
                savePreferences(preference.getKey(), String.valueOf(defaultValue));
            } else {
                numberPickerPreference.setSummary(chosenNumber);
                savePreferences(preference.getKey(), chosenNumber);
            }
        } else if (preference instanceof SwitchPreference) {
            place[0] = "";
            if (!switchPreference.isChecked()) {
                fusedLocationClient = LocationServices.
                        getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(10 * 1000); // 10 seconds
                locationRequest.setFastestInterval(5 * 1000); // 5 seconds
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            Log.i(FRAGMENT_TAG, "location result is null");
                            switchPreference.setChecked(false);
                            autoCompletePreference.setEnabled(true);
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                fetchLocationData(location);
                            }
                            if (fusedLocationClient != null) {
                                fusedLocationClient.removeLocationUpdates(locationCallback);
                            }
                        }
                    }
                };
                // check the gps status, also creates the dialog if gps not enables
                new GpsUtils(Objects.requireNonNull(getContext())).turnGPSOn(new GpsUtils.onGpsListener() {
                    @Override
                    public void gpsStatus(boolean isGPSEnable) {
                        // GPS request is success/failure
                        gpsRequestStatus = isGPSEnable;
                    }
                });

                if (!gpsRequestStatus) {
                    Toast.makeText(getContext(), "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    Log.i(FRAGMENT_TAG, "gpsRequestStatus is false");
                    switchPreference.setChecked(false);
                    autoCompletePreference.setEnabled(true);
                } else {
                    Log.i(FRAGMENT_TAG, "gpsRequestStatus is true");
                    getLocation();
                }
            } else
                autoCompletePreference.setEnabled(true);
        } else if (preference instanceof TextAutoCompletePreference) {
            String chosenCountry = newValue.toString();
            if (newValue.toString().isEmpty()) {
                String defaultValue = autoCompletePreference.getDefaultValue();
                autoCompletePreference.setSummary(defaultValue);
                savePreferences(preference.getKey(), defaultValue);
            } else {
                autoCompletePreference.setSummary(chosenCountry);
                savePreferences(preference.getKey(), chosenCountry);
            }
        }
        return true;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        if (preference instanceof TextAutoCompletePreference) { // for the name
            String preferenceString = restorePreferences(preference.getKey());
            if ((preferenceString == null || preferenceString.isEmpty())) {
                // when there is no saved data - put the default value
                onPreferenceChange(preference, autoCompletePreference.getDefaultValue());
            } else {
                onPreferenceChange(preference, preferenceString);
            }
        } else if (preference instanceof SwitchPreference) {
            // when the setting screen opens - always set the switch preference to not checked
            switchPreference.setChecked(false);
        } else if (preference instanceof NumberPickerPreference) {
            String preferenceString = restorePreferences(preference.getKey());
            if ((preferenceString == null || preferenceString.isEmpty())) {
                // when there is no saved data - put the default value
                onPreferenceChange(preference, numberPickerPreference.getDefaultValue());
            } else {
                onPreferenceChange(preference, preferenceString);
            }
        }
    }

    // only relevant to dialog preferences - not for the switch one
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // check if dialog is already showing
        assert getFragmentManager() != null;
        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) != null) {
            return;
        }
        final DialogFragment f;
        if (preference instanceof NumberPickerPreference) {
            f = NumberPickerPreferenceDialogFragment.newInstance(preference.getKey(), widgetId);
        } else if (preference instanceof TextAutoCompletePreference) {
            f = TextAutoCompletePreferenceDialogFragment.newInstance(preference.getKey(), widgetId);
        } else
            f = null;
        if (f != null) {
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    // This method to store the custom preferences changes
    private void savePreferences(String key, String value) {
        SharedPreferences myPreferences = Objects.requireNonNull(this.getActivity()).getSharedPreferences(
                SHARED_PREFERENCES + widgetId, Context.MODE_PRIVATE);
        SharedPreferences.Editor myEditor = myPreferences.edit();
        myEditor.putString(key, value);
        myEditor.apply();
    }

    // This method to restore the custom preferences data
    private String restorePreferences(String key) {
        SharedPreferences myPreferences = Objects.requireNonNull(this.getActivity()).getSharedPreferences(
                SHARED_PREFERENCES + widgetId, Context.MODE_PRIVATE);
        if (myPreferences.contains(key))
            return myPreferences.getString(key, "");
        else return "";
    }

    //from here - what happen when the switchPreference is checked - i.e. - location is taken from device
    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (!EasyPermissions.hasPermissions(Objects.requireNonNull(getContext()),
                Manifest.permission.ACCESS_FINE_LOCATION)
                && !EasyPermissions.hasPermissions(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            EasyPermissions.requestPermissions(this, "Please grant the location permission",
                    REQUEST_LOCATION_PERMISSION, perms);
        } else {
            //  important!!! - when you switch the GPS on and off - stops getting location data unless you
            // add the following line - i.e - request location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            fusedLocationClient.getLastLocation().
                    addOnSuccessListener(Objects.requireNonNull(getActivity()), location -> {
                        if (location != null) {
                            fetchLocationData(location);
                        } else {
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                    locationCallback, null);
                        }
                    });
        }
    }

    private void fetchLocationData(Location location) {
        wayLatitude = location.getLatitude();
        wayLongitude = location.getLongitude();
        try {
            Geocoder geo = new Geocoder(getContext(), Locale.US);
            List<Address> addresses = geo.getFromLocation
                    (wayLatitude, wayLongitude, 1);
            if (addresses.isEmpty()) {
                Log.i(FRAGMENT_TAG, "Waiting for Location");
            } else {
                String city = addresses.get(0).getLocality();
                String country = addresses.get(0).getCountryName();
                place[0] = city + ", " + country;
                if (!place[0].equals("")) {
                    // how to set value ->
                    autoCompletePreference.setSummary(place[0]);
                    autoCompletePreference.setEnabled(false);
                    savePreferences(getResources().getString(R.string.settings_city_key), place[0]);
                } else {
                    Toast.makeText(getContext(), "Device location not found", Toast.LENGTH_SHORT).show();
                    autoCompletePreference.setEnabled(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // getFromLocation() may sometimes fail
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        Toast.makeText(getContext(), "Location permission approved", Toast.LENGTH_SHORT).show();
        if (gpsRequestStatus) getLocation();
        else {
            switchPreference.setChecked(false);
            Toast.makeText(getContext(), "Location permission approved but GPS is off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        switchPreference.setChecked(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // goes back from the activity and do what needed when it is a request to enable device gps
            case GPS_REQUEST:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(FRAGMENT_TAG, "User agreed to make required location settings changes.");
                        gpsRequestStatus = true;
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(FRAGMENT_TAG, "User chose not to make required location settings changes.");
                        autoCompletePreference.setEnabled(true);
                        switchPreference.setChecked(false);
                        break;
                }
                break;
        }
    }
}
