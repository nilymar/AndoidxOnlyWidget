package com.example.android.andoidxonlywidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import pub.devrel.easypermissions.EasyPermissions;
import static com.example.android.andoidxonlywidget.AppConstants.GPS_REQUEST;


public class MySettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener,
        EasyPermissions.PermissionCallbacks {
    // name for sharedPreferences location
    private static final String SHARED_PREFERENCES = "androidxonlywidget";
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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Preference switchPreference = findPreference(
                getResources().getString(R.string.settings_switch_key));
        if (switchPreference != null) {
            bindPreferenceSummaryToValue(switchPreference);
        }
        Preference autoCompletePreference = findPreference
                (getResources().getString(R.string.settings_city_key));
        if (autoCompletePreference != null) {
            bindPreferenceSummaryToValue(autoCompletePreference);
            autoCompletePreference.setSelectable(true);
        }
        Preference numberPickerPreference = findPreference
                (getResources().getString(R.string.settings_forecast_days_key));
        if (numberPickerPreference != null) {
            bindPreferenceSummaryToValue(numberPickerPreference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof NumberPickerPreference) {
            String chosenNumber = newValue.toString();
            savePreferences(preference.getKey(), chosenNumber);
            preference.setSummary(chosenNumber);
            if (newValue.toString().isEmpty()) {
                int defaultValue = ((NumberPickerPreference) preference).getDefaultValue();
                preference.setSummary(defaultValue);
                ((NumberPickerPreference) preference).setValue(defaultValue);
            } else {
                preference.setSummary(chosenNumber);
                ((NumberPickerPreference) preference).setValue(Integer.parseInt(chosenNumber));
            }
        } else if (preference instanceof SwitchPreference) {
            place[0] = "";
            if (!((SwitchPreference) preference).isChecked()) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(10 * 1000); // 10 seconds
                locationRequest.setFastestInterval(5 * 1000); // 5 seconds
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            Log.i(FRAGMENT_TAG, "location result is null");
                            ((SwitchPreference)preference).setChecked(false);
                            getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                                    setEnabled(true);
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                wayLatitude = location.getLatitude();
                                wayLongitude = location.getLongitude();
                                try {
                                    Geocoder geo = new Geocoder(getContext(), Locale.US);
                                    List<Address> addresses = geo.getFromLocation
                                            (wayLatitude, wayLongitude, 1);
                                    if (addresses.isEmpty()) {
                                        return;
                                    } else {
                                        String city = addresses.get(0).getLocality();
                                        String country = addresses.get(0).getCountryName();
                                        place[0] = city + ", " + country;
                                        getPreferenceScreen().findPreference(getResources().
                                                getString(R.string.settings_city_key)).
                                                setSummary(place[0]);
                                        getPreferenceScreen().findPreference(getResources().
                                                getString(R.string.settings_city_key)).
                                                setEnabled(false);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); // getFromLocation() may sometimes fail
                                }
                            }
                            if (fusedLocationClient != null) {
                                fusedLocationClient.removeLocationUpdates(locationCallback);
                            }
                        }
                    }
                };
                // check the gps status, also creates the dialog if gps not enables
                new GpsUtils(getContext()).turnGPSOn(new GpsUtils.onGpsListener() {
                    @Override
                    public void gpsStatus(boolean isGPSEnable) {
                        // GPS request is success/failure
                        gpsRequestStatus = isGPSEnable;
                    }
                });

                if (!gpsRequestStatus) {
                    Toast.makeText(getContext(), "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    Log.i(FRAGMENT_TAG, "gpsRequestStatus is false");
                    ((SwitchPreference)preference).setChecked(false);
                    getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                            setEnabled(true);
                } else {
                    Log.i(FRAGMENT_TAG, "gpsRequestStatus is true");
                    getLocation();
                }
            } else
                getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                        setEnabled(true);
        } else if (preference instanceof TextAutoCompletePreference) {
            String chosenCountry = newValue.toString();
            savePreferences(preference.getKey(), chosenCountry);
            if (newValue.toString().isEmpty()) {
                String defaultValue = ((TextAutoCompletePreference) preference).getDefaultValue();
                preference.setSummary(defaultValue);
                ((TextAutoCompletePreference) preference).setValue(defaultValue);
            } else {
                preference.setSummary(chosenCountry);
                ((TextAutoCompletePreference) preference).setValue(chosenCountry);
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
                onPreferenceChange(preference, ((TextAutoCompletePreference) preference).getDefaultValue());
            } else {
                onPreferenceChange(preference, preferenceString);
            }
        } else if (preference instanceof SwitchPreference) {
            // when the setting screen opens - always set the switch preference to not checked
            ((SwitchPreference) preference).setChecked(false);
        } else if (preference instanceof NumberPickerPreference) {
            String preferenceString = restorePreferences(preference.getKey());
            if ((preferenceString == null || preferenceString.isEmpty())) {
                // when there is no saved data - put the default value
                onPreferenceChange(preference, ((NumberPickerPreference) preference).getDefaultValue());
            } else {
                onPreferenceChange(preference, preferenceString);
            }
        }
    }

    // only relevant to dialog preferences - not for the switch one
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG) != null) {
            return;
        }
        final DialogFragment f;
        if (preference instanceof NumberPickerPreference) {
            f = NumberPickerPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof TextAutoCompletePreference) {
            f = TextAutoCompletePreferenceDialogFragment.newInstance(preference.getKey());
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
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor myEditor = myPreferences.edit();
            myEditor.putString(key, value);
            myEditor.apply();
        }
    }

    // This method to restore the custom preferences data
    private String restorePreferences(String key) {
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            if (myPreferences.contains(key))
                return myPreferences.getString(key, "");
            else return "";
        } else return "";
    }

    //from here - what happen when the switchPreference is checked - i.e. - location is taken from device
    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (!EasyPermissions.hasPermissions(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                && !EasyPermissions.hasPermissions(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            EasyPermissions.requestPermissions(this, "Please grant the location permission",
                    REQUEST_LOCATION_PERMISSION, perms);
        } else {
            //  important!!! - when you switch the GPS on and off - stops getting location data unless you
            // add the following line - i.e - request location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            fusedLocationClient.getLastLocation().
                    addOnSuccessListener(this.getActivity(), location -> {
                        if (location != null) {
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
                                    Log.i(FRAGMENT_TAG, "the device city is " + city);
                                    String country = addresses.get(0).getCountryName();
                                    Log.i(FRAGMENT_TAG, "the device country is " + country);
                                    place[0] = city + ", " + country;
                                    if (!place[0].equals("")) {
                                        Log.i(FRAGMENT_TAG, place[0]);
                                        // how to set value ->
                                        savePreferences(getResources().getString(R.string.settings_city_key), place[0]);
                                        getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                                                setSummary(place[0]);
                                        getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                                                setEnabled(false);
                                    } else {
                                        Toast.makeText(getContext(), "Device location not found", Toast.LENGTH_SHORT).show();
                                        getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                                                setEnabled(true);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace(); // getFromLocation() may sometimes fail
                            }
                        } else {
                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        }
                    });
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
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
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
                        getPreferenceScreen().findPreference(getResources().getString(R.string.settings_city_key)).
                                setEnabled(true);
                        SwitchPreference sp = getPreferenceScreen().findPreference(getResources().
                                getString(R.string.settings_switch_key));
                        sp.setChecked(false);
                        break;
                }
                break;
        }
    }

}
