package com.shuaybprojects.smsvoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * Created by Shuayb on 10-Dec-16.
 */

public class SmsListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)){
            Object[] pdus = (Object[]) (intent.getExtras().get("pdus")); //The raw messages
            SmsMessage[] msg = new SmsMessage[pdus.length];

            for (int i = 0; i < msg.length; i++) {
                msg[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                MainActivity.readLastMessage(context, msg[i].getOriginatingAddress(), msg[i].getMessageBody());
            }
        }
    }
}
