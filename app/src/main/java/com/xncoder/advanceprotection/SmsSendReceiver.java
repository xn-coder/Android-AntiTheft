package com.xncoder.advanceprotection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsSendReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                // SMS sent successfully
                Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                // Generic failure
                Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                // No service
                Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                // Null PDU
                Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                // Radio off
                Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}