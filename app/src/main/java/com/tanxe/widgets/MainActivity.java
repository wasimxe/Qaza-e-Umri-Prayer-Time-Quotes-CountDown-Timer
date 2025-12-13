package com.tanxe.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String BACKUP_FILENAME = "tanxe_widgets_backup.json";

    private TextView totalWidgetsText;
    private TextView countCountdown, countPrayer, countQuotes, countQaza;
    private TextView lastBackupTime;
    private LinearLayout cardCountdown, cardPrayer, cardQuotes, cardQaza;
    private LinearLayout widgetsList;
    private TextView noWidgetsText;
    private Button btnBackup, btnRestore;
    private SeekBar globalFontSizeSeekBar;
    private SharedPreferences appPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        updateWidgetCounts();
        updateLastBackupTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidgetCounts();
    }

    private void initViews() {
        appPrefs = getSharedPreferences("TanxeWidgetsApp", MODE_PRIVATE);

        totalWidgetsText = findViewById(R.id.totalWidgetsText);
        countCountdown = findViewById(R.id.countCountdown);
        countPrayer = findViewById(R.id.countPrayer);
        countQuotes = findViewById(R.id.countQuotes);
        countQaza = findViewById(R.id.countQaza);
        lastBackupTime = findViewById(R.id.lastBackupTime);

        cardCountdown = findViewById(R.id.cardCountdown);
        cardPrayer = findViewById(R.id.cardPrayer);
        cardQuotes = findViewById(R.id.cardQuotes);
        cardQaza = findViewById(R.id.cardQaza);

        widgetsList = findViewById(R.id.widgetsList);
        noWidgetsText = findViewById(R.id.noWidgetsText);

        btnBackup = findViewById(R.id.btnBackup);
        btnRestore = findViewById(R.id.btnRestore);

        globalFontSizeSeekBar = findViewById(R.id.globalFontSizeSeekBar);
        globalFontSizeSeekBar.setProgress(appPrefs.getInt("globalFontSize", 50));
        globalFontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    appPrefs.edit().putInt("globalFontSize", progress).apply();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Default font size saved", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        // Widget cards - pin widgets directly
        cardCountdown.setOnClickListener(v -> requestPinWidget(Countdown.class, "Countdown"));
        cardPrayer.setOnClickListener(v -> requestPinWidget(Prayertime.class, "Prayer Times"));
        cardQuotes.setOnClickListener(v -> requestPinWidget(Quotes.class, "Quotes"));
        cardQaza.setOnClickListener(v -> requestPinWidget(QazaeUmri.class, "Qaza-e-Umri"));

        // Backup/Restore buttons
        btnBackup.setOnClickListener(v -> performBackup());
        btnRestore.setOnClickListener(v -> performRestore());
    }

    private void requestPinWidget(Class<?> widgetClass, String widgetName) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ supports pinning widgets programmatically
            ComponentName widgetProvider = new ComponentName(this, widgetClass);

            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                // Request to pin the widget
                appWidgetManager.requestPinAppWidget(widgetProvider, null, null);
                Toast.makeText(this, "Adding " + widgetName + " widget...", Toast.LENGTH_SHORT).show();
            } else {
                // Launcher doesn't support pinning, fall back to manual method
                fallbackWidgetAdd(widgetName);
            }
        } else {
            // For older Android versions, fall back to manual method
            fallbackWidgetAdd(widgetName);
        }
    }

    private void fallbackWidgetAdd(String widgetName) {
        Toast.makeText(this, "Long press on home screen to add " + widgetName + " widget", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void updateWidgetCounts() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        int countdownCount = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, Countdown.class)).length;
        int prayerCount = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, Prayertime.class)).length;
        int quotesCount = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, Quotes.class)).length;
        int qazaCount = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, QazaeUmri.class)).length;

        int totalCount = countdownCount + prayerCount + quotesCount + qazaCount;

        totalWidgetsText.setText(totalCount + " widgets active");
        countCountdown.setText(countdownCount + " active");
        countPrayer.setText(prayerCount + " active");
        countQuotes.setText(quotesCount + " active");
        countQaza.setText(qazaCount + " active");

        // Update existing widgets list
        updateWidgetsList(countdownCount, prayerCount, quotesCount, qazaCount);
    }

    private void updateWidgetsList(int countdown, int prayer, int quotes, int qaza) {
        // Clear existing views except noWidgetsText
        for (int i = widgetsList.getChildCount() - 1; i >= 0; i--) {
            View child = widgetsList.getChildAt(i);
            if (child.getId() != R.id.noWidgetsText) {
                widgetsList.removeViewAt(i);
            }
        }

        int total = countdown + prayer + quotes + qaza;
        if (total == 0) {
            noWidgetsText.setVisibility(View.VISIBLE);
            return;
        }

        noWidgetsText.setVisibility(View.GONE);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Add countdown widgets
        addWidgetItems(appWidgetManager, Countdown.class, "Countdown", "CountdownPrefs_");

        // Add prayer widgets
        addWidgetItems(appWidgetManager, Prayertime.class, "Prayer Times", "PrayerTimesPrefs_");

        // Add quotes widgets
        addWidgetItems(appWidgetManager, Quotes.class, "Quotes", "QuotesPrefs_");

        // Add qaza widgets
        addWidgetItems(appWidgetManager, QazaeUmri.class, "Qaza-e-Umri", "QazaeUmri_Prefs_");
    }

    private void addWidgetItems(AppWidgetManager appWidgetManager, Class<?> widgetClass, String typeName, String prefsPrefix) {
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, widgetClass));

        for (int widgetId : widgetIds) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(16, 12, 16, 12);
            itemLayout.setBackgroundResource(R.drawable.card_background);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            itemLayout.setLayoutParams(params);

            TextView typeText = new TextView(this);
            typeText.setText(typeName);
            typeText.setTextColor(0xFFFFFFFF);
            typeText.setTextSize(14);
            LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            typeText.setLayoutParams(typeParams);

            TextView idText = new TextView(this);
            idText.setText("ID: " + widgetId);
            idText.setTextColor(0xFF888888);
            idText.setTextSize(12);

            itemLayout.addView(typeText);
            itemLayout.addView(idText);

            // Click to open config
            final int fWidgetId = widgetId;
            itemLayout.setOnClickListener(v -> openWidgetConfig(widgetClass, fWidgetId));

            widgetsList.addView(itemLayout);
        }
    }

    private void openWidgetConfig(Class<?> widgetClass, int widgetId) {
        Intent configIntent = null;

        if (widgetClass == Countdown.class) {
            configIntent = new Intent(this, ConfigCountdown.class);
        } else if (widgetClass == Prayertime.class) {
            configIntent = new Intent(this, ConfigPrayer.class);
        } else if (widgetClass == Quotes.class) {
            configIntent = new Intent(this, ConfigQuotes.class);
        } else if (widgetClass == QazaeUmri.class) {
            configIntent = new Intent(this, ConfigQazaeUmri.class);
        }

        if (configIntent != null) {
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            startActivity(configIntent);
        }
    }

    private void performBackup() {
        try {
            JSONObject backupData = new JSONObject();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

            // Backup countdown widgets
            backupWidgetPrefs(backupData, appWidgetManager, Countdown.class, "countdown", "CountdownPrefs_");

            // Backup prayer widgets
            backupWidgetPrefs(backupData, appWidgetManager, Prayertime.class, "prayer", "PrayerTimesPrefs_");

            // Backup quotes widgets
            backupWidgetPrefs(backupData, appWidgetManager, Quotes.class, "quotes", "QuotesPrefs_");

            // Backup qaza widgets
            backupWidgetPrefs(backupData, appWidgetManager, QazaeUmri.class, "qaza", "QazaeUmri_Prefs_");

            // Save to file
            File backupFile = new File(getExternalFilesDir(null), BACKUP_FILENAME);
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(backupData.toString(2).getBytes());
            fos.close();

            // Save backup time
            SharedPreferences prefs = getSharedPreferences("TanxeWidgetsApp", MODE_PRIVATE);
            prefs.edit().putLong("lastBackupTime", System.currentTimeMillis()).apply();

            updateLastBackupTime();
            Toast.makeText(this, "Backup saved to: " + backupFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void backupWidgetPrefs(JSONObject backupData, AppWidgetManager appWidgetManager,
                                    Class<?> widgetClass, String key, String prefsPrefix) throws JSONException {
        JSONObject widgetBackup = new JSONObject();
        int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, widgetClass));

        for (int widgetId : widgetIds) {
            SharedPreferences prefs = getSharedPreferences(prefsPrefix + widgetId, MODE_PRIVATE);
            JSONObject prefsJson = new JSONObject();

            Map<String, ?> allPrefs = prefs.getAll();
            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                prefsJson.put(entry.getKey(), entry.getValue());
            }

            widgetBackup.put(String.valueOf(widgetId), prefsJson);
        }

        backupData.put(key, widgetBackup);
    }

    private void performRestore() {
        try {
            File backupFile = new File(getExternalFilesDir(null), BACKUP_FILENAME);
            if (!backupFile.exists()) {
                Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show();
                return;
            }

            FileInputStream fis = new FileInputStream(backupFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject backupData = new JSONObject(sb.toString());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

            // Restore countdown widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Countdown.class, "countdown", "CountdownPrefs_");

            // Restore prayer widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Prayertime.class, "prayer", "PrayerTimesPrefs_");

            // Restore quotes widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Quotes.class, "quotes", "QuotesPrefs_");

            // Restore qaza widgets
            restoreWidgetPrefs(backupData, appWidgetManager, QazaeUmri.class, "qaza", "QazaeUmri_Prefs_");

            // Update all widgets
            Countdown.updateAllWidgets(this);
            Prayertime.updateAllWidgets(this);
            Quotes.updateAllWidgets(this);
            QazaeUmri.updateAllWidgets(this);

            Toast.makeText(this, "Restore completed! Widget settings restored.", Toast.LENGTH_SHORT).show();
            updateWidgetCounts();

        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreWidgetPrefs(JSONObject backupData, AppWidgetManager appWidgetManager,
                                     Class<?> widgetClass, String key, String prefsPrefix) throws JSONException {
        if (!backupData.has(key)) return;

        JSONObject widgetBackup = backupData.getJSONObject(key);
        int[] currentWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, widgetClass));

        // Match backup data to current widgets by index (since IDs may have changed)
        Iterator<String> backupIds = widgetBackup.keys();
        int index = 0;

        while (backupIds.hasNext() && index < currentWidgetIds.length) {
            String backupId = backupIds.next();
            int currentId = currentWidgetIds[index];

            JSONObject prefsJson = widgetBackup.getJSONObject(backupId);
            SharedPreferences prefs = getSharedPreferences(prefsPrefix + currentId, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            Iterator<String> prefKeys = prefsJson.keys();
            while (prefKeys.hasNext()) {
                String prefKey = prefKeys.next();
                Object value = prefsJson.get(prefKey);

                if (value instanceof String) {
                    editor.putString(prefKey, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(prefKey, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(prefKey, (Long) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(prefKey, (Boolean) value);
                } else if (value instanceof Float || value instanceof Double) {
                    editor.putFloat(prefKey, ((Number) value).floatValue());
                }
            }

            editor.apply();
            index++;
        }
    }

    private void updateLastBackupTime() {
        SharedPreferences prefs = getSharedPreferences("TanxeWidgetsApp", MODE_PRIVATE);
        long lastBackup = prefs.getLong("lastBackupTime", 0);

        if (lastBackup == 0) {
            lastBackupTime.setText("Last backup: Never");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            lastBackupTime.setText("Last backup: " + sdf.format(new Date(lastBackup)));
        }
    }
}
