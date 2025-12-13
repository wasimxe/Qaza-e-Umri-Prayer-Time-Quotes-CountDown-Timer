package com.tanxe.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Qibla;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigPrayer extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private ImageView compassNeedle;
    private float[] magnetometerValues = new float[3];
    private static final float ANGLE_THRESHOLD = 5.0f; // Define your threshold angle here
    private static final float ACCELEROMETER_ALPHA = 0.8f; // Define accelerometer filter constant here
    private float[] gravity = new float[3];

    private Coordinates qiblaCoordinates;


    private int appWidgetId;
    private SharedPreferences preferences;
    private boolean isBackgroundColor = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    Button backgroundButton;
    Button textColorButton;
    EditText latitude;
    EditText longitude;
    SeekBar fontSizeSeekBar;
    private double lastAngleToQibla;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prayer_widget_config);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve the widget ID from the intent extras
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Check if the widget ID is valid
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        preferences = getSharedPreferences("PrayerTimesPrefs_" + appWidgetId, Context.MODE_PRIVATE);

        backgroundButton = findViewById(R.id.backgroundButton);
        textColorButton = findViewById(R.id.textColorButton);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);

        Spinner spinner = findViewById(R.id.calmethods);
        String[] options = {"MUSLIM_WORLD_LEAGUE", "EGYPTIAN", "KARACHI", "UMM_AL_QURA", "DUBAI", "MOON_SIGHTING_COMMITTEE", "NORTH_AMERICA", "KUWAIT", "QATAR", "SINGAPORE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Arrays.asList(options).indexOf(preferences.getString("calmethod", "KARACHI")));

        Spinner azan = findViewById(R.id.azan);
        Field[] fields = R.raw.class.getFields();
        List<String> fileNames = new ArrayList<>();
        fileNames.add("NONE");
        for (Field field : fields) {
            try {
                int resourceId = field.getInt(null);
                String fileName = field.getName();
                fileNames.add(fileName);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        String[] azanOptions = fileNames.toArray(new String[0]);

        ArrayAdapter<String> azanAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, azanOptions);
        azanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        azan.setAdapter(azanAdapter);
        azan.setSelection(Arrays.asList(options).indexOf(preferences.getString("azan", azanOptions[0])));


        backgroundButton.setBackgroundColor(preferences.getInt("background", Color.BLACK));
        textColorButton.setBackgroundColor(preferences.getInt("textColor", Color.WHITE));
        latitude.setText(preferences.getString("latitude", "33.5913"));
        longitude.setText(preferences.getString("longitude", "73.3868"));

        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
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

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("calmethod", parentView.getItemAtPosition(position).toString());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle the case when nothing is selected (optional)
            }
        });

        azan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences.Editor editor = preferences.edit();
                String selectedAzan = parentView.getItemAtPosition(position).toString();
                editor.putString("azan", selectedAzan);
                editor.apply();

                if (!"NONE".equals(selectedAzan)) {
                    try {
                        Field field = R.raw.class.getDeclaredField(selectedAzan);
                        // field.getInt(null); = R.raw.selectedAzanFileName
                    } catch (Exception e) {e.printStackTrace();}
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle the case when nothing is selected (optional)
            }
        });

        ImageView gpsButton = findViewById(R.id.gps);
        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationUpdates();
            }
        });


        // Return the result to the widget provider to update the widget with the user's configuration
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);

        Button minusHijriDay = findViewById(R.id.minusHijriDay);
        minusHijriDay.setText(preferences.getString("minushijriday", "NO"));

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveConfiguration(latitude.getText().toString(), longitude.getText().toString(), minusHijriDay.getText().toString(), fontSizeSeekBar.getProgress());
                setResult(RESULT_OK);
                finish();

                // Update the widget with the saved configuration
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ConfigPrayer.this);
                android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
                Prayertime.updatePrayerWidget(ConfigPrayer.this, appWidgetManager, appWidgetId, width, height);

                // Return the result to the calling activity (the launcher)
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        compassNeedle = findViewById(R.id.qibla);
        qiblaCoordinates = new Coordinates(Double.parseDouble(latitude.getText().toString()), Double.parseDouble(longitude.getText().toString()));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            for (int i = 0; i < 3; i++) {
                gravity[i] = ACCELEROMETER_ALPHA * gravity[i] + (1 - ACCELEROMETER_ALPHA) * event.values[i];
            }
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerValues, 0, event.values.length);
        }

        if (gravity != null && magnetometerValues != null) {
            float[] rotationMatrix = new float[9];
            float[] orientationValues = new float[3];
            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetometerValues);
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            double azimuthInRadians = orientationValues[0];
            double azimuthInDegrees =  Math.toDegrees(azimuthInRadians);
            double angleToQibla = calculateAngleToQibla(azimuthInDegrees);

            if (Math.abs(angleToQibla - lastAngleToQibla) >= ANGLE_THRESHOLD) {
                compassNeedle.setRotation((float) angleToQibla);
                lastAngleToQibla = angleToQibla;
            }
        }
    }
    private double calculateQiblaDirection(Coordinates coordinates) {
        double latitude = Math.toRadians(coordinates.latitude);
        double longitude = Math.toRadians(coordinates.longitude);

        double kaabaLatitude = Math.toRadians(21.4225); // Latitude of the Kaaba in Mecca
        double kaabaLongitude = Math.toRadians(39.8262); // Longitude of the Kaaba in Mecca

        double deltaLongitude = kaabaLongitude - longitude;

        double y = Math.sin(deltaLongitude);
        double x = Math.cos(latitude) * Math.tan(kaabaLatitude) - Math.sin(latitude) * Math.cos(deltaLongitude);

        double qiblaDirection = Math.toDegrees(Math.atan2(y, x));

        if (qiblaDirection < 0) {
            qiblaDirection += 360; // Ensure the direction is in the range [0, 360] degrees
        }

        return qiblaDirection;
    }
    private double calculateAngleToQibla(double currentAzimuth) {
        double qiblaDirection = calculateQiblaDirection(qiblaCoordinates);
        double angleToQibla = qiblaDirection - currentAzimuth;

        if (angleToQibla > 180) {
            angleToQibla -= 360;
        } else if (angleToQibla < -180) {
            angleToQibla += 360;
        }

        return angleToQibla;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Register sensor listeners
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    private boolean checkLocationPermission() {
        // Check if location permission is already granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return false; // Permission not granted yet
        }
        return true; // Permission already granted
    }
    private boolean locationUpdateRequested = false;
    private void requestLocationUpdates() {
        if (!locationUpdateRequested && checkLocationPermission()) {
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // Create a location callback
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            latitude.setText(String.valueOf(location.getLatitude()));
                            longitude.setText(String.valueOf(location.getLongitude()));
                            locationUpdateRequested = true;
                        }
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void saveConfiguration(String latitude, String longitude, String minusHijriDay, int fontSize) {
        // Save all preferences to SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("latitude", latitude);
        editor.putString("longitude", longitude);
        editor.putString("minushijriday", minusHijriDay);
        editor.putInt("fontsize", fontSize);

        editor.apply();
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


    private void saveLocationToSharedPreferences(Context context, double latitude, double longitude) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("latitude", String.valueOf(latitude));
        editor.putString("longitude", String.valueOf(longitude));

        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates when the activity is destroyed
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

}
