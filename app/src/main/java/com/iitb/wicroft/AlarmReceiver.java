package com.iitb.wicroft;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by swinky on 16/6/16.
 */

//Heartbeat alarm receiver: (tiggered every minute)
public class AlarmReceiver extends WakefulBroadcastReceiver {

    // Prevents instantiation
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        String msg = "Alarm Received.  ";
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.d(Constants.LOGTAG, " Alarm receiver BOOT_COMPLETE.....");
            msg += "BOOT_COMPLETE. Starting Utils.NewConnection().execute() . Starting Heartbeat Service. ";
            Threads.writeLog(Constants.debugLogFilename, msg);
            new Utils.NewConnection().execute();
            // Set the alarm here.
            Intent startServiceIntent = new Intent( context , MainService.class);
            context.startService( startServiceIntent);
            return;
        }

        if(MainActivity.serverConnection == null) {
            Log.d(Constants.LOGTAG," In alarm Receiver server connection is null ");
            msg +=" MainActivity.serverConnection is null. Starting Utils.NewConnection().execute() .  ";
            new Utils.NewConnection().execute();
        }

        Intent callingIntent = new Intent(context, DeviceInfo.class);
        startWakefulService(context,callingIntent);

        Log.d(Constants.LOGTAG,msg);
        Threads.writeLog(Constants.debugLogFilename, msg);
    }

}
