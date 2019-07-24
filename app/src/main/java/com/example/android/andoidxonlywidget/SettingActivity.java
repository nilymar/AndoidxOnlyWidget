package com.example.android.andoidxonlywidget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import static com.example.android.andoidxonlywidget.AppConstants.GPS_REQUEST;
import static com.example.android.andoidxonlywidget.WidgetUpdateJobIntentService.ACTION_UPDATE_WEATHER_WIDGET_ONLINE;

public class SettingActivity extends AppCompatActivity {
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    String comp;
    int firstTime = 0;

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
            comp = extras.getString("provider");
            if (!extras.containsKey("from_main")) {
                assert actionBar != null;
                actionBar.setHomeButtonEnabled(false); // disable the button
                actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
                actionBar.setDisplayShowHomeEnabled(false); // remove the icon
            } else {
                // when the setting are accessed from main - i.e. not on installation
                firstTime = extras.getInt("from_main", 0);
                assert actionBar != null;
                actionBar.setHomeButtonEnabled(true); // disable the button
                actionBar.setDisplayHomeAsUpEnabled(true); // remove the left caret
                actionBar.setDisplayShowHomeEnabled(true); // remove the icon
            }
        }
        // put the setting fragment in its frame
        MySettingsFragment msf = new MySettingsFragment();
        Bundle bundle = new Bundle();
        // make sure the fragment gets the right widget id and provider
        bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        bundle.putString("provider", comp);
        msf.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, msf).commit();
    }

    private void updateWidget(){
        ServiceResultReceiver mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        WidgetUpdateJobIntentService.enqueueWork(this, mServiceResultReceiver,
                ACTION_UPDATE_WEATHER_WIDGET_ONLINE, appWidgetId, comp);
        // need to sent the widget id as result intent
        Intent resultValue = new Intent();
        // make sure the widget gets the right data
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        resultValue.putExtra("provider", comp);
        setResult(RESULT_OK, resultValue);
    }

    @Override
    public void onBackPressed() {
        // i.e. not the first time the widget is accessed - so go back to main
        if (firstTime == 1) {
            // didn't come from the widget - so go back to where it is defined - i.e. mainActivity
            final Intent upIntent = NavUtils.getParentActivityIntent(this);
            upIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            upIntent.putExtra("provider", comp);
            if (upIntent == null)
                super.onBackPressed();
            else
                //optionally add this flag to the intent: Intent.FLAG_ACTIVITY_SINGLE_TOP
                NavUtils.navigateUpTo(this, upIntent);
        } else {// if it is on widget installation - update the widget if setting was changed and close the app
            updateWidget();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // what happens when you press the home(back) button in the optionMenu
        if (item.getItemId() == android.R.id.home) {
            final Intent upIntent = NavUtils.getParentActivityIntent(this);
            upIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            upIntent.putExtra("provider", comp);
            if (upIntent == null)
                onBackPressed();
            else
                //optionally add this flag to the intent: Intent.FLAG_ACTIVITY_SINGLE_TOP
                NavUtils.navigateUpTo(this, upIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the GPS wasn't active and you try to fetch location from the device - a dialog opens
        if (requestCode == GPS_REQUEST) {
            // if the requestCode is GPS_REQUEST - go to the fragment onActivityResult
            MySettingsFragment fragment = (MySettingsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.settings_container);
            // go the the setting fragment onActivityResult if there was a GPS request
            fragment.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

