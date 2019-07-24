package com.example.android.andoidxonlywidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.android.andoidxonlywidget.AppConstants.SHARED_PREFERENCES;
import static com.example.android.andoidxonlywidget.AppConstants.URI_SCHEME;

/**
 * Implementation of App Widget functionality.
 */
public class WeatherWidgetProvider extends AppWidgetProvider {
    public static final String LOG_TAG = WeatherWidgetProvider.class.getName();
    public static final String ACTION_UPDATE_WEATHER_WIDGET_ONLINE =
            "com.example.android.androidxonlywidget.action.update_weather_widget_online";

    // updating an app widget, using different views for narrow and wide widgets
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                Weather weather, int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views;
        // Creating the widget views, depending on its width
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        if (width < 200) {
            views = getSmallRemoteView(context, weather, appWidgetId); // for a narrow widget
        } else {
            views = getBigRemoteView(context, weather, appWidgetId); // for a wide widget
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the small mode widget
     * @param context The context
     * @param weather The current weather object with all its data
     * @return The RemoteViews for the small display mode widget
     */
    private static RemoteViews getSmallRemoteView(Context context, Weather weather, int widgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        // Set the click handler to open MainActivity and send the widget id and provider component name to the activity
        Intent intent = new Intent(context, MainActivity.class);
        ComponentName comp = new ComponentName(context.getPackageName(), WeatherWidgetProvider.class.getName());
        intent.putExtra("provider", comp.toString());
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        // the following 4 lines make sure each widget installment will get its own intent with its own data
        Uri data = Uri.withAppendedPath(
                Uri.parse(URI_SCHEME + "://widget/id/")
                , String.valueOf(widgetId));
        intent.setData(data);
        // the pendingIntent to open the activity
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        // initialize the values to put in the widget
        String time = "-";
        String widgetTemp = "-";
        int weatherIcon = R.drawable.a113;
        String city = "-";
        boolean isDay = true;
        // if no weather was fetched
        if (weather == null) {
            widgetTemp = "No data";
        } else {
            city = weather.getCity();
            long timeLong = getCreatedAtTime(weather.getLastUpdated());
            Date timeObject = new Date(timeLong);
            // the format of time on screen
            time = formatTime(timeObject);
            String temp = weather.getCurrentTemp();
            Float curTemp = Float.parseFloat(temp);
            int currTemp = Math.round(curTemp); // to get a rounded temp number
            widgetTemp = currTemp + " \u2103";
            String condition = weather.getConditions();
            if (weather.getIsDay() == 1) {
                String[] conditions = context.getResources().getStringArray(R.array.day_conditions);
                String[] icons = context.getResources().getStringArray(R.array.day_icons);
                isDay = true;
                for (int i = 0; i < conditions.length; i++) {
                    // setting the right condition icon to the weather condition
                    if (condition.equalsIgnoreCase(conditions[i])) {
                        weatherIcon = context.getResources().
                                getIdentifier(icons[i], null, context.getPackageName());
                        break; // if the icon was found - don't continue with the for loop
                    }
                }
            } else {
                isDay = false;
                String[] conditions = context.getResources().getStringArray(R.array.night_conditions);
                String[] icons = context.getResources().getStringArray(R.array.night_icons);
                for (int i = 0; i < conditions.length; i++) {
                    // setting the right condition icon to the weather condition
                    if (condition.equalsIgnoreCase(conditions[i])) {
                        weatherIcon = context.getResources().
                                getIdentifier(icons[i], null, context.getPackageName());
                        break; // if the icon was found - don't continue with the for loop
                    }
                }
            }
        }
        // setting the strings in the right places in the views
        views.setTextViewText(R.id.current_city, city);
        views.setTextViewText(R.id.current_time, time);
        views.setTextViewText(R.id.current_temp, widgetTemp);
        views.setImageViewResource(R.id.widget_image, weatherIcon);
        if (isDay) { // if it is day - the text is black and background light blue
            views.setTextColor(R.id.current_city, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.current_time, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.current_temp, context.getResources().getColor(R.color.text_color));
            views.setImageViewResource(R.id.widget_background, R.color.light_blue);
        } else { // if it is night - the text is white and background night dark
            views.setTextColor(R.id.current_city, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.current_temp, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.current_time, context.getResources().getColor(R.color.night_text));
            views.setImageViewResource(R.id.widget_background, R.color.night_dark);
        }
        // clicking anywhere in the widget - will activate the pending intent
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent);
        return views;
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the big mode widget
     * @param context The context
     * @param weather The weater object with all its data
     * @return The RemoteViews for the small display mode widget
     */
    private static RemoteViews getBigRemoteView(Context context, Weather weather, int widgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wide_view);
        // Set the click handler to open the MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        ComponentName comp = new ComponentName(context.getPackageName(), WeatherWidgetProvider.class.getName());
        intent.putExtra("provider", comp.toString());
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        Uri data = Uri.withAppendedPath(
                Uri.parse(URI_SCHEME + "://widget/id/")
                , String.valueOf(widgetId));
        intent.setData(data);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        String time = "-";
        String widgetTemp = "-";
        String feelLike = "-";
        int weatherIcon = R.drawable.a113;
        String city = "-";
        String humidity = "-";
        String wind = "-";
        String windDir = "-";
        boolean isDay = true;
        if (weather == null) {
            widgetTemp = "No data";
        } else {
            city = weather.getCity();
            humidity = weather.getHumidity();
            humidity += "%";
            // creating the feel like temp display
            feelLike = weather.getFeelsLikeC();
            Float feelL = Float.parseFloat(feelLike);
            int feelLi = Math.round(feelL); // to get a rounded temp number
            feelLike = feelLi + " \u2103";
            // adding kph to the wind string
            wind = weather.getWindKph() + " kph";
            windDir = weather.getWindDir();
            long timeLong = getCreatedAtTime(weather.getLastUpdated());
            Date timeObject = new Date(timeLong);
            // the format of time on screen
            time = formatTime(timeObject);
            widgetTemp = weather.getCurrentTemp();
            Float curTemp = Float.parseFloat(widgetTemp);
            int currTemp = Math.round(curTemp); // to get a rounded temp number
            widgetTemp = currTemp + " \u2103"; // adding celsius sign to the number
            String condition = weather.getConditions();
            if (weather.getIsDay() == 1) {
                String[] conditions = context.getResources().getStringArray(R.array.day_conditions);
                String[] icons = context.getResources().getStringArray(R.array.day_icons);
                isDay = true;
                for (int i = 0; i < conditions.length; i++) {
                    // setting the right condition icon to the weather condition
                    if (condition.equalsIgnoreCase(conditions[i])) {
                        weatherIcon = context.getResources().
                                getIdentifier(icons[i], null, context.getPackageName());
                        break; // if the icon was found - don't continue with the for loop
                    }
                }
            } else {
                isDay = false;
                String[] conditions = context.getResources().getStringArray(R.array.night_conditions);
                String[] icons = context.getResources().getStringArray(R.array.night_icons);
                for (int i = 0; i < conditions.length; i++) {
                    // setting the right condition icon to the weather condition
                    if (condition.equalsIgnoreCase(conditions[i])) {
                        weatherIcon = context.getResources().
                                getIdentifier(icons[i], null, context.getPackageName());
                        break; // if the icon was found - don't continue with the for loop
                    }
                }
            }
        }
        views.setTextViewText(R.id.current_city, city);
        views.setTextViewText(R.id.current_time, time);
        views.setTextViewText(R.id.current_temp, widgetTemp);
        views.setTextViewText(R.id.feel_like, feelLike);
        views.setTextViewText(R.id.humidity, humidity);
        views.setTextViewText(R.id.wind, wind);
        views.setTextViewText(R.id.wind_dir, windDir);
        views.setImageViewResource(R.id.widget_image, weatherIcon);
        if (isDay) { // setting the right colors for day display
            views.setTextColor(R.id.current_city, context.getResources().getColor(R.color.colorPrimary));
            views.setTextColor(R.id.current_time, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.current_temp, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.feel_like, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.humidity, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.wind, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.wind_dir, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.current_temp_title, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.current_time_title, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.feel_like_title, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.humidity_title, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.wind_title, context.getResources().getColor(R.color.text_color));
            views.setTextColor(R.id.wind_dir_title, context.getResources().getColor(R.color.text_color));
            views.setImageViewResource(R.id.widget_background, R.color.light_blue);
        } else { // setting the right colors for night display
            views.setTextColor(R.id.current_city, context.getResources().getColor(R.color.background_main));
            views.setTextColor(R.id.current_temp, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.current_time, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.feel_like, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.humidity, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.wind, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.wind_dir, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.current_temp_title, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.current_time_title, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.feel_like_title, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.humidity_title, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.wind_title, context.getResources().getColor(R.color.night_text));
            views.setTextColor(R.id.wind_dir_title, context.getResources().getColor(R.color.night_text));
            views.setImageViewResource(R.id.widget_background, R.color.night_dark);
        }
        // clicking anywhere in the widget - will activate the pending intent
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent);
        return views;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ServiceResultReceiver mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        ComponentName comp = new ComponentName(context.getPackageName(), WeatherWidgetProvider.class.getName());
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            Log.i(LOG_TAG, "onUpdate widget id is: " + appWidgetId);
            WidgetUpdateJobIntentService.enqueueWork(context, mServiceResultReceiver,
                    ACTION_UPDATE_WEATHER_WIDGET_ONLINE, appWidgetId, comp.toString());
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        RemoteViews defaultViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        ComponentName comp = new ComponentName(context.getPackageName(), WeatherWidgetProvider.class.getName());
        int[] appWidgetIds = mgr.getAppWidgetIds(comp);
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            Intent idefault = new Intent(context, MainActivity.class);
            // putting the name of provider and the id of the widget as extra to pass to main
            idefault.putExtra("provider", comp.toString());
            idefault.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            Uri data = Uri.withAppendedPath(
                    Uri.parse(URI_SCHEME + "://widget/id/")
                    , String.valueOf(appWidgetIds[i]));
            idefault.setData(data);
            PendingIntent defaultpendingIntent = PendingIntent.getActivity(context, 0, idefault,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            defaultViews.setOnClickPendingIntent(R.id.widget_background, defaultpendingIntent);
            mgr.updateAppWidget(comp, defaultViews);
        }
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(LOG_TAG, "onDisabled activated");
    }

    // handle updating widget when there are changes in size
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        ServiceResultReceiver mServiceResultReceiver = new ServiceResultReceiver(new Handler());
        ComponentName comp = new ComponentName(context.getPackageName(), WeatherWidgetProvider.class.getName());
        WidgetUpdateJobIntentService.enqueueWork(context, mServiceResultReceiver,
                ACTION_UPDATE_WEATHER_WIDGET_ONLINE, appWidgetId, comp.toString());
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    // when a widget is deleted from home screen - delete the sharedPreferences file with the widget data
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            String fileLocation = context.getFilesDir().getParent() + File.separator + "shared_prefs/";
            String fileName = SHARED_PREFERENCES + appWidgetIds[i] + ".xml";
            String filePath = fileLocation + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) Log.i(LOG_TAG, "onDeleted deleted file " +
                        filePath);
                else Log.i(LOG_TAG, "onDeleted didn't delete the file");
            }
        }
        super.onDeleted(context, appWidgetIds);
    }

    // Return the formatted time string (i.e. "4:30 PM") from a Date object
    private static String formatTime(Date dateObject) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        return timeFormat.format(dateObject);
    }

    public static long getCreatedAtTime(String createdAt) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date createDate = null;
        try {
            createDate = formatter.parse(createdAt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return createDate.getTime();
    }
}



