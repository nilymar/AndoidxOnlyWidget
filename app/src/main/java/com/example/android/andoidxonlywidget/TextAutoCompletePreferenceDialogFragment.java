package com.example.android.andoidxonlywidget;
/*
created this by adjusting the implementation of numberPickerPreferenceFragmentCompat in the library here:
https://github.com/h6ah4i/android-numberpickerprefcompat
 */
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import static com.example.android.andoidxonlywidget.AppConstants.SHARED_PREFERENCES;

public class TextAutoCompletePreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    // name of key for saving for state change (like rotating the screen)
    private static final String SAVE_STATE_VALUE = "TextAutoCompletePreferenceDialogFragment.value";
    private AutoCompleteTextView mAutoCompleteTextView;
    private String mValue;
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @NonNull
    public static TextAutoCompletePreferenceDialogFragment newInstance(@NonNull String key, int id) {
        final TextAutoCompletePreferenceDialogFragment fragment = new TextAutoCompletePreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            widgetId = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (savedInstanceState == null) {
            // if it is first run after installation - get the default value
            mValue = getTextAutoCompletePreference().getDefaultValue();
        } else
            // if not - there is a saved value
            mValue = savedInstanceState.getString(SAVE_STATE_VALUE);
    }

    // get the TextAutoCompletePreference instance
    private TextAutoCompletePreference getTextAutoCompletePreference() {
        return (TextAutoCompletePreference) getPreference();
    }

    // save the value
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_STATE_VALUE, mValue);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        // finding the numberPicker view
        mAutoCompleteTextView = view.findViewById(R.id.auto_complete);
        mAutoCompleteTextView.setSelectAllOnFocus(true);
        // the places that will auto appear when typing similar strings
        String[] PLACES = new String[0];
        JSONObject obj = null;
        ArrayList<String> places= new ArrayList<>();
        String nameCountry;
        JSONArray cities;
        try {
            obj = new JSONObject(loadJSONFromAsset());
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        if (obj!=null) {
            cities = null;
            // going through each object inside the base json object, to get the name of country and attach to citys
            for(Iterator key = obj.keys(); key.hasNext();){
                // get the name of current jsonObject - i.e. name of country
                nameCountry =  key.next().toString();
                try {
                    // get the array of cities in that country
                    cities = obj.getJSONArray(nameCountry);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(cities != null && cities.length() != 0){
                    for (int i=0; i<cities.length(); i++){
                        try {
                            String cityCountry = cities.getString(i) + ", " + nameCountry;
                            places.add(cityCountry);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    // when there are no cities in the country
                } else places.add(nameCountry);
            }
            PLACES = new String[places.size()];
            for (int i = 0, count = places.size(); i < count; i++) {
                PLACES[i]=places.get(i);
            }
        }
       if (PLACES.length!=0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, PLACES);
            mAutoCompleteTextView.setAdapter(adapter);
        }
        // throw an IllegalStateException if there is no NumberPicker view
        if (mAutoCompleteTextView == null) {
            throw new IllegalStateException("Dialog view must contain an auto_complete with id");
        }
        // set the value for the AutoCompleteTextView - i.e. name of last place saved
        mValue = restorePreferences(getResources().getString(R.string.settings_city_key));
        mAutoCompleteTextView.setText(mValue);
    }

    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = getContext().getAssets().open("locations1.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    // This method to store the custom preferences changes
    private void savePreferences(String key, String value) {
        Activity activity = this.getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES + widgetId,
                    Context.MODE_PRIVATE);
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
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES + widgetId,
                    Context.MODE_PRIVATE);
            if (myPreferences.contains(key))
                return myPreferences.getString(key, "");
            else return "";
        } else return "";
    }

    // what to do when the dialog is closed
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mAutoCompleteTextView.clearFocus();
            final String value = mAutoCompleteTextView.getText().toString();
            savePreferences(getResources().getString(R.string.settings_city_key),value);
            getTextAutoCompletePreference().setValue(value);
            getTextAutoCompletePreference().setSummary(value);
            if (getTextAutoCompletePreference().callChangeListener(value)) {
                getTextAutoCompletePreference().setValue(value);
            }
        }
    }
}
