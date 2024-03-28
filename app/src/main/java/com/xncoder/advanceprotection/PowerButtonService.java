package com.xncoder.advanceprotection;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

public class PowerButtonService extends Service {
    private PowerButtonReceiver receiver;
    private long pressStartTime;
    private boolean isLongPress;
    private static final long LONG_PRESS_TIME = 1500;

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new PowerButtonReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private class PowerButtonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                pressStartTime = System.currentTimeMillis();
                isLongPress = false;
                new Handler().postDelayed(() -> {
                    long pressDuration = System.currentTimeMillis() - pressStartTime;
                    if (pressDuration >= LONG_PRESS_TIME) {
                        isLongPress = true;
                        Toast.makeText(context, "Power button long press detected", Toast.LENGTH_SHORT).show();
                        if (!Settings.System.canWrite(context)) {
                            Intent intentSetting = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intentSetting.setData(Uri.parse("package:" + context.getPackageName()));
                            startActivity(intentSetting);
                        }
                        try {
                            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
                        } catch (SecurityException ignored) {

                        }
                        Intent activityIntent = new Intent(context, DummyPowerScreen.class);
                        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(activityIntent);
                    }
                }, LONG_PRESS_TIME);
            }
        }
    }
}