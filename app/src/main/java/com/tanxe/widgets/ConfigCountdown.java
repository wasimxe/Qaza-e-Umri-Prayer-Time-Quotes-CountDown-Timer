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
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TimePicker;

import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ConfigCountdown extends Activity implements ColorPickerDialog.ColorPickerDialogListener {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean isBackgroundColor = false;
    Button backgroundButton;
    Button textColorButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown_config);

        // Get the appWidgetId from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set a result to send back to the calling activity (the launcher)
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        // Initialize views
        EditText titleEditText = findViewById(R.id.titleEditText);
        EditText startTimeEditText = findViewById(R.id.startTimeEditText);
        View startll = findViewById(R.id.startll);

        backgroundButton = findViewById(R.id.backgroundButton);
        textColorButton = findViewById(R.id.textColorButton);

        CheckBox repeatCheckBox = findViewById(R.id.repeatCheckBox);
        DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker);
        TimePicker timePicker = findViewById(R.id.timePicker);

        CheckBox hideHoursCheckBox = findViewById(R.id.hidehours);
        CheckBox hideMinsCheckBox = findViewById(R.id.hidemins);
        CheckBox hideDaysCheckBox = findViewById(R.id.hidedays);

        // timePicker.setIs24HourView(true);
        Button saveButton = findViewById(R.id.saveButton);

        SharedPreferences preferences = getSharedPreferences("CountdownPrefs_" + appWidgetId, Context.MODE_PRIVATE);

        SeekBar fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        // Use global font size as default for new widgets
        SharedPreferences appPrefs = getSharedPreferences("TanxeWidgetsApp", Context.MODE_PRIVATE);
        int defaultFontSize = appPrefs.getInt("globalFontSize", 50);
        fontSizeSeekBar.setProgress(preferences.getInt("fontsize", defaultFontSize));

        titleEditText.setText(preferences.getString("title", ""));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, Calendar.JANUARY); // December is month 11 (0-based index)
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        startTimeEditText.setText(preferences.getString("starttime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime())));

        backgroundButton.setBackgroundColor(preferences.getInt("background", Color.BLACK));
        textColorButton.setBackgroundColor(preferences.getInt("textColor", Color.WHITE));

        boolean ischecked = preferences.getBoolean("repeat", false);
        boolean ischeckedHideHours = preferences.getBoolean("hidehours", false);
        boolean ischeckedHideMins = preferences.getBoolean("hidemins", false);
        boolean ischeckedHideDays = preferences.getBoolean("hidedays", false);
        
        repeatCheckBox.setChecked(ischecked);
        hideHoursCheckBox.setChecked(ischeckedHideHours);
        hideMinsCheckBox.setChecked(ischeckedHideMins);
        hideDaysCheckBox.setChecked(ischeckedHideDays);
        
        if(ischecked)startll.setVisibility(View.VISIBLE);

        Button showDatePickerButton = findViewById(R.id.showDatePickerButton);
        Button showTimePickerButton = findViewById(R.id.showTimePickerButton);

        // Set click listeners for the buttons
        showDatePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show DatePicker, hide TimePicker
                datePicker.setVisibility(View.VISIBLE);
                timePicker.setVisibility(View.GONE);
            }
        });

        repeatCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startll.setVisibility(repeatCheckBox.isChecked()?View.VISIBLE:View.GONE);
            }
        });


        showTimePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show TimePicker, hide DatePicker
                datePicker.setVisibility(View.GONE);
                timePicker.setVisibility(View.VISIBLE);
            }
        });

        backgroundButton.setOnClickListener(v -> {
            showColorPicker(true);
        });

        textColorButton.setOnClickListener(v -> {
            showColorPicker(false); // Show color picker for text color
        });

        /********** Trying to set datetime picker *********/

        calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));


        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null);
        long targetTimeMillis = preferences.getLong("targetTimeMillis", 0);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        if (targetTimeMillis > 0) {
            calendar.setTimeInMillis(targetTimeMillis);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        }
        datePicker.updateDate(year,month, dayOfMonth);

        // timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        /********** Trying to set datetime picker *********/

        // Save the configuration when the "Save" button is clicked
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEditText.getText().toString();
                boolean repeat = repeatCheckBox.isChecked();
                boolean hideHours = hideHoursCheckBox.isChecked();
                boolean hideMins = hideMinsCheckBox.isChecked();
                boolean hideDays = hideDaysCheckBox.isChecked();
                String startTime = startTimeEditText.getText().toString();

                int year = datePicker.getYear();
                int month = datePicker.getMonth();
                int day = datePicker.getDayOfMonth();
                int hour = timePicker.getCurrentHour();
                int minute = timePicker.getCurrentMinute();


                int fontSize = fontSizeSeekBar.getProgress();
                saveConfiguration(title, repeat, startTime, year, month, day, hour, minute, hideHours, hideMins, hideDays, fontSize);
                // Set the result to indicate success and finish the configuration activity
                setResult(RESULT_OK);
                finish();

                // Update the widget with the saved configuration
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ConfigCountdown.this);
                android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
                Countdown.updateCountdownWidget(ConfigCountdown.this, appWidgetManager, appWidgetId, width, height);

                // Return the result to the calling activity (the launcher)
                setResult(RESULT_OK, resultValue);

                // Finish the activity
                finish();
            }
        });
    }

    @Override
    public void onColorSelected(int color) {
        if (isBackgroundColor) {
            backgroundButton.setBackgroundColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        } else {
            textColorButton.setBackgroundColor(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        }
        String colorKey = isBackgroundColor ? "background" : "textColor";
        SharedPreferences preferences = getSharedPreferences("CountdownPrefs_" + appWidgetId, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(colorKey, color); // Save color as a hexadecimal string
        editor.apply();
    }

    private void showColorPicker(final boolean isBackgroundColorPicker) {
        int initialColor;
        isBackgroundColor = isBackgroundColorPicker;
        SharedPreferences preferences = getSharedPreferences("CountdownPrefs_" + appWidgetId, Context.MODE_PRIVATE);
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

    private void saveConfiguration(String title, boolean repeat, String startTime, int year, int month, int day, int hour, int minute, boolean hideHours, boolean hideMins, boolean hideDays, int fontSize) {
        // Save all preferences to SharedPreferences
        SharedPreferences preferences = getSharedPreferences("CountdownPrefs_" + appWidgetId, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Save date and time to SharedPreferences
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute);

        // Save targetTimeMillis in SharedPreferences
        long targetTimeMillis = calendar.getTimeInMillis();
        editor.putLong("targetTimeMillis", targetTimeMillis);

        long start_from = System.currentTimeMillis();

        if(repeat){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date date = dateFormat.parse(startTime);
                start_from = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        long originalTimeDifference = targetTimeMillis - start_from;

        // Save other preferences
        editor.putString("title", title);
        editor.putBoolean("repeat", repeat);
        editor.putBoolean("hidehours", hideHours);
        editor.putBoolean("hidemins", hideMins);
        editor.putBoolean("hidedays", hideDays);
        editor.putString("starttime", startTime);
        editor.putLong("originalTimeDifference", originalTimeDifference);
        editor.putInt("fontsize", fontSize);

        editor.apply();
    }

}
