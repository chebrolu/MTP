package com.iitb.wicroft;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.File;



/**
 * Created by swinky on 17/6/16.
 */
public class MainService extends Service {
    public static PowerManager powerManager ;
    public static PowerManager.WakeLock wakeLock;

    public MainService() {
        ;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        int move_to_foreground =sharedPref.getInt(getString(R.string.move_to_foreground), Constants.false_value);
        int move_to_background = sharedPref.getInt(getString(R.string.move_to_background), Constants.false_value);
        SharedPreferences.Editor editor = sharedPref.edit();


        //Bringing app in foreground, so that it may run without android optimizations,server sends this signal typically when the experiments are to be conducted.
        if(move_to_foreground==Constants.true_value) {
            editor.putInt(getString(R.string.move_to_foreground), Constants.false_value);
            editor.putInt(getString(R.string.is_running_in_foreground),Constants.true_value );
            editor.commit();
            wakeLock.acquire();     //acquire cpu wake lock
            Notification notification;
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.wicroft)
                            .setContentTitle("WiCroft in Foreground")
                            .setContentText("This notification will disappear automatically after experiments are done.");
            notification = mBuilder.build();
            notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
            startForeground(Constants.notifyIdMainService , notification);

        }
        else
        //Moving to background when tforeground timer expires.
        if( move_to_background==Constants.true_value){
            try {
                wakeLock.release();
            }
            catch (Exception e){
                //just ignoring
            }
            editor.putInt(getString(R.string.move_to_background), Constants.false_value);
            editor.putInt(getString(R.string.is_running_in_foreground),Constants.false_value );
            editor.commit();
            stopForeground(true);

        }
        else {
            // The Mainservice
            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakelockTag");
            try {
                wakeLock.release();
            }
            catch (Exception e){
                //just ignore this exception
            }

            editor.putInt(getString(R.string.is_running_in_foreground),Constants.false_value );
            editor.commit();

            Log.d(Constants.LOGTAG, " heartbeat: Initializing and registering everything.....");

            MainActivity.heartbeat_enabled = true;
            File logDir = new File(Constants.logDirectory);
            File controlDir = new File(Constants.controlFileDirectory);
            logDir.mkdirs();
            controlDir.mkdirs();

            //Register EventAlarmReceiver.
            IntentFilter alarmIntentFilter = new IntentFilter(Constants.BROADCAST_ALARM_ACTION);
            EventAlarmReceiver alarmReceiver = new EventAlarmReceiver();

            LocalBroadcastManager.getInstance(this).registerReceiver(alarmReceiver, alarmIntentFilter);
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            WiFiChecker wifi = new WiFiChecker();
            registerReceiver(wifi, filter);

            Threads.writeLog(Constants.debugLogFilename, "MainService : Initialized everything. Now starting Backgroundservices.");

            final Context ctx = getApplicationContext();
            EventRunner runEvent = new EventRunner(ctx);
            runEvent.execute();

            //calling heartbeat scheduler
            Heartbeat_scheduleAlarm();
            Log.d(Constants.LOGTAG, "Alarmscheduled.... :)");
            Threads.writeLog(Constants.debugLogFilename, "MainService started and HB Alarm scheduled");


        }
        return super.onStartCommand(intent, flags, startId);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void Heartbeat_scheduleAlarm( ){
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, Constants.hbRequestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        AlarmManager alarm = (AlarmManager) this.getSystemService(this.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstMillis, Constants.heartbeat_duration * 1000, pIntent);
    }


}
