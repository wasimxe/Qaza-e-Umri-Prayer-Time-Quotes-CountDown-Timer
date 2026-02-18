package com.tanxe.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class Prayertime extends AppWidgetProvider {
    private static Context context;
    private static Map<String, String> prayersMap = new HashMap<>();

    // Default text sizes (in sp)
    private static final float DEFAULT_HEADER_SIZE = 11f;
    private static final float DEFAULT_LABEL_SIZE = 13f;
    private static final float DEFAULT_TIME_SIZE = 12f;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updatePrayerWidget(context, appWidgetManager, appWidgetId, width, height);
        }
        updateAllWidgets(this.context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        updatePrayerWidget(context, appWidgetManager, appWidgetId, width, height);
    }

    private static float getUserFontScale(SharedPreferences preferences) {
        // fontsize ranges from 0-100, default 50
        // Scale factor: 0 -> 0.6x, 50 -> 1.0x, 100 -> 1.6x
        int fontSizeProgress = preferences.getInt("fontsize", 50);
        return 0.6f + (fontSizeProgress * 0.01f);
    }

    private static void setTextColorForAllTextViews(RemoteViews views, View rootView, int textColorValue) {
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
                setTextColorForAllTextViews(views, childView, textColorValue);
            }
        }
    }
    private static String getFormattedCountdownTime(long targetTimeMillis, long startTime) {

        Log.d("wasimtarget", String.valueOf(targetTimeMillis));
        long timeDifference = targetTimeMillis - startTime;

        long seconds = timeDifference / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        hours %= 24;
        minutes %= 60;

        System.out.println(hours);

        StringBuilder countdownTimeBuilder = new StringBuilder();
        if (hours > 0) {
            countdownTimeBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownTimeBuilder.append(minutes).append("m ");
        }

        Log.d("wasimout", countdownTimeBuilder.toString());
        return countdownTimeBuilder.toString();
    }
    private static String midNight(long nextDaySunrise, long todaySunset) {
        long midTime = todaySunset + ((nextDaySunrise - todaySunset) / 2);
        Date midNightDate = new Date(midTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
        return dateFormat.format(midNightDate);

    }

    static void updatePrayerWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int widgetWidth, int widgetHeight) {

        SharedPreferences preferences = context.getSharedPreferences("PrayerTimesPrefs_" + appWidgetId, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.prayer_widget);

        // Calculate text sizes based on user preference
        float fontScale = getUserFontScale(preferences);
        float headerSize = DEFAULT_HEADER_SIZE * fontScale;
        float labelSize = DEFAULT_LABEL_SIZE * fontScale;
        float timeSize = DEFAULT_TIME_SIZE * fontScale;

        // Apply text sizes to header
        views.setFloat(R.id.leftHeaderText, "setTextSize", headerSize);
        views.setFloat(R.id.centerHeaderText, "setTextSize", headerSize);
        views.setFloat(R.id.rightHeaderText, "setTextSize", headerSize);

        // Apply text sizes to prayer labels
        views.setFloat(R.id.temp1, "setTextSize", labelSize);
        views.setFloat(R.id.temp2, "setTextSize", labelSize);
        views.setFloat(R.id.temp3, "setTextSize", labelSize);
        views.setFloat(R.id.temp4, "setTextSize", labelSize);
        views.setFloat(R.id.temp5, "setTextSize", labelSize);
        views.setFloat(R.id.temp6, "setTextSize", labelSize);

        // Apply text sizes to prayer times
        views.setFloat(R.id.fajr, "setTextSize", timeSize);
        views.setFloat(R.id.sunrise, "setTextSize", timeSize);
        views.setFloat(R.id.dhuhr, "setTextSize", timeSize);
        views.setFloat(R.id.asr, "setTextSize", timeSize);
        views.setFloat(R.id.maghrib, "setTextSize", timeSize);
        views.setFloat(R.id.isha, "setTextSize", timeSize);

        Intent configIntent = new Intent(context, ConfigPrayer.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.prayerWidget, configPendingIntent);

        CalculationParameters calMethod;

        String selectedMethod = preferences.getString("calmethod", "KARACHI");
        switch (selectedMethod) {
            case "MUSLIM_WORLD_LEAGUE":
                calMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE.getParameters();
                break;
            case "EGYPTIAN":
                calMethod = CalculationMethod.EGYPTIAN.getParameters();
                break;
            case "KARACHI":
                calMethod = CalculationMethod.KARACHI.getParameters();
                break;
            case "UMM_AL_QURA":
                calMethod = CalculationMethod.UMM_AL_QURA.getParameters();
                break;
            case "DUBAI":
                calMethod = CalculationMethod.DUBAI.getParameters();
                break;
            case "MOON_SIGHTING_COMMITTEE":
                calMethod = CalculationMethod.MOON_SIGHTING_COMMITTEE.getParameters();
                break;
            case "NORTH_AMERICA":
                calMethod = CalculationMethod.NORTH_AMERICA.getParameters();
                break;
            case "KUWAIT":
                calMethod = CalculationMethod.KUWAIT.getParameters();
                break;
            case "QATAR":
                calMethod = CalculationMethod.QATAR.getParameters();
                break;
            case "SINGAPORE":
                calMethod = CalculationMethod.SINGAPORE.getParameters();
                break;
            default:
                // Handle the default case here, e.g., set a default calMethod
                calMethod = CalculationMethod.KARACHI.getParameters();
                break;
        }

        calMethod.madhab = preferences.getInt("madhab", 1) == 1 ? Madhab.SHAFI : Madhab.HANAFI;
        double latitude = Double.parseDouble(preferences.getString("latitude", "33.5913"));
        double longitude = Double.parseDouble(preferences.getString("longitude", "73.3868"));

        PrayerTimes prayerTimes = new PrayerTimes(new Coordinates(latitude, longitude), DateComponents.from(new Date()), calMethod);

        String nextPrayerTimeString = null;
        long nextPrayerTime = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy");

        prayersMap.put("fajr", "فجر");
        prayersMap.put("sunrise", "طلوع");
        prayersMap.put("dhuhr", "ظہر");
        prayersMap.put("asr", "عصر");
        prayersMap.put("maghrib", "مغرب");
        prayersMap.put("isha", "عشاء");

        try {
            String nextPrayer = prayerTimes.nextPrayer().toString().toLowerCase();
            if(nextPrayer.equals("none")){
                nextPrayer = "fajr";
                nextPrayerTimeString = prayerTimes.fajr.toString();
                nextPrayerTime = dateFormat.parse((String) nextPrayerTimeString).getTime() + 86400000; // Adding one day to get next fajr time
            }
            else {
                nextPrayerTimeString = prayerTimes.getClass().getDeclaredField(nextPrayer).get(prayerTimes).toString();
                nextPrayerTime = dateFormat.parse((String) nextPrayerTimeString).getTime();
            }
            nextPrayerTimeString = getFormattedCountdownTime(nextPrayerTime, System.currentTimeMillis());
            views.setTextViewText(R.id.leftHeaderText, nextPrayerTimeString + "☚ " + prayersMap.get(nextPrayer));


            String[] mid_header = new String[3];
            long time1 = dateFormat.parse((String) prayerTimes.sunrise.toString()).getTime();
            long time2 = dateFormat.parse((String) prayerTimes.maghrib.toString()).getTime();
            mid_header[0] = "دن کا دورانیہ: "+ getFormattedCountdownTime(time2, time1).replace("h", "گھنٹے").replace("m", "منٹ");

            time1 = dateFormat.parse((String) prayerTimes.maghrib.toString()).getTime();
            time2 = dateFormat.parse((String) prayerTimes.sunrise.toString()).getTime() + 86400000;
            mid_header[1] = "رات کا دورانیہ: "+getFormattedCountdownTime(time2, time1).replace("h", "گھنٹے").replace("m", "منٹ");

            mid_header[2] = "آدھی رات: "+midNight(time2, time1);

            views.setTextViewText(R.id.centerHeaderText, mid_header[new Random().nextInt(3)]);

        } catch (ParseException e) {
            System.out.println(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Karachi"));

        views.setTextViewText(R.id.fajr, formatter.format(prayerTimes.fajr));
        views.setTextViewText(R.id.sunrise, formatter.format(prayerTimes.sunrise));
        views.setTextViewText(R.id.dhuhr, formatter.format(prayerTimes.dhuhr));


        SharedPreferences.Editor editor = preferences.edit();
        if(prayerTimes.currentPrayer() == Prayer.ASR && calMethod.madhab == Madhab.SHAFI)editor.putInt("madhab", 2);
        else if(prayerTimes.currentPrayer() == Prayer.MAGHRIB)editor.putInt("madhab", 1);
        editor.apply();

        views.setTextViewText(R.id.asr, formatter.format(prayerTimes.asr));

        views.setTextViewText(R.id.maghrib, formatter.format(prayerTimes.maghrib));
        views.setTextViewText(R.id.isha, formatter.format(prayerTimes.isha));

        Calendar calendar = Calendar.getInstance();
        String[] urduDays = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        Log.d("wasimday",urduDays[day]);

        LocalDate currentDate = null;
        HijrahDate islamicDate = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();

            if("YES".equals(preferences.getString("minushijriday", "NO"))){ // This is to minus one day from hijri date
                currentDate = currentDate.minusDays(1);
            }

            // Islamic date changes at Maghrib, not midnight.
            // After midnight the Gregorian date advances but the Islamic day hasn't ended yet,
            // so subtract 1 day to keep the same Islamic date until next Maghrib.
            String currentPrayer = prayerTimes.currentPrayer().toString().toLowerCase();
            if(!currentPrayer.equals("maghrib") && !currentPrayer.equals("isha")){
                currentDate = currentDate.minusDays(1);
            }

            islamicDate = HijrahDate.from(currentDate);

            // Use Locale for Urdu (Pakistan) to get month names in Urdu
            Locale urduLocale = new Locale("ur", "PK");

            DateTimeFormatter urduDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", urduLocale);
            String urduIslamicDate = urduDateFormatter.format(islamicDate);

            views.setTextViewText(R.id.rightHeaderText,  urduDays[day] + " - " + urduIslamicDate);
        }



        int backgroundColor = preferences.getInt("background", context.getResources().getColor(R.color.default_bg_color));
        try {
            views.setInt(R.id.prayerWidget, "setBackgroundColor", ColorUtils.setAlphaComponent(backgroundColor, 150));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }


        // Set textColor from shared preferences
        int textColor = preferences.getInt("textColor", 0); // Replace with the key for text color preference
        try {
            int textColorValue = textColor==0?context.getResources().getColor(R.color.default_text_color):textColor;
            View rootView = views.apply(context, null);
            setTextColorForAllTextViews(views, rootView, textColorValue);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    public static void updateAllWidgets(Context context) {
        if (context == null) {
            Log.d("wasim", "prayer Context is null in updateAllWidgets()");
            return;
        }
        Log.d("wasim_c_prayer2", String.valueOf(context));
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Prayertime.class));
        for (int appWidgetId : appWidgetIds) {
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
            int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
            updatePrayerWidget(context, appWidgetManager, appWidgetId, width, height);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        this.context = context.getApplicationContext();
        Log.d("wasim_c_prayer", String.valueOf(this.context));
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
