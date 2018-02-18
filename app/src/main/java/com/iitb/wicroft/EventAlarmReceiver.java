package com.iitb.wicroft;

/**
 * Created by swinky on 3/7/16.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

//Triggered when alarm is received. Handles two events : normal alarm event to process next download event using DownloaderService
public class EventAlarmReceiver extends WakefulBroadcastReceiver
{
    // Prevents instantiation
    public EventAlarmReceiver() {
    }

    // Called when the BroadcastReceiver gets an Intent it's registered to receive
    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String msg = "EventAlarm Receiver : ";
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.appPref, context.MODE_PRIVATE);
        int expt_running = sharedPref.getInt(context.getString(R.string.running), Constants.false_value);

        if(expt_running==Constants.false_value){
            Log.d(Constants.LOGTAG, "Alarm Receiver : alarm just received. But experiment not running");
            msg +=" alarm just received. But experiment not running ";
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

        int eventid = bundle.getInt("eventid");

        if(eventid > 0){
           Log.d(Constants.LOGTAG, "Alarm Receiver : alarm just received (eventid=" + eventid + ") Now preparing to handle event");

            msg+="\n alarm just received (eventid=" + eventid + ") Now preparing to handle event";
            Intent callingIntent = new Intent(context, DownloaderService.class);
            callingIntent.putExtra("eventid", (int)eventid);
            startWakefulService(context, callingIntent);
            msg+=" Started the Downloader Service";
        }
        else{
            msg += " eventid=" + eventid + " Setting up first alarm";
        }
        Log.d(" EventAlarmReceiver:" , " MainActivity.currEvent =  "+MainActivity.currEvent);
        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);
        scheduleNextAlarm(context);
        }


    //Looks at next event from eventlist and schedules next alarm
   void scheduleNextAlarm(Context context){
        String msg = "EventAlarmeceiver - ScheduleNextAlarm ";
       SharedPreferences sharedPref = context.getSharedPreferences(Constants.appPref, context.MODE_PRIVATE);
       int expt_running = sharedPref.getInt(context.getString(R.string.running), Constants.false_value);

        if(expt_running==Constants.false_value){
            msg+= "scheduleNextAlarm : Experiment not 'running'";
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

        if(MainActivity.load == null){
            msg+= "scheduleNextAlarm : load null";
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

        if(MainActivity.currEvent >= MainActivity.load.independent_events.size()) {
            msg+= "scheduleNextAlarm : All independent events alarms over.";
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

       AlarmManager am = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        Log.d("EventAlarmRx :nextalarm" , " MainActivity.currEvent =  "+MainActivity.currEvent);
        RequestEvent e = new RequestEvent( MainActivity.load.independent_events.get(MainActivity.currEvent) ) ;
        Intent intent = new Intent(context, EventAlarmReceiver.class);
        intent.putExtra("eventid", (int) e.event_id);

       Random r = new Random();
       int i1 = r.nextInt(10000 - 0) + 0;
       PendingIntent sender = PendingIntent.getBroadcast(context, Constants.alarmRequestCode+i1, intent, PendingIntent.FLAG_UPDATE_CURRENT);


       msg +="\n e.cal time in millisec : "+ Long.toString(e.cal.getTimeInMillis()) + "MainActivity.serverTimeDelta : " + Long.toString(MainActivity.serverTimeDelta);
        msg+="\n Scheduling " + e.event_id + "@" + Constants.sdf.format(e.cal.getTime());
        msg+= "\n current time in ms :" + Long.toString(Calendar.getInstance().getTimeInMillis());
        msg+= "\n alarmwakeup in ms"+ Long.toString(e.cal.getTimeInMillis());
       if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
           // for kitkat and above versions
           am.setExact(AlarmManager.RTC_WAKEUP, e.cal.getTimeInMillis(), sender);
       } else{
           // for phones running an SDK before kitkat
           am.set(AlarmManager.RTC_WAKEUP, e.cal.getTimeInMillis(), sender);
       }

        MainActivity.currEvent++;
        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);


    }


    //To schedule dependent events on completion of an event
    public static void schedule_event(Context context , RequestEvent e){

        AlarmManager am = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        Intent intent = new Intent(context, EventAlarmReceiver.class);
        intent.putExtra("eventid", (int)e.event_id);
        Random r = new Random();
        int i1 = r.nextInt(10000 - 0) + 0;
        PendingIntent sender = PendingIntent.getBroadcast(context, 123456+i1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String msg = "Schedule_dependency_event";
        msg+="\n Scheduling " + e.event_id + "@ " + e.relative_time;

        long t = e.relative_time*1000;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + t, sender);
        }
        else{
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + t, sender);
        }

        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);

    }
}