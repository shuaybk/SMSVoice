package com.shuaybprojects.smsvoice;

/**
 * Created by Shuayb on 10-Dec-16.
 */

public class SmsMessage {
    public String sender;
    public String message;

    SmsMessage(String sender, String message){
        this.sender = sender;
        this.message = message;
    }
}
