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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.InputStream;

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
    private static final String BACKUP_FILENAME_PREFIX = "tanxe-widgets-";

    private TextView totalWidgetsText;
    private TextView countCountdown, countPrayer, countQuotes, countQaza;
    private TextView lastBackupTime;
    private LinearLayout cardCountdown, cardPrayer, cardQuotes, cardQaza;
    private LinearLayout widgetsList;
    private TextView noWidgetsText;
    private Button btnBackup, btnRestore;
    private SeekBar globalFontSizeSeekBar;
    private SharedPreferences appPrefs;
    private ActivityResultLauncher<String> filePickerLauncher;
    private String pendingBackupData = null; // Store backup data for re-applying after adding widgets

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFilePickerLauncher();
        initViews();
        setupClickListeners();
        updateWidgetCounts();
        updateLastBackupTime();
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        restoreFromUri(uri);
                    }
                }
        );
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

            // Generate filename with current date (e.g., tanxe-widgets-14-Dec-2025.json)
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            String backupFilename = BACKUP_FILENAME_PREFIX + dateFormat.format(new Date()) + ".json";

            // Save to file
            File backupFile = new File(getExternalFilesDir(null), backupFilename);
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(backupData.toString(2).getBytes());
            fos.close();

            // Save backup time
            SharedPreferences prefs = getSharedPreferences("TanxeWidgetsApp", MODE_PRIVATE);
            prefs.edit().putLong("lastBackupTime", System.currentTimeMillis()).apply();

            updateLastBackupTime();
            Toast.makeText(this, "Backup saved successfully!", Toast.LENGTH_SHORT).show();

            // Share the backup file
            shareBackupFile(backupFile);

        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBackupFile(File backupFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    backupFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Tanxe Widgets Backup");
            shareIntent.putExtra(Intent.EXTRA_TITLE, backupFile.getName()); // Hint filename for Drive
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Tanxe Widgets backup file - Import this in the app to restore your widget settings.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Set ClipData for better filename support
            shareIntent.setClipData(android.content.ClipData.newUri(getContentResolver(), backupFile.getName(), fileUri));

            startActivity(Intent.createChooser(shareIntent, "Share Backup"));
        } catch (Exception e) {
            Log.e(TAG, "Share failed", e);
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        // Always open file picker to let user choose backup file
        Toast.makeText(this, "Select your backup file", Toast.LENGTH_SHORT).show();
        openFilePicker();
    }

    private void openFilePicker() {
        try {
            if (filePickerLauncher == null) {
                Toast.makeText(this, "File picker not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            filePickerLauncher.launch("*/*");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open file picker", e);
            Toast.makeText(this, "Cannot open file picker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Could not open file", Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            inputStream.close();

            processRestoreData(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Restore from URI failed", e);
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreFromFile(File backupFile) {
        try {
            FileInputStream fis = new FileInputStream(backupFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            processRestoreData(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processRestoreData(String jsonData) {
        try {
            JSONObject backupData = new JSONObject(jsonData);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

            // Store backup data for potential re-apply after adding widgets
            pendingBackupData = jsonData;

            // Count widgets in backup
            int backupCountdown = backupData.has("countdown") ? backupData.getJSONObject("countdown").length() : 0;
            int backupPrayer = backupData.has("prayer") ? backupData.getJSONObject("prayer").length() : 0;
            int backupQuotes = backupData.has("quotes") ? backupData.getJSONObject("quotes").length() : 0;
            int backupQaza = backupData.has("qaza") ? backupData.getJSONObject("qaza").length() : 0;
            int totalBackupWidgets = backupCountdown + backupPrayer + backupQuotes + backupQaza;

            // Count current widgets on screen
            int currentCountdown = appWidgetManager.getAppWidgetIds(new ComponentName(this, Countdown.class)).length;
            int currentPrayer = appWidgetManager.getAppWidgetIds(new ComponentName(this, Prayertime.class)).length;
            int currentQuotes = appWidgetManager.getAppWidgetIds(new ComponentName(this, Quotes.class)).length;
            int currentQaza = appWidgetManager.getAppWidgetIds(new ComponentName(this, QazaeUmri.class)).length;
            int totalCurrentWidgets = currentCountdown + currentPrayer + currentQuotes + currentQaza;

            // Check if no widgets on screen
            if (totalCurrentWidgets == 0 && totalBackupWidgets > 0) {
                showNoWidgetsDialog(backupCountdown, backupPrayer, backupQuotes, backupQaza);
                return;
            }

            // Restore countdown widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Countdown.class, "countdown", "CountdownPrefs_");

            // Restore prayer widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Prayertime.class, "prayer", "PrayerTimesPrefs_");

            // Restore quotes widgets
            restoreWidgetPrefs(backupData, appWidgetManager, Quotes.class, "quotes", "QuotesPrefs_");

            // Restore qaza widgets
            restoreWidgetPrefs(backupData, appWidgetManager, QazaeUmri.class, "qaza", "QazaeUmri_Prefs_");

            // Force update all widgets using broadcast (more reliable than direct call)
            forceUpdateAllWidgets();

            // Calculate restored vs missing
            int restoredCount = 0;
            int missingCount = 0;
            StringBuilder missingDetails = new StringBuilder();
            java.util.List<Class<?>> missingWidgetTypes = new java.util.ArrayList<>();

            restoredCount += Math.min(currentCountdown, backupCountdown);
            if (backupCountdown > currentCountdown) {
                int missing = backupCountdown - currentCountdown;
                missingCount += missing;
                missingDetails.append("• ").append(missing).append(" Countdown\n");
                for (int i = 0; i < missing; i++) missingWidgetTypes.add(Countdown.class);
            }

            restoredCount += Math.min(currentPrayer, backupPrayer);
            if (backupPrayer > currentPrayer) {
                int missing = backupPrayer - currentPrayer;
                missingCount += missing;
                missingDetails.append("• ").append(missing).append(" Prayer Times\n");
                for (int i = 0; i < missing; i++) missingWidgetTypes.add(Prayertime.class);
            }

            restoredCount += Math.min(currentQuotes, backupQuotes);
            if (backupQuotes > currentQuotes) {
                int missing = backupQuotes - currentQuotes;
                missingCount += missing;
                missingDetails.append("• ").append(missing).append(" Quotes\n");
                for (int i = 0; i < missing; i++) missingWidgetTypes.add(Quotes.class);
            }

            restoredCount += Math.min(currentQaza, backupQaza);
            if (backupQaza > currentQaza) {
                int missing = backupQaza - currentQaza;
                missingCount += missing;
                missingDetails.append("• ").append(missing).append(" Qaza-e-Umri\n");
                for (int i = 0; i < missing; i++) missingWidgetTypes.add(QazaeUmri.class);
            }

            // Show appropriate message
            if (missingCount > 0) {
                showMissingWidgetsDialog(restoredCount, missingCount, missingDetails.toString(), missingWidgetTypes);
            } else if (restoredCount > 0) {
                Toast.makeText(this, "Restore completed! " + restoredCount + " widget(s) updated.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No matching widgets to restore. Add widgets first, then restore.", Toast.LENGTH_LONG).show();
            }
            updateWidgetCounts();

        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showNoWidgetsDialog(int countdown, int prayer, int quotes, int qaza) {
        StringBuilder message = new StringBuilder();
        message.append("Your backup contains:\n");
        if (countdown > 0) message.append("• ").append(countdown).append(" Countdown widget(s)\n");
        if (prayer > 0) message.append("• ").append(prayer).append(" Prayer Times widget(s)\n");
        if (quotes > 0) message.append("• ").append(quotes).append(" Quotes widget(s)\n");
        if (qaza > 0) message.append("• ").append(qaza).append(" Qaza-e-Umri widget(s)\n");
        message.append("\nClick 'Add Widgets' to add them to your home screen, then click 'Apply Backup' to restore your data.");

        // Build list of widget types to add
        java.util.List<Class<?>> widgetTypes = new java.util.ArrayList<>();
        for (int i = 0; i < countdown; i++) widgetTypes.add(Countdown.class);
        for (int i = 0; i < prayer; i++) widgetTypes.add(Prayertime.class);
        for (int i = 0; i < quotes; i++) widgetTypes.add(Quotes.class);
        for (int i = 0; i < qaza; i++) widgetTypes.add(QazaeUmri.class);

        new AlertDialog.Builder(this)
                .setTitle("Add Widgets First")
                .setMessage(message.toString())
                .setPositiveButton("Add Widgets", (dialog, which) -> {
                    requestPinMissingWidgets(widgetTypes);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMissingWidgetsDialog(int restoredCount, int missingCount, String missingDetails, java.util.List<Class<?>> missingWidgetTypes) {
        StringBuilder message = new StringBuilder();
        if (restoredCount > 0) {
            message.append("✓ Restored settings for ").append(restoredCount).append(" widget(s)\n\n");
        }
        message.append("⚠ Missing ").append(missingCount).append(" widget(s) from backup:\n");
        message.append(missingDetails);
        message.append("\nClick 'Add Widgets' to add them to your home screen, then restore again.");

        new AlertDialog.Builder(this)
                .setTitle("Partial Restore")
                .setMessage(message.toString())
                .setPositiveButton("Add Widgets", (dialog, which) -> {
                    // Request to pin each missing widget
                    requestPinMissingWidgets(missingWidgetTypes);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void requestPinMissingWidgets(java.util.List<Class<?>> widgetTypes) {
        if (widgetTypes == null || widgetTypes.isEmpty()) return;

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported()) {
            // Request to pin each widget one by one
            int count = 0;
            for (Class<?> widgetClass : widgetTypes) {
                ComponentName widgetProvider = new ComponentName(this, widgetClass);
                appWidgetManager.requestPinAppWidget(widgetProvider, null, null);
                count++;
            }
            // Show follow-up dialog after a delay to let user add widgets
            final int finalCount = count;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                showCompleteRestoreDialog(finalCount);
            }, 1500);
        } else {
            // Fallback for older devices
            Toast.makeText(this, "Long press on home screen and add Tanxe Widgets manually, then come back and click Restore again", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    private void showCompleteRestoreDialog(int widgetCount) {
        new AlertDialog.Builder(this)
                .setTitle("Complete Restore")
                .setMessage("After adding " + widgetCount + " widget(s) to your home screen, click 'Apply Backup' to restore your saved data to them.")
                .setPositiveButton("Apply Backup", (dialog, which) -> {
                    if (pendingBackupData != null) {
                        processRestoreData(pendingBackupData);
                    } else {
                        Toast.makeText(this, "No backup data available. Please restore from file again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    Toast.makeText(this, "Come back and click Restore to apply your backup data", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    private void forceUpdateAllWidgets() {
        // Send broadcast to force update all widget types
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Update Countdown widgets
        int[] countdownIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, Countdown.class));
        if (countdownIds.length > 0) {
            Intent countdownIntent = new Intent(this, Countdown.class);
            countdownIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            countdownIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, countdownIds);
            sendBroadcast(countdownIntent);
        }

        // Update Prayertime widgets
        int[] prayerIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, Prayertime.class));
        if (prayerIds.length > 0) {
            Intent prayerIntent = new Intent(this, Prayertime.class);
            prayerIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            prayerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, prayerIds);
            sendBroadcast(prayerIntent);
        }

        // Update Quotes widgets
        int[] quotesIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, Quotes.class));
        if (quotesIds.length > 0) {
            Intent quotesIntent = new Intent(this, Quotes.class);
            quotesIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            quotesIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, quotesIds);
            sendBroadcast(quotesIntent);
        }

        // Update QazaeUmri widgets
        int[] qazaIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, QazaeUmri.class));
        if (qazaIds.length > 0) {
            Intent qazaIntent = new Intent(this, QazaeUmri.class);
            qazaIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            qazaIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, qazaIds);
            sendBroadcast(qazaIntent);
        }

        Log.d(TAG, "Sent update broadcasts to all widgets");
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
