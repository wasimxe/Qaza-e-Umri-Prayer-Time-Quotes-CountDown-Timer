package com.tanxe.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.lang.reflect.Field;

public class QazaeUmri extends AppWidgetProvider {
    private static Context context;

    // Default text sizes (in sp)
    private static final float DEFAULT_LABEL_SIZE = 16f;
    private static final float DEFAULT_COUNT_SIZE = 14f;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("wasim", "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            Log.d("wasim", String.valueOf(appWidgetId));
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateQazaeUmriWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        updateQazaeUmriWidget(context, appWidgetManager, appWidgetId, width, height);
    }

    private static float getUserFontScale(SharedPreferences preferences) {
        // fontsize ranges from 0-100, default 50
        // Scale factor: 0 -> 0.6x, 50 -> 1.0x, 100 -> 1.6x
        int fontSizeProgress = preferences.getInt("fontsize", 50);
        return 0.6f + (fontSizeProgress * 0.01f);
    }

    private static void setTextColorForAllTextViews(Context context, int appWidgetId, RemoteViews views, View rootView,
            int textColorValue) {
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

    protected static PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId, boolean lock) {
        Log.d("wasim","pendingIntent");
        if(lock) {
            Log.d("wasim", "lock=true setting will open");
            Intent configIntent = new Intent(context, ConfigQazaeUmri.class);
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
         else {
            Log.d("wasim", "lock=false count down will be called");
            Intent intent = new Intent(context, QazaeUmri.class);
            intent.setAction(action);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
    }

    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    static void updateQazaeUmriWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int widgetWidth, int widgetHeight) {

        Log.d("wasim", "updateQazaeUmriWidget width=" + widgetWidth + " height=" + widgetHeight);
        SharedPreferences preferences = context.getSharedPreferences("QazaeUmri_Prefs_" + appWidgetId, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.qaza_e_umri);

        // Calculate text sizes based on user preference
        float fontScale = getUserFontScale(preferences);
        float labelSize = DEFAULT_LABEL_SIZE * fontScale;
        float countSize = DEFAULT_COUNT_SIZE * fontScale;

        // Apply text sizes to all labels
        views.setFloat(R.id.fajrlabel, "setTextSize", labelSize);
        views.setFloat(R.id.dhuhrlabel, "setTextSize", labelSize);
        views.setFloat(R.id.asrlabel, "setTextSize", labelSize);
        views.setFloat(R.id.maghriblabel, "setTextSize", labelSize);
        views.setFloat(R.id.ishalabel, "setTextSize", labelSize);
        views.setFloat(R.id.vitarlabel, "setTextSize", labelSize);

        // Apply text sizes to all counts
        views.setFloat(R.id.fajr, "setTextSize", countSize);
        views.setFloat(R.id.dhuhr, "setTextSize", countSize);
        views.setFloat(R.id.asr, "setTextSize", countSize);
        views.setFloat(R.id.maghrib, "setTextSize", countSize);
        views.setFloat(R.id.isha, "setTextSize", countSize);
        views.setFloat(R.id.vitar, "setTextSize", countSize);

        views.setImageViewResource(R.id.lock, preferences.getBoolean("lock", true) ? R.drawable.lock : R.drawable.unlock);

        String[] actions = { "fajr", "dhuhr", "asr", "maghrib", "isha", "vitar" };

        int backgroundColor = preferences.getInt("background", context.getResources().getColor(R.color.default_bg_color)); // Replace with the key for background color preference
        try {
            views.setInt(R.id.QazaeUmriWidget, "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 50));
            for (int i = 0; i < actions.length; i++) {
                int prayerBox = getResId(actions[i] + "box", R.id.class);

                String prayerLable = preferences.getString(actions[i]+"Text","");
                Log.d("wasim_p", prayerLable);
                if(!prayerLable.isEmpty()){
                    views.setTextViewText(getResId(actions[i]+"label", R.id.class), prayerLable);
                }
                views.setOnClickPendingIntent(prayerBox, getPendingSelfIntent(context, actions[i], appWidgetId, preferences.getBoolean("lock", true)));   // click event on each prayer
                views.setViewVisibility(prayerBox, preferences.getBoolean("hide" + actions[i], false) ? View.GONE:View.VISIBLE); // hiding specific prayers
                views.setTextViewText(getViewIdForKey(actions[i]), String.valueOf(getCurrentNumber(context, appWidgetId, actions[i]))); // Setting prayer counts from sharedpreference

                views.setInt(prayerBox, "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 130));
                views.setInt(getResId(actions[i], R.id.class), "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 180));
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }


        int textColor = preferences.getInt("textColor", 0); // Replace with the key for text color preference
        try {
            int textColorValue = textColor == 0 ? context.getResources().getColor(R.color.default_text_color) : textColor;
            View rootView = views.apply(context, null);
            setTextColorForAllTextViews(context, appWidgetId, views, rootView, textColorValue);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("wasim", "widget click in unlock");
        Log.d("wasim_c_onreceiveqaza", String.valueOf(context));
        int appWidgetId;
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateNumber(context, appWidgetId, intent.getAction());
            }
        }
    }
    private void updateNumber(Context context, int appWidgetId, String key) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int viewId = getViewIdForKey(key);
        int currentNumber = getCurrentNumber(context, appWidgetId, key);

        if (currentNumber > 0) {
            currentNumber--;

            SharedPreferences preferences = context.getSharedPreferences("QazaeUmri_Prefs_" + appWidgetId, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, currentNumber);
            editor.apply();

            // Update the widget with the new number using full update for proper scaling
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateQazaeUmriWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }
    public static void updateAllWidgets(Context context) {
        Log.d("wasim_c_updateall", String.valueOf(context));
        if (context == null)return;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, QazaeUmri.class));
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences preferences = context.getSharedPreferences("QazaeUmri_Prefs_" + appWidgetId, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("lock", true);
            editor.apply();
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updateQazaeUmriWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }
    private static int getCurrentNumber(Context context, int appWidgetId, String key) {
        SharedPreferences preferences = context.getSharedPreferences("QazaeUmri_Prefs_" + appWidgetId, Context.MODE_PRIVATE);
        return preferences.getInt(key, 0);
    }

    private static int getViewIdForKey(String key) {
        switch (key) {
            case "fajr":
                return R.id.fajr;
            case "dhuhr":
                return R.id.dhuhr;
            case "asr":
                return R.id.asr;
            case "maghrib":
                return R.id.maghrib;
            case "isha":
                return R.id.isha;
            case "vitar":
                return R.id.vitar;
            default:
                return 0;
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        this.context = context.getApplicationContext();
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
