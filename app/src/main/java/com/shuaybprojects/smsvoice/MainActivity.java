package com.shuaybprojects.smsvoice;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import javax.security.auth.callback.PasswordCallback;


public class MainActivity extends AppCompatActivity {

    protected static final String INBOX = "content://sms/inbox";
    protected final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    protected static TextToSpeech tts;
    Button button;
    TextView messageText;
    protected static HashMap<String, String> map;
    protected static String[][] contactNames;

    protected static boolean started;
    SmsListener smsListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(this, OnClearFromRecentService.class));

        started = false;

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.US);
                }
            }
        });

        tts.setSpeechRate(0.85f);

        messageText = (TextView)findViewById(R.id.message_text);
        button = (Button)findViewById(R.id.button);
        button.setText("Start\nListening");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (started) {
                    started = false;
                    tts.stop();
                    unregisterReceiver(smsListener);
                    button.setText("Start\nListening");
                    button.setBackgroundResource(R.drawable.roundbutton_red);
                } else {
                    if((ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED)) {
                        started = true;
                        if (smsListener == null) {
                            smsListener = new SmsListener();
                        }
                        registerReceiver(smsListener, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
                        button.setText("Stop\nListening");
                        button.setBackgroundResource(R.drawable.roundbutton_green);
                    } else {
                        requestPerm();
                    }
                }
            }
        });
        button.post(new Runnable(){
            @Override
            public void run() {
                float density = getResources().getDisplayMetrics().density;
                int size = (int)(button.getWidth() + 30 * density + 0.5f);
                button.setWidth(size);
                button.setHeight(size);
            }
        });

        if((ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED)) {
            initContacts();
        } else {
            requestPerm();
        }
    }

    void requestPerm() {
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS", "android.permission.READ_CONTACTS"}, REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if ((ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED)) {
                    initContacts();
                    messageText.setText("");
                } else {
                    messageText.setText("Missing permissions! App won't work");
                }
                break;
        }
    }

    void initContacts() {
        Cursor cursor = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data._ID}, null, null, ContactsContract.Data.DISPLAY_NAME + ", " + ContactsContract.Data._ID);
        Cursor tempCursor = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER}, null, null, null);

        contactNames = new String[tempCursor.getCount()][3];
        tempCursor.close();
        //To avoid inserting duplicate contact names (one preference item per name)
        HashSet<String> set = new HashSet<String>();

        cursor.moveToFirst();
        int i = 0;
        do {
            if (set.add(cursor.getString(0))) {
                contactNames[i][0] = cursor.getString(1); //The names
                contactNames[i][1] = cursor.getString(0); //The phone numbers
                contactNames[i][2] = "key_" + cursor.getString(2); //Construct the key
                i++;
            }
        } while(cursor.moveToNext());

        cursor.close();
    }

    public static SmsMessage getMessage(Context context) {
        String name = null;
        String message = null;

        Cursor cursor = context.getContentResolver().query(Uri.parse(INBOX), new String[]{"address", "body"}, null, null, "date DESC");
        if (cursor.moveToFirst()) {
            name = getName(cursor.getString(0), context);
            message = cursor.getString(1);
        }
        cursor.close();

        return new SmsMessage(name, message);
    }

    public static String getName(String phoneNum, Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name"}, ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + "=" + "\'" + phoneNum + "\'", null, null);
        String name = null;

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();

        return name;
    }

    public static boolean isExcluded(String name, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "";
        for (int i = 0; i < MainActivity.contactNames.length; i++) {
            if (MainActivity.contactNames[i][0].equals(name)) {
                key = MainActivity.contactNames[i][2];
                break;
            }
        }
        if (key.equals("")) {
            return false;
        }
        return sharedPref.getBoolean(key, false);
    }

    public static void readLastMessage(Context context){
        SmsMessage toRead = getMessage(context);

        if (!isExcluded(toRead.sender, context)) {
            map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id");
            tts.speak("Message from ", TextToSpeech.QUEUE_ADD, map);
            tts.playSilence(200, TextToSpeech.QUEUE_ADD, map);
            if (toRead.sender == null) {
                tts.speak("Unknown number", TextToSpeech.QUEUE_ADD, map);
            } else {
                tts.speak(toRead.sender, TextToSpeech.QUEUE_ADD, map);
            }
            tts.playSilence(700, TextToSpeech.QUEUE_ADD, map);
            tts.speak(toRead.message, TextToSpeech.QUEUE_ADD, map);
            tts.playSilence(1000, TextToSpeech.QUEUE_ADD, map);
        }
    }

}


