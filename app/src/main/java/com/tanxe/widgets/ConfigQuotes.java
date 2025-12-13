package com.tanxe.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.core.content.ContextCompat;

public class ConfigQuotes extends Activity implements ColorPickerDialog.ColorPickerDialogListener {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean isBackgroundColor = false;
    Button backgroundButton;
    Button textColorButton;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quotes_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);


        preferences = getSharedPreferences("QuotesPrefs_" + appWidgetId, Context.MODE_PRIVATE);

        EditText quote = findViewById(R.id.quote);
        quote.setText(preferences.getString("quote", ""));


        backgroundButton = findViewById(R.id.quoteBackgroundColor);
        textColorButton = findViewById(R.id.quoteTextColor);

        backgroundButton.setBackgroundColor(preferences.getInt("background", Color.BLACK));
        textColorButton.setBackgroundColor(preferences.getInt("textColor", Color.WHITE));

        backgroundButton.setOnClickListener(v -> {
            showColorPicker(true);
        });

        textColorButton.setOnClickListener(v -> {
            showColorPicker(false); // Show color picker for text color
        });


        SeekBar fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        // Use global font size as default for new widgets
        SharedPreferences appPrefs = getSharedPreferences("TanxeWidgetsApp", Context.MODE_PRIVATE);
        int defaultFontSize = appPrefs.getInt("globalFontSize", 50);
        int fontSizeProgress = preferences.getInt("fontsize", defaultFontSize);
        fontSizeSeekBar.setProgress(fontSizeProgress);


        Button saveButton = findViewById(R.id.saveQuote);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveConfiguration(quote.getText().toString(), fontSizeSeekBar.getProgress());
                setResult(RESULT_OK);
                finish();

                // Update the widget with the saved configuration
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ConfigQuotes.this);
                android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
                Quotes.updateQuotesWidget(ConfigQuotes.this, appWidgetManager, appWidgetId, width, height);

                // Return the result to the calling activity (the launcher)
                setResult(RESULT_OK, resultValue);

                // Finish the activity
                finish();
            }
        });
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

    private void saveConfiguration(String quote, int fontSize) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("quote", quote);
        editor.putInt("fontsize", fontSize);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
