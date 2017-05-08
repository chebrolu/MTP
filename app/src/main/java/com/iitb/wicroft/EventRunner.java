package com.iitb.wicroft;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Calendar;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import com.rampo.updatechecker.notice.Notice;
import com.rampo.updatechecker.UpdateChecker;
import com.rampo.updatechecker.store.Store;

import android.content.pm.PackageManager;

/**
 * Created by swinky on 28/6/16.
 */

public class EventRunner extends AsyncTask< Void , Void, Void> {
    final Context ctx;
    public static AlarmManager my_am;

    public EventRunner(Context app_ctx) {
        ctx = app_ctx;
    }

    protected Void doInBackground(Void... params) {
        String msg ="\n EventRunner : ";

        String data = "";
        Long localTimeInMillis;// = Calendar.getInstance().getTimeInMillis();

        try {

            data = ConnectionManager.readFromStream();

            if (data == null || data.equals("")) {
                // This happens when some exception caught in readFromStream()

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException ex) {
                    msg +=" Exception caught in sleep. " + ex.toString();
                   // Log.d(Constants.LOGTAG, ex.toString());
                }
            } else {
                msg += "Data Received from Server : "+data;

                Calendar cal1 = Calendar.getInstance();
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
                Threads.writeToLogFile(MainActivity.debugfilename , format2.format(cal1.getTime()) + " " + Utils.sdf.format(cal1.getTime())+ " Event Runner : Data received : "+data);

                Map<String, String> jsonMap = Utils.ParseJson(data);
                String action = jsonMap.get(Constants.action);
               // Log.d(Constants.LOGTAG, "###  ### ### serverTimeDelta = " + MainActivity.serverTimeDelta / 1000 + " seconds");


                if (action.compareTo(Constants.action_connectToAp) == 0) {



                    String _ssid = jsonMap.get(Constants.ssid);
                    String _timer = jsonMap.get(Constants.ap_timer);
                    String _username = jsonMap.get(Constants.username);
                    String _password = jsonMap.get(Constants.password);
                    String _type = jsonMap.get(Constants.security);

                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    Threads.writeToLogFile("ConnectionLog" , format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime())+ " ssid : "+_ssid+" timer: "+_timer +" username : "+_username+ " password: "+_password+"type : "+_type );
                    //start wait timer
                    cal = Calendar.getInstance();
                    Intent intentAlarm = new Intent(ctx, ChangeAp.class);
                    long timeout = Integer.parseInt(_timer)*1000 ;
                    Log.d(Constants.LOGTAG, "THE AP wait timer is : " + timeout);
                    intentAlarm.putExtra("ssid" , _ssid);

                    intentAlarm.putExtra("username" , _username);
                    intentAlarm.putExtra("password" ,_password);
                    intentAlarm.putExtra("type" , _type);

                    PendingIntent APintent = PendingIntent.getBroadcast(ctx, 1986, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
                   // APintent.putExtra("fileid", "" + jsonMap.get(Constants.fileid).toString());

                    my_am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + timeout,APintent );




                }
                else if(action.compareTo(Constants.action_bringAppInForeground)==0){

                    int duration = Integer.parseInt(jsonMap.get("duration")); //duration in seconds
                    long foreground_timeout =duration*1000;
                    //Intent startServiceIntent = new Intent(ctx, ForegroundAppService.class);
                    //ctx.startService(startServiceIntent);
                    MainActivity.app_in_foreground = true;
                    Log.d("EventRunner" , " I have started Hearbeat service in Foreground.!!");
                    //start heartbeat service as normal
                    Intent startServiceIntent = new Intent(ctx, Heartbeat.class);
                    ctx.startService(startServiceIntent);
                    Log.d("Eventrunner ", " New service started*********..");




                    //set an alarm for x seconds; the app will go to background after this much time

                    Calendar cal = Calendar.getInstance();
                    Intent intentAlarm = new Intent(ctx, BackgroundAppReceiver.class);


                    if(MainActivity.is_running_in_foreground==true){
                        //cancel the previous alarm before setting a new one.
                        my_am.cancel( PendingIntent.getBroadcast(ctx, 1112, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
                    }
                    Log.d(Constants.LOGTAG, "THE Foreground timer is : " + foreground_timeout);
                    my_am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + foreground_timeout, PendingIntent.getBroadcast(ctx, 1112, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));



                }

                else if (action.compareTo(Constants.getLogFiles) == 0) {
                    msg +="\n getLogFiles Action received ";
                    Log.d("Eventrunner ", " get log files msg received..");


                    try {
                        final String logFileName = Long.toString(MainActivity.load.loadid);
                        Runnable r = new Runnable() {
                            public void run() {
                                Threads.sendLog(logFileName);
                            }
                        };
                        Thread t = new Thread(r);
                        t.start();
                    } catch (NullPointerException e) {
                        // e.printStackTrace();
                        // Log.d("send log", "null pointer execption");
                        msg +="\n Send Log Null Pointer Exception : " + e.toString();
                        msg +="\n Sending all pending log files. ";
                        //sending pending log files
                        Runnable r = new Runnable() {
                            public void run() {
                                Threads.sendLogFilesBackground(ctx);
                            }
                        };
                        Thread t = new Thread(r);
                        t.start();

                    }


                }
                else if(action.compareTo(Constants.action_controlFile) == 0 ){
                    String control_msg = jsonMap.get(Constants.message).toString();
                    String fileid = jsonMap.get(Constants.fileid).toString();

                    String status = Threads.saveControlFile(fileid, control_msg);
                    if(status != " control file write success\n"){
                        // ack
                        String ack = "{\"action\":\"" + Constants.acknowledgement + "\",\"" + Constants.fileid + "\":\"" + fileid + "\"}";
                        ConnectionManager.writeToStream(ack);
                        Log.d("Debugging", "Control file Ack sent: " + ack);
                    }
                    else{
                        //negative ack
                    }


                }

                else if (action.compareTo(Constants.startExperiment) == 0) {
                    // foreground...

                    //stop heartbeat during an experiment.
                    MainActivity.heartbeat_enabled = false;

                    //start HB timer: Its the time after which the HB service will restart even if it doesn't receive stop experiment signal
                    Calendar cal = Calendar.getInstance();
                    Intent intentAlarm = new Intent(ctx, restartHB.class);
                    long restart_hb_timeout = Integer.parseInt(jsonMap.get(Constants.hb_timer))*1000 ;
                    Log.d(Constants.LOGTAG, "THE HB restart timer is : " + restart_hb_timeout);
                    my_am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + restart_hb_timeout, PendingIntent.getBroadcast(ctx, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

                    MyBrowser.selective_logging = Boolean.parseBoolean(jsonMap.get(Constants.selectiveLog));

                    msg+="\n Disabling Heartbeat as experiment is being started. EXP N0 : ";

                    MainActivity.serverTimeInMillis = Long.parseLong(jsonMap.get(Constants.serverTime));
                    localTimeInMillis= Calendar.getInstance().getTimeInMillis();
                    MainActivity.serverTimeDelta = MainActivity.serverTimeInMillis - localTimeInMillis;

                    File logfile = new File(MainActivity.logDir, MainActivity.debugfilename);
                    logfile.delete();
                    Log.d("Event Runner", "log file deleted");


                    msg += "\n ServerTimeDelta : "+  MainActivity.serverTimeDelta / 1000 + " seconds";
                    if (MainActivity.running == true) {
                        //this should not happen. As one experiment is already running Send 300 response
                        Log.d(Constants.LOGTAG, "Experiment running but received another control file request");
                        msg+="\n Experiment running but received another control file request";
                        //TODO: Should be reset the experiment here.. problem with receiving stop experiment signal.. never received..
                        return null;
                    }
                    MainActivity.running = true;
                    //Log.d(Constants.LOGTAG, "Controlfile receieved...");
                    MainActivity.startExperimentIntent = new Intent(ctx, Experiment.class);
                    MainActivity.startExperimentIntent.putExtra("textfollow", Boolean.parseBoolean((jsonMap.get(Constants.textFileFollow))));
                    MainActivity.startExperimentIntent.putExtra("fileid", "" + jsonMap.get(Constants.fileid).toString());
                    MainActivity.exptno = Integer.parseInt(jsonMap.get(Constants.exptid));

                   // Log.d("Event Runner : ", jsonMap.get(Constants.fileid).toString());
                    /*starting foreground service:
                    Foreground service is required because we don't want the activity manager in android to force close the app,
                    which it can do with a background service
                    */
                    ctx.startService(MainActivity.startExperimentIntent);


                } else if (action.compareTo(Constants.action_stopExperiment) == 0) {
                    Log.d(Constants.LOGTAG, "MainActivity.running boolean set to false. Reset()");
                    msg+=" Stop Experiment Received : ";

                    boolean was_running =  MainActivity.context.stopService(MainActivity.startExperimentIntent);
                    Log.d(Constants.LOGTAG, " Stopping the service from my event runnner : stop expt received.. : "+ was_running);

                    if (MainActivity.running && MainActivity.load != null) {

                        msg +=" Calling MainActivity.reset() ";
                        MainActivity.reset(ctx);
                    }

                }
                else if(action.compareTo(Constants.action_updateAvailable) == 0){
                    //start the activity here
                  //  Notification.show(ctx ,Store.GOOGLE_PLAY, 7786);




                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(ctx)
                                    .setSmallIcon(R.mipmap.wicroft)
                                    .setContentTitle("WiCroft : Update Available")
                                    .setContentText("Click here to update to latest version");

                    mBuilder.setSound(alarmSound);
                    mBuilder.setAutoCancel(true);
                    final String appPackageName = ctx.getPackageName(); // getPackageName() from Context or Activity object
                    Intent myIntent;

                    try {
                        myIntent =new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));

                    } catch (android.content.ActivityNotFoundException anfe) {
                        myIntent =new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));

                    }

                    myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    PendingIntent intent2 = PendingIntent.getActivity(ctx, 1,myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(intent2);
                    NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

                    mNotificationManager.notify(1, mBuilder.build());




                    /*
                    Intent startUpdateService = new Intent(ctx, UpdateService.class);
                    ctx.startService(startUpdateService);
                    Log.d("EventRunner" , " started the service...");
                    */

                }

                //these funcationalities are not used
                else if(action.compareTo(Constants.action_hbDuration) == 0) //change heartbeat duration
                {
                    MainActivity.heartbeat_duration = Integer.parseInt(jsonMap.get(Constants.hearbeat_duration));
                   // Heartbeat.Heartbeat_scheduleAlarm();
                }
                else if(action.compareTo(Constants.action_changeServer) == 0) //change server-ip&port duration
                {
                    /*
                    if(jsonMap.get(Constants.serverip)!="")
                        MainActivity.serverip = jsonMap.get(Constants.serverip);
                    if(jsonMap.get(Constants.connport)!="")
                        MainActivity.serverport = Integer.parseInt(jsonMap.get(Constants.connport));
                    if(jsonMap.get(Constants.serverport)!="")
                        MainActivity.port = Integer.parseInt(jsonMap.get(Constants.serverport));
*/

                }
                else {
                    //Log.d(Constants.LOGTAG, "eventRunner() : Wrong action code");
                    msg +=" Wrong Action Code ";
                }

            }

        } catch (Exception e) {

            //Somehow experiment could not be started due to some IOException in socket transfer. So again reset running variable to false
            MainActivity.running = false;
            msg+= "\n Somehow experiment could not be started due to some IOException in socket transfer : " + e.toString();
           // e.printStackTrace();
        }


        Log.d(Constants.LOGTAG, msg);
        if(MainActivity.debugging_on ) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + msg);
        }
        return null;

    }

    @Override
    protected void onProgressUpdate(Void... values) {

        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {

      /*  String msg = "\n EventRunner : On post execute";
        Log.d(Constants.LOGTAG, msg);
        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + msg);
        }
        */
        EventRunner runEvent = new EventRunner(ctx);
        runEvent.execute();

    }
}

