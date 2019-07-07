package com.example.android.andoidxonlywidget;


import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import static com.example.android.andoidxonlywidget.WidgetUpdateJobIntentService.ACTION_UPDATE_WEATHER_WIDGET_ONLINE;

public class SettingActivity extends AppCompatActivity {
    private static final String SHARED_PREFERENCES = "androidxonlywidget"; // name for sharedPreferences location
    long appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setResult(RESULT_CANCELED);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        ActionBar actionBar = getSupportActionBar();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            // hide the home (back to mainActivity) button when the setting screen pop on new widget installment
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(false); // disable the button
                actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
                actionBar.setDisplayShowHomeEnabled(false); // remove the icon
            }

        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MySettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            // didn't come from the widget - so go back to where it is defined - i.e. mainActivity
            NavUtils.navigateUpFromSameTask(this);
        else {
            // It is the responsibility of the configuration activity to update the app widget
            ServiceResultReceiver mServiceResultReceiver = new ServiceResultReceiver(new Handler());
            WidgetUpdateJobIntentService.enqueueWork(this, mServiceResultReceiver,
                    ACTION_UPDATE_WEATHER_WIDGET_ONLINE);
            // need to sent the widget id as result intent
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            // instead of going to mainActivity - close the app
            finish();
        }
    }

}
