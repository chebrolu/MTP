package com.iitb.wicroft;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by swinky on 24/1/17.
 */
public class UpdateManager extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Threads.writeLog(Constants.debugLogFilename , "app has been updated. Starting MainService");
        //The intent is received after the application has been updated, we need to re-start the app(service) here.
        Intent startServiceIntent = new Intent(context, MainService.class);
        context.startService(startServiceIntent);
        Log.d(Constants.LOGTAG , " app has been updated. Starting MainService" );
    }
}
