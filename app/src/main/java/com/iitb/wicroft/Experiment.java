package com.iitb.wicroft;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by swinky on 7/10/16.
 */
public class Experiment extends Service {

    public Experiment() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override

    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent myintent, int flags, int startId) {

        Notification notification;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.wicroft)
                        .setContentTitle("WiCroft")
                        .setContentText("Experiment in Progress..!!");
        notification = mBuilder.build();
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

        //starting Experiment
        boolean textFileFollow = true;
        String msg = "";

        if (textFileFollow) {
            String fileid;
            try {
                fileid = myintent.getStringExtra("fileid");
            }
            catch (Exception e){
                fileid ="-1";
            }
            String exptid ="-1";
            try {
                exptid = myintent.getStringExtra("exptno");
            }catch (Exception e){
                onDestroy(); //destroy this service
            }
            String controlFile = Threads.getcontrolfile(fileid);
            startForeground(Constants.notifyIdExperiment, notification);
            Log.d(Constants.LOGTAG, " Control file read :  " + controlFile);

            if (controlFile != "") {

                MainActivity.load = RequestEventParser.parseEvents(controlFile , exptid);
                Log.d("Experiment", "total request events : " + MainActivity.load.total_events);
                MainActivity.numDownloadOver = 0;
                MainActivity.currEvent = 0;

                msg += " Setting Up Alarms for the experiment.";
                //send broadcast to trigger alarms
                Intent localIntent = new Intent(Constants.BROADCAST_ALARM_ACTION);
                localIntent.putExtra("eventid", (int) 0); //this is just to trigger first scheduleNextAlarm
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

                Threads.writeLog(Constants.debugLogFilename , msg);
                Log.d(Constants.LOGTAG ,msg);
            }
        }
        return super.onStartCommand(myintent, flags, startId);

    }

    @Override
    public void onDestroy() {
        Log.d(Constants.LOGTAG, "  Foreground Experiment service is destroyed here.");
        SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.running), Constants.false_value);
        editor.commit();
        stopForeground( true);
        super.onDestroy();
    }
}
