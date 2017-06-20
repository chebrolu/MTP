package com.iitb.wicroft;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.app.AlarmManager;
import android.util.Log;
import android.webkit.WebView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {


    public static HashMap<Integer, WebView> webViewMap = new HashMap<Integer, WebView>();
    public static Socket serverConnection = null;
    public static DataInputStream dis = null;
    public static DataOutputStream dout =null;
    static long serverTimeDelta = 0;    //(serverTime - clientTime)
    static int numDownloadOver = 0;     //indicates for how many events download in thread is over
    static Load load = null;            //this stores info about current experiment such as exp id and all events(get requests) with resp scheduled time
    static int currEvent = 0;           //which event is currently being processed
    static Long serverTimeInMillis;
    public static String logfilename;
    static boolean heartbeat_enabled = true ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Writing data to SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.email_flag),Constants.false_value );
        editor.putInt(getString(R.string.running),Constants.false_value );
        editor.putInt(getString(R.string.move_to_background),Constants.false_value );
        editor.putInt(getString(R.string.move_to_foreground),Constants.false_value );
        editor.putInt(getString(R.string.is_running_in_foreground),Constants.false_value );
        editor.commit();

        Log.d(Constants.LOGTAG, "Main Activity : Starting the Mainservice");
        Intent startServiceIntent = new Intent(this, MainService.class);
        startService(startServiceIntent);
    }

    @Override
    public void onBackPressed() {
        Log.d(Constants.LOGTAG, "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();


    }

    public static void reset(Context ctx){
        SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        int expt_running = sharedPref.getInt(ctx.getString(R.string.running), Constants.false_value);
        if(expt_running==Constants.true_value){
            load = null;
            currEvent = 0;
            editor.putInt(ctx.getString(R.string.running),Constants.false_value );
            editor.commit();
            numDownloadOver = 0;
            heartbeat_enabled = true;

            //cancel scheduled alarms
            Intent intent = new Intent(ctx, AlarmReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(ctx, Constants.alarmRequestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
            am.cancel(sender);

            cancelTimeoutAlarm(ctx);
        }
    }


    static void cancelTimeoutAlarm(Context ctx){
        Log.d(Constants.LOGTAG, "MainActivity : cancel timeout alarm ");
        //cancel timeout alarm
        Intent timeoutintent = new Intent(ctx, AlarmReceiver.class);
        PendingIntent timeoutsender = PendingIntent.getBroadcast(ctx, Constants.timeoutAlarmRequestCode, timeoutintent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(timeoutsender);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    synchronized public static void removeWebView(int eventid){
        webViewMap.remove(eventid);
    }
/*
    public void clickexit(View v)
    {
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        onDestroy();
    }
*/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
