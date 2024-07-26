package com.xncoder.advanceprotection;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SmsReceiver extends BroadcastReceiver {

    Context context;
    private static final int RINGER_MODE_SILENT = AudioManager.RINGER_MODE_SILENT;
    private  static final int RINGER_MODE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE;
    private static final int RINGER_MODE_NORMAL = AudioManager.RINGER_MODE_NORMAL;
    private final Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    private static boolean secureMode = false;
    private static String secureModeNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        String code = new SaveSecureCode(context).getData();
                        SmsManager smsManager = SmsManager.getDefault();
                        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);

                        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ringtone.setLooping(true);
                        }

                        int codeInd = messageBody.indexOf("-");
                        if (messageBody.startsWith(code) && codeInd == -1) {
                            String message1 = "Secure Mode Activated\nHelp: <Secure Code>-<Options>\nOptions:\n1. Get-Location";
                            String message2 = "2. Ringer-Normal, Silent, Vibrate, DND\n3. Sound-Play, Stop\n4. Flash-On, Off\n5. Secure-Deactivated";

                            smsManager.sendTextMessage(sender, null, message1, sentPendingIntent, null);
                            smsManager.sendTextMessage(sender, null, message2, sentPendingIntent, null);
                            secureMode = true;
                            secureModeNumber = sender;

                            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
                        } else {
                            if (codeInd != -1 && messageBody.substring(0, codeInd).equals(code) && secureMode) {
                                assert sender != null;
                                if (sender.equals(secureModeNumber)) {
                                    messageBody = messageBody.substring(codeInd + 1);
                                    int modeInd = messageBody.indexOf("-");
                                    String mode = messageBody.substring(modeInd + 1);
                                    if (messageBody.substring(0, modeInd).equals("Get") && mode.startsWith("Location")) {
                                        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
                                        LocationRequest locationRequest = LocationRequest.create();
                                        locationRequest.setInterval(600000);
                                        locationRequest.setFastestInterval(300000);
                                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                        LocationCallback locationCallback = new LocationCallback() {
                                            @Override
                                            public void onLocationResult(LocationResult locationResult) {
                                                for (Location location : locationResult.getLocations()) {
                                                    Log.d("Locations: ", location.toString());
                                                    if (location != null) {
                                                        try {
                                                            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                                                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                                            new SaveLocation(context).updateLocation("https://www.google.com/maps?q=" + addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
                                                            Toast.makeText(context, ""+addresses.get(0).getLatitude()+addresses.get(0).getLongitude(), Toast.LENGTH_SHORT).show();
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }
                                                }
                                            }
                                        };
                                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                                        String message = "Secure Mode get location\nLink : " + new SaveLocation(context).getLocationLink();
                                        smsManager.sendTextMessage(sender, null, message, sentPendingIntent, null);
                                    } else if (messageBody.substring(0, modeInd).equals("Ringer")) {
                                        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                        if (mode.startsWith("Normal")) {
                                            audioManager.setRingerMode(RINGER_MODE_NORMAL);
                                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                                        } else if (mode.startsWith("Silent"))
                                            audioManager.setRingerMode(RINGER_MODE_SILENT);
                                        else if (mode.startsWith("Vibrate"))
                                            audioManager.setRingerMode(RINGER_MODE_VIBRATE);
                                        else if (mode.startsWith("DND")) {
                                            if (audioManager.getRingerMode() != RINGER_MODE_NORMAL)
                                                audioManager.setRingerMode(RINGER_MODE_NORMAL);
                                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                                        }
                                        smsManager.sendTextMessage(sender, null, "Done " + mode, sentPendingIntent, null);
                                    } else if (messageBody.substring(0, modeInd).equals("Sound")) {
                                        if (messageBody.contains("Play") && !ringtone.isPlaying()) {
                                            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                            audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_ALLOW_RINGER_MODES);
                                            ringtone.play();
                                        } else if (messageBody.contains("Stop"))
                                            ringtone.stop();
                                        smsManager.sendTextMessage(sender, null, "Done " + messageBody.substring(0, modeInd) + " " + mode, sentPendingIntent, null);
                                    } else if (messageBody.substring(0, modeInd).equals("Flash")) {
                                        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                                        String cameraId;
                                        try {
                                            cameraId = cameraManager.getCameraIdList()[0];
                                        } catch (CameraAccessException e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (mode.startsWith("On")) {
                                            try {
                                                cameraManager.setTorchMode(cameraId, true);
                                            } catch (CameraAccessException e) {
                                                throw new RuntimeException(e);
                                            }
                                        } else if (mode.startsWith("Off")) {
                                            try {
                                                cameraManager.setTorchMode(cameraId, false);
                                            } catch (CameraAccessException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                        smsManager.sendTextMessage(sender, null, "Done " + messageBody.substring(0, modeInd) + " " + mode, sentPendingIntent, null);
                                    } else if (messageBody.substring(0, modeInd).equals("Secure") && mode.startsWith("Deactivated")) {
                                        smsManager.sendTextMessage(sender, null, "Secure Mode Deactivated", sentPendingIntent, null);
                                        secureMode = false;
                                        secureModeNumber = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}