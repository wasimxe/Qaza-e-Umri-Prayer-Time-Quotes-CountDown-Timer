package com.tanxe.widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Random;
import android.view.LayoutInflater;

public class ConfigQazaeUmri extends Activity implements ColorPickerDialog.ColorPickerDialogListener {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean isBackgroundColor = false;
    Button backgroundButton;
    Button textColorButton;
    SeekBar fontSizeSeekBar;
    private SharedPreferences preferences;
    private final String[] prayerNames = {"fajr", "dhuhr", "asr", "maghrib", "isha", "vitar"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qazaeumri_widget_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set a result to send back to the calling activity (the launcher)
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        // setResult(RESULT_OK, resultValue);

        // Check if the widget ID is valid
//        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
//            finish();
//            return;
//        }

        preferences = getSharedPreferences("QazaeUmri_Prefs_" + appWidgetId, Context.MODE_PRIVATE);

        EditText[] prayerEditText = new EditText[prayerNames.length];
        CheckBox[] hidePrayerCheckBoxes = new CheckBox[prayerNames.length];

        for (int i = 0; i < prayerNames.length; i++) {
            prayerEditText[i] = findViewById(QazaeUmri.getResId(prayerNames[i]+"Qaza", R.id.class));
            hidePrayerCheckBoxes[i] = findViewById(QazaeUmri.getResId("hide" + prayerNames[i].substring(0, 1).toUpperCase() + prayerNames[i].substring(1) + "Qaza", R.id.class));

            prayerEditText[i].setText(String.valueOf(preferences.getInt(prayerNames[i], 0)));
            hidePrayerCheckBoxes[i].setChecked(preferences.getBoolean("hide" + prayerNames[i], false));
        }

        backgroundButton = findViewById(R.id.backgroundButton2);
        textColorButton = findViewById(R.id.textColorButton2);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);

        backgroundButton.setBackgroundColor(preferences.getInt("background", Color.BLACK));
        textColorButton.setBackgroundColor(preferences.getInt("textColor", Color.WHITE));
        // Use global font size as default for new widgets
        SharedPreferences appPrefs = getSharedPreferences("TanxeWidgetsApp", Context.MODE_PRIVATE);
        int defaultFontSize = appPrefs.getInt("globalFontSize", 50);
        fontSizeSeekBar.setProgress(preferences.getInt("fontsize", defaultFontSize));

        backgroundButton.setOnClickListener(v -> {
            showColorPicker(true);
        });

        textColorButton.setOnClickListener(v -> {
            showColorPicker(false); // Show color picker for text color
        });


        Button saveButton = findViewById(R.id.saveQazaConfig);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfiguration(prayerEditText, hidePrayerCheckBoxes, fontSizeSeekBar.getProgress());
                setResult(RESULT_OK);
                finish();

                // Update the widget with the saved configuration
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ConfigQazaeUmri.this);
                android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
                QazaeUmri.updateQazaeUmriWidget(ConfigQazaeUmri.this, appWidgetManager, appWidgetId, width, height);

                // Return the result to the calling activity (the launcher)
                setResult(RESULT_OK, resultValue);

                // Finish the activity
                finish();
            }
        });

        int[] textViewIds = { R.id.fajrText, R.id.dhuhrText, R.id.asrText, R.id.maghribText, R.id.ishaText, R.id.vitarText };

        for (final int textViewId : textViewIds) {
            final TextView textView = findViewById(textViewId);
            final String key = getResources().getResourceEntryName(textViewId);

            String textValue = preferences.getString(key, "");
            if(!textValue.isEmpty()) textView.setText(textValue);

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCustomDialog(textView, key);
                }
            });
        }




        TextView captcha = findViewById(R.id.captcha);
        int randomNo = new Random().nextInt(6);
        captcha.setText("2² + "+ randomNo +" =");


        Button unlockSettings = findViewById(R.id.unlockSettings);
        Button unlockWidget = findViewById(R.id.unlockWidget);

        unlockSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlock(findViewById(R.id.answer), 4 + randomNo);
            }
        });
        unlockWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlock(findViewById(R.id.answer), 4 + randomNo);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("lock", false);
                editor.apply();

                AppWidgetManager appWidgetManager2 = AppWidgetManager.getInstance(ConfigQazaeUmri.this);
                android.os.Bundle options2 = appWidgetManager2.getAppWidgetOptions(appWidgetId);
                int width2 = options2.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                int height2 = options2.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
                QazaeUmri.updateQazaeUmriWidget(ConfigQazaeUmri.this, appWidgetManager2, appWidgetId, width2, height2);

                setResult(RESULT_OK, resultValue);  // Return the result to the calling activity (the launcher)
                finish();
            }
        });
    }
    private void showCustomDialog(TextView prayerText, String key) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_prompt_dialog, null);

        final EditText editText = dialogView.findViewById(R.id.editText);
        Button okButton = dialogView.findViewById(R.id.okButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        editText.setText(prayerText.getText().toString());

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Edit Title")
                .setCancelable(false)
                .create();

        dialog.show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newText = editText.getText().toString();
                prayerText.setText(newText);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, newText);
                editor.apply();

                dialog.dismiss();
            }
        });

        // Handle Cancel button click
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
    private void unlock(EditText editText, Integer correctAnswer){
        String answerStr = editText.getText().toString();
        if (!answerStr.isEmpty()) {
            Integer answer = Integer.parseInt(answerStr);
            if(answer == correctAnswer) {
                findViewById(R.id.overlayLayout).setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
            else Toast.makeText(getApplicationContext(), "Incorrect Answer", Toast.LENGTH_LONG).show();
        }
    }

    public void onColorSelected(int color) {
        if (isBackgroundColor) {
            backgroundButton.setBackgroundColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        } else {
            textColorButton.setBackgroundColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        }
        String colorKey = isBackgroundColor ? "background" : "textColor";
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(colorKey, color); // Save color as a hexadecimal string
        editor.apply();
    }

    private void showColorPicker(final boolean isBackgroundColorPicker) {
        int initialColor;
        isBackgroundColor = isBackgroundColorPicker;
        String colorKey = isBackgroundColorPicker ? "background" : "textColor";
        int savedColor = preferences.getInt(colorKey, 0);
        if (savedColor != 0) {
            initialColor = savedColor;
        } else {
            initialColor = ContextCompat.getColor(this, isBackgroundColorPicker ? R.color.default_bg_color : R.color.default_text_color);
        }

        // Create a color picker dialog
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, this::onColorSelected);
        colorPickerDialog.show();
    }

    private void saveConfiguration(EditText[] prayerEditText, CheckBox[] hidePrayerCheckBoxes, int fontSize) {
        SharedPreferences.Editor editor = preferences.edit();

        for (int i = 0; i < prayerNames.length; i++) {
            editor.putInt(prayerNames[i], Integer.parseInt(prayerEditText[i].getText().toString()));
            editor.putBoolean("hide"+prayerNames[i], hidePrayerCheckBoxes[i].isChecked());
        }
        editor.putInt("fontsize", fontSize);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
