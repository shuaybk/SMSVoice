package com.shuaybprojects.smsvoice;


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    protected static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    protected static final int NOTIF_ID = 456;
    protected static NotificationManager notifManager;
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

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        startService(new Intent(this, OnClearFromRecentService.class));
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
                    stopNotification();
                    tts.stop();
                    unregisterReceiver(smsListener);
                    button.setText("Start\nListening");
                    button.setBackgroundResource(R.drawable.roundbutton_red);
                } else {
                    if((ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) &&
                            (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED)) {
                        started = true;
                        startNotification();
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

                //Ensure phoneNum is not null or will cause issues in other places
                if (contactNames[i][1] == null) {
                    contactNames[i][1] = "";
                }
                i++;
            }
        } while(cursor.moveToNext());

        cursor.close();
    }

    public static String getName(Context context, String phoneNum) {
        String name = "";
        for (int i = 0; i < contactNames.length; i++) {
            if (PhoneNumberUtils.compare(contactNames[i][1], phoneNum) == true) {
                name = contactNames[i][0];
                break;
            }
        }
        return name;
    }

    public static boolean isExcluded(String phoneNum, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String key = "";

        for (int i = 0; i < MainActivity.contactNames.length; i++) {
            if (PhoneNumberUtils.compare(contactNames[i][1], phoneNum) == true) {
                key = MainActivity.contactNames[i][2];
                break;
            }
        }
        if (key.equals("")) {
            return false;
        }
        return sharedPref.getBoolean(key, false);
    }

    public static void readLastMessage(Context context, String phoneNum, String message){

        if (!isExcluded(phoneNum, context)) {
            String sender = getName(context, phoneNum);
            map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id");
            tts.speak("Message from ", TextToSpeech.QUEUE_ADD, map);
            tts.playSilence(200, TextToSpeech.QUEUE_ADD, map);
            if (sender.equals("") || (sender == null)) {
                tts.speak("Unknown number", TextToSpeech.QUEUE_ADD, map);
            } else {
                tts.speak(sender, TextToSpeech.QUEUE_ADD, map);
            }
            tts.playSilence(700, TextToSpeech.QUEUE_ADD, map);
            tts.speak(message, TextToSpeech.QUEUE_ADD, map);
            tts.playSilence(1000, TextToSpeech.QUEUE_ADD, map);
        }
    }

    public void startNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notif)
                .setContentTitle("SMSListener")
                .setContentText("Listening for incoming messages")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        notifManager.notify(NOTIF_ID, mBuilder.build());
    }

    public void stopNotification() {
        notifManager.cancel(NOTIF_ID);
    }

}


