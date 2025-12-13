package com.tanxe.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.tanxe.widgets.Countdown;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static Context context = null; // Store the context
    private static final long UPDATE_INTERVAL = 60000; // 1 minute
    private static Handler handler = new Handler();
    private static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Countdown.updateAllWidgets(context);
            Prayertime.updateAllWidgets(context);
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("wasim_c_screen", String.valueOf(context));
        if(this.context == null)this.context = context;
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                handler.postDelayed(runnable, 0);
                QazaeUmri.updateAllWidgets(context);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                handler.removeCallbacks(runnable);
            }
        }
    }
}


