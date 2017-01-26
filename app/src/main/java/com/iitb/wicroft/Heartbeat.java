package com.iitb.wicroft;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Patterns;
//import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;
/*
import java.net.Socket;
import java.util.concurrent.ExecutionException;
*/
/**
 * Created by swinky on 17/6/16.
 */
public class Heartbeat extends Service {


    public Heartbeat() {
        ;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("Checking flow", " heartbeat: Initializing and registering everything.....");

        MainActivity.heartbeat_enabled = true;

        //log and control file directories
        MainActivity.logDir = new File(Constants.logDirectory);
        MainActivity.logDir.mkdirs();
        MainActivity.controlDir = new File(Constants.controlFileDirectory);
       MainActivity.controlDir.mkdirs();

        MainActivity.wifimanager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = MainActivity.wifimanager.getConnectionInfo();
       /*
       Log.d("BSSID" ,info.getBSSID());
        String ssid  = info.getSSID();
        */
        MainActivity.am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        EventRunner.hb_restartalarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        //Register Broadcast receiver. To receive messages which needs to be displayed on screen
        IntentFilter broadcastIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        ResponseReceiver broadcastReceiver = new ResponseReceiver(new Handler());
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastIntentFilter);

        //Register EventAlarmReceiver.
        IntentFilter alarmIntentFilter = new IntentFilter(Constants.BROADCAST_ALARM_ACTION);
        EventAlarmReceiver alarmReceiver = new EventAlarmReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(alarmReceiver, alarmIntentFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        WiFiChecker wifi = new WiFiChecker();
        registerReceiver(wifi, filter);

        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": Heartbeat : Initialized everything. Now starting Backgroundservices.");
            //Threads.writeToLogFile(MainActivity.debugfilename , Utils.sdf.format(cal.getTime())+": Heartbeat : Initializing everything ");
        }

        MainActivity.context = getApplicationContext();
/*
        Log.d("Heartbeat", "Starting the background service");
        Intent startBackgroundServiceIntent = new Intent(getApplicationContext(), BackgroundServices.class);
        startService(startBackgroundServiceIntent);

        */

        final Context ctx = getApplicationContext();
        Log.d("Heartbeat", "Starting event Runner Async Task");
        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": Backgroundservices: Starting event Runner Async Task.");
            //Threads.writeToLogFile(MainActivity.debugfilename , Utils.sdf.format(cal.getTime())+": Heartbeat : Initializing everything ");
        }
        EventRunner runEvent = new EventRunner(ctx);
        runEvent.execute();

        //calling heartbeat scheduler
        Heartbeat_scheduleAlarm();
        Log.d("Heartbeat", "Alarmscheduled.... :)");
        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": Heartbeat : Backgroundservices started alarm scheduled.");
            //Threads.writeToLogFile(MainActivity.debugfilename , Utils.sdf.format(cal.getTime())+": Heartbeat : Initializing everything ");
        }

        if(!MainActivity.email_sent) {
            //getting an email information of the user
            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            Account[] accounts = AccountManager.get(MainActivity.context).getAccounts();
            String possibleEmail="";
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    possibleEmail = account.name;
                    Log.d("Heartbeat", "Account INfo : " + possibleEmail);
                    MainActivity.user_email = possibleEmail;

                }
            }

        }





/*
            }
        }).start();
*/
        return super.onStartCommand(intent, flags, startId);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void Heartbeat_scheduleAlarm( ){
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval in millisec
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstMillis, MainActivity.heartbeat_duration * 1000, pIntent);
    }


    public void hb_timeout(Long time , Context ctx)
    {

        // Create an Intent and set the class that will execute when the Alarm triggers. Here we have
        // specified AlarmReceiver in the Intent. The onReceive() method of this class will execute when the broadcast from your alarm is received.
        Intent intentAlarm = new Intent(ctx, AlarmReceiver.class);
        // Get the Alarm Service.
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Set the alarm for a particular time.
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(ctx, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

    }



}
