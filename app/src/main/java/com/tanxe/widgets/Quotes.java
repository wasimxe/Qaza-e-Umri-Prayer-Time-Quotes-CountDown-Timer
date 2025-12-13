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
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

public class Quotes extends AppWidgetProvider {
    private static Context context;

    // Default text size (in sp)
    private static final float DEFAULT_TEXT_SIZE = 16f;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateQuotesWidget(context, appWidgetManager, appWidgetId, width, height);
        }
        updateAllWidgets(this.context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        updateQuotesWidget(context, appWidgetManager, appWidgetId, width, height);
    }

    private static float getUserFontScale(SharedPreferences preferences) {
        // fontsize ranges from 0-100, default 50
        // Scale factor: 0 -> 0.6x, 50 -> 1.0x, 100 -> 1.6x
        int fontSizeProgress = preferences.getInt("fontsize", 50);
        return 0.6f + (fontSizeProgress * 0.01f);
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
    static void updateQuotesWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int widgetWidth, int widgetHeight) {

        Log.d("wasim", "quotes called width=" + widgetWidth + " height=" + widgetHeight);
        SharedPreferences preferences = context.getSharedPreferences("QuotesPrefs_" + appWidgetId, Context.MODE_PRIVATE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.quotes_widget);

        // Calculate text size based on user preference
        float fontScale = getUserFontScale(preferences);
        float textSize = DEFAULT_TEXT_SIZE * fontScale;
        views.setFloat(R.id.widgetTitleTextView, "setTextSize", textSize);

        // Set click event on widget
        Intent configIntent = new Intent(context, ConfigQuotes.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.quoteWidget, configPendingIntent);

        // Set title from shared preferences
        String quote = preferences.getString("quote", "Enter Quotes");
        if (!quote.isEmpty()) {
            views.setTextViewText(R.id.widgetTitleTextView, quote);
        }

        int backgroundColor = preferences.getInt("background", context.getResources().getColor(R.color.default_bg_color));
        try {
            views.setInt(R.id.quoteWidget, "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 150));
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
        Log.d("wasim_c_quote2", String.valueOf(context));
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Log.d("wasimbug1", appWidgetManager.toString());
        Log.d("wasimbug2", context.toString());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Quotes.class));
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateQuotesWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        this.context = context.getApplicationContext();
        Log.d("wasim_c_quote", String.valueOf(this.context));
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
