package com.iitb.wicroft;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
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

        final int myID = 1234;
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
        // boolean textFileFollow = myintent.getBooleanExtra("textfollow", true);
        // boolean textFileFollow = Boolean.parseBoolean((String) jsonMap.get(Constants.textFileFollow));
        String msg = "";

        if (textFileFollow) {
            String fileid = myintent.getStringExtra("fileid");
            Log.d("Foreground", "Control file received setting up alarms..!!!");
            String controlFile = Threads.getcontrolfile(fileid);

            msg += " Setting Up Alarms for the experiment.";
            startForeground(myID, notification);

            Log.d(" Experiment ", " Control file read :  " + controlFile);


            if (controlFile != "") {

                MainActivity.load = RequestEventParser.parseEvents(controlFile);
                Log.d("Experiment", "total request events : " + MainActivity.load.total_events);
                MainActivity.numDownloadOver = 0;
                MainActivity.currEvent = 0;

                Log.d("Foreground", "Setting up alarms..........here.....");
                //send broadcast to trigger alarms
                Intent localIntent = new Intent(Constants.BROADCAST_ALARM_ACTION);
                localIntent.putExtra("eventid", (int) 0); //this is just to trigger first scheduleNextAlarm
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
                Log.d("Foreground", "Local intent fired");
                //restart the alarm for session timeout
                // MainActivity.setKillTimeoutAlarm(ctx);


                String ack = "{\"action\":\"" + "expack" + "\",\"" + Constants.experimentNumber + "\":\"" + MainActivity.load.loadid + "\"}";
                ConnectionManager.writeToStream(ack);
                Log.d("Debugging", " Ack sent: " + ack);

                msg += "\n Written Ack:  " + ack + "for Experiment_no : " + MainActivity.load.loadid;

            }
        }

        return super.onStartCommand(myintent, flags, startId);

    }

    @Override
    public void onDestroy() {

            Log.d(" Foreground ", " The service is destroying here.");
            MainActivity.running = false;
            stopForeground( true);
        super.onDestroy();
    }
}
