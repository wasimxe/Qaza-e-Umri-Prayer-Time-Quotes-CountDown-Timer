# Tanxe Widgets

A collection of beautiful and customizable Android home screen widgets.

## Widgets Included

### 1. Countdown Timer
- Set countdown to any future date/time
- Customizable display (show/hide Years, Days, Hours, Minutes)
- Repeat countdown option
- Custom title support

### 2. Prayer Times
- Displays Islamic prayer times based on your location
- Auto-updates throughout the day
- Location-based calculation

### 3. Qaza-e-Umri (Prayer Tracker)
- Track missed prayers
- Increment/decrement counters
- Persistent storage

### 4. Motivational Quotes
- Displays inspiring quotes
- Auto-refresh option
- Customizable appearance

## Features

- **Customizable Font Size**: Adjust text size for all widgets via global setting or per-widget
- **Color Customization**: Choose background and text colors for each widget
- **Easy Widget Addition**: Add widgets directly from the app (Android 8.0+)
- **Responsive Design**: Widgets adapt to different sizes
- **Dark/Light Theme Support**: Widgets look great on any wallpaper

## Screenshots

*Coming soon*

## Installation

### From APK
1. Download the APK from the [Releases](../../releases) section
2. Enable "Install from unknown sources" in your device settings
3. Install the APK

### From Source
1. Clone this repository
2. Open in Android Studio
3. Build and run on your device

## Requirements

- Android 5.0 (API 21) or higher
- Location permission (for Prayer Times widget)

## How to Use

1. Open the app
2. Adjust the global font size if desired
3. Tap on any widget card to add it to your home screen
4. Configure the widget settings as needed
5. Tap Save to confirm

## Building

```bash
# Set Java and Android SDK paths
export JAVA_HOME="path/to/jdk-17"
export ANDROID_HOME="path/to/android-sdk"

# Build debug APK
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/
├── java/com/tanxe/widgets/
│   ├── MainActivity.java          # Main app activity
│   ├── Countdown.java             # Countdown widget provider
│   ├── ConfigCountdown.java       # Countdown configuration
│   ├── Prayertime.java            # Prayer times widget provider
│   ├── ConfigPrayer.java          # Prayer times configuration
│   ├── QazaeUmri.java             # Qaza tracker widget provider
│   ├── ConfigQazaeUmri.java       # Qaza tracker configuration
│   ├── Quotes.java                # Quotes widget provider
│   ├── ConfigQuotes.java          # Quotes configuration
│   └── ...
├── res/
│   ├── layout/                    # Widget and activity layouts
│   ├── xml/                       # Widget info files
│   └── values/                    # Colors, strings, themes
└── AndroidManifest.xml
```

## Permissions

- `ACCESS_FINE_LOCATION` - For prayer time calculations
- `POST_NOTIFICATIONS` - For widget updates
- `RECEIVE_BOOT_COMPLETED` - To restore widgets after device restart
- `WAKE_LOCK` - To update widgets reliably

## License

This project is proprietary software by Tanxe Studio.

## Author

**Tanxe Studio**

---

*Built with Android Studio*
