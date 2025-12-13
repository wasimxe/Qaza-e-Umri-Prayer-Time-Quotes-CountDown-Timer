package com.tanxe.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Countdown extends AppWidgetProvider {
    private static Context context;

    // Default text sizes (in sp)
    private static final float DEFAULT_NUMBER_SIZE = 32f;
    private static final float DEFAULT_LABEL_SIZE = 12f;
    private static final float DEFAULT_TITLE_SIZE = 14f;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateCountdownWidget(context, appWidgetManager, appWidgetId, width, height);
        }
        updateAllWidgets(this.context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        updateCountdownWidget(context, appWidgetManager, appWidgetId, width, height);
    }

    private static float getUserFontScale(SharedPreferences preferences) {
        // fontsize ranges from 0-100, default 50
        // Scale factor: 0 -> 0.6x, 50 -> 1.0x, 100 -> 1.6x
        int fontSizeProgress = preferences.getInt("fontsize", 50);
        return 0.6f + (fontSizeProgress * 0.01f);
    }
    private static String getFormattedCountdownTime(long targetTimeMillis) {

        long currentTimeMillis = System.currentTimeMillis();
        long timeDifference = targetTimeMillis - currentTimeMillis;

        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long years = days / 365;
        days %= 365;
        hours %= 24;
        minutes %= 60;

        StringBuilder countdownTimeBuilder = new StringBuilder();
        if (years > 0) {
            countdownTimeBuilder.append(years).append("Y ");
        }
        if (days > 0) {
            countdownTimeBuilder.append(days).append("D ");
        }
        if (hours > 0) {
            countdownTimeBuilder.append(hours).append("H ");
        }
        if (minutes > 0) {
            countdownTimeBuilder.append(minutes).append("M ");
        }

        return countdownTimeBuilder.toString();
    }
    private static void setTextColorForAllTextViews(Context context, int appWidgetId, RemoteViews views, View rootView, int textColorValue) {
        if (rootView instanceof TextView) {
            int viewId = rootView.getId();
            if (viewId != View.NO_ID) {
                views.setTextColor(viewId, textColorValue);
            }
        } else if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = viewGroup.getChildAt(i);
                setTextColorForAllTextViews(context, appWidgetId, views, childView, textColorValue);
            }
        }
    }
    static void updateCountdownWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int widgetWidth, int widgetHeight) {

        Log.d("wasim", "countcalled width=" + widgetWidth + " height=" + widgetHeight);
        SharedPreferences preferences = context.getSharedPreferences("CountdownPrefs_" + appWidgetId, Context.MODE_PRIVATE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.countdown_widget);

        // Calculate text sizes based on user preference
        float fontScale = getUserFontScale(preferences);
        float numberSize = DEFAULT_NUMBER_SIZE * fontScale;
        float labelSize = DEFAULT_LABEL_SIZE * fontScale;
        float titleSize = DEFAULT_TITLE_SIZE * fontScale;

        // Apply text sizes
        views.setFloat(R.id.yearsTextView, "setTextSize", numberSize);
        views.setFloat(R.id.daysTextView, "setTextSize", numberSize);
        views.setFloat(R.id.hoursTextView, "setTextSize", numberSize);
        views.setFloat(R.id.minutesTextView, "setTextSize", numberSize);
        views.setFloat(R.id.y, "setTextSize", labelSize);
        views.setFloat(R.id.d, "setTextSize", labelSize);
        views.setFloat(R.id.h, "setTextSize", labelSize);
        views.setFloat(R.id.m, "setTextSize", labelSize);
        views.setFloat(R.id.widgetTitleTextView, "setTextSize", titleSize);
        views.setFloat(R.id.msg, "setTextSize", labelSize);

        // Set click event on widget
        Intent configIntent = new Intent(context, ConfigCountdown.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.centerLayout, configPendingIntent);

        // Set title from shared preferences
        String title = preferences.getString("title", "");
        if (!title.isEmpty()) {
            views.setTextViewText(R.id.widgetTitleTextView, title);
            views.setViewVisibility(R.id.widgetTitleTextView, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widgetTitleTextView, View.GONE);
        }

        long targetTimeMillis = preferences.getLong("targetTimeMillis", 1703962800 * 1000);
        boolean repeat = preferences.getBoolean("repeat", false);
        boolean hideDays = preferences.getBoolean("hidedays", false);
        boolean hideHours = preferences.getBoolean("hidehours", false);
        boolean hideMins = preferences.getBoolean("hidemins", false);

        if (repeat && System.currentTimeMillis() >= targetTimeMillis) {
            long originalTimeDifference = preferences.getLong("originalTimeDifference", 0);

            targetTimeMillis = System.currentTimeMillis() + originalTimeDifference;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong("targetTimeMillis", targetTimeMillis);
            editor.apply();
        }

        // Set the targetTime from shared preferences
        String formattedTimeLeft = getFormattedCountdownTime(targetTimeMillis);


        boolean allZero = true;
        Matcher matcher = Pattern.compile("(\\d+)Y").matcher(formattedTimeLeft);
        if (matcher.find()) {
            allZero = false;
            views.setTextViewText(R.id.yearsTextView, matcher.group(1));
            views.setViewVisibility(R.id.yearsTextView, View.VISIBLE);
            views.setViewVisibility(R.id.y, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.yearsTextView, "");
            views.setViewVisibility(R.id.yearsTextView, View.GONE);
            views.setViewVisibility(R.id.y, View.GONE);
        }

        matcher = Pattern.compile("(\\d+)D").matcher(formattedTimeLeft);
        if (matcher.find() && !hideDays) {
            allZero = false;
            views.setTextViewText(R.id.daysTextView, matcher.group(1));
            views.setViewVisibility(R.id.daysTextView, View.VISIBLE);
            views.setViewVisibility(R.id.d, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.daysTextView, "");
            views.setViewVisibility(R.id.daysTextView, View.GONE);
            views.setViewVisibility(R.id.d, View.GONE);
        }

        matcher = Pattern.compile("(\\d+)H").matcher(formattedTimeLeft);
        if (matcher.find() && !hideHours) {
            allZero = false;
            views.setTextViewText(R.id.hoursTextView, matcher.group(1));
            views.setViewVisibility(R.id.hoursTextView, View.VISIBLE);
            views.setViewVisibility(R.id.h, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.hoursTextView, "");
            views.setViewVisibility(R.id.hoursTextView, View.GONE);
            views.setViewVisibility(R.id.h, View.GONE);
        }

        matcher = Pattern.compile("(\\d+)M").matcher(formattedTimeLeft);
        if (matcher.find() && !hideMins) {
            allZero = false;
            views.setTextViewText(R.id.minutesTextView, matcher.group(1));
            views.setViewVisibility(R.id.minutesTextView, View.VISIBLE);
            views.setViewVisibility(R.id.m, View.VISIBLE);
        } else {
            views.setTextViewText(R.id.minutesTextView, "");
            views.setViewVisibility(R.id.minutesTextView, View.GONE);
            views.setViewVisibility(R.id.m, View.GONE);
        }
        if(allZero){
            if(repeat)updateCountdownWidget(context, appWidgetManager, appWidgetId, widgetWidth, widgetHeight);
            else views.setViewVisibility(R.id.msg, View.VISIBLE);
        }
        else views.setViewVisibility(R.id.msg, View.GONE);

        int backgroundColor = preferences.getInt("background", context.getResources().getColor(R.color.default_bg_color));
        try {
            views.setInt(R.id.widgetLayout, "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 150));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        // Set textColor from shared preferences
        int textColor = preferences.getInt("textColor", 0);
        try {
            int textColorValue = textColor==0?context.getResources().getColor(R.color.default_text_color):textColor;
            View rootView = views.apply(context, null);
            setTextColorForAllTextViews(context, appWidgetId, views, rootView, textColorValue);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    public static void updateAllWidgets(Context context) {
        if (context == null) {
            Log.d("wasim", "Context is null in updateAllWidgets()");
            return;
        }
        Log.d("wasim_c_countdown2", String.valueOf(context));
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Log.d("wasimbug1", appWidgetManager.toString());
        Log.d("wasimbug2", context.toString());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Countdown.class));
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateCountdownWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        this.context = context.getApplicationContext();
        Log.d("wasim_c_countdown", String.valueOf(this.context));
    }
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}
