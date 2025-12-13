package com.tanxe.widgets;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStateReceiver, filter);
    }
}
