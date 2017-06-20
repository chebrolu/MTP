package com.iitb.wicroft;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by swinky on 28/6/16.
 */

//Listens to server for the events to be performed and takes necessary action
public class EventRunner extends AsyncTask< Void , Void, Void> {
    final Context ctx;
    public static AlarmManager my_am;

    public EventRunner(Context app_ctx) {
        ctx = app_ctx;
        my_am = (AlarmManager)ctx.getSystemService(ctx.ALARM_SERVICE);
    }

    protected Void doInBackground(Void... params) {
        String msg ="\n EventRunner : ";

        String data = "";
        Long localTimeInMillis;

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
                Threads.writeLog(Constants.debugLogFilename, " Event Runner : Data received : " + data);
                Map<String, String> jsonMap = Utils.ParseJson(data);
                String action = jsonMap.get(Constants.action);

                if (action.compareTo(Constants.action_connectToAp) == 0) {

                    String _ssid = jsonMap.get(Constants.ssid);
                    String _timer = jsonMap.get(Constants.ap_timer);    //Connect to said SSID after _timer seconds.
                    String _username = jsonMap.get(Constants.username);
                    String _password = jsonMap.get(Constants.password);
                    String _type = jsonMap.get(Constants.security);

                    Threads.writeLog(Constants.connLogFilename , " ssid : "+_ssid+" timer: "+_timer +" username : "+_username+ " password: "+_password+"type : "+_type);

                    Intent intentAlarm = new Intent(ctx, ChangeAp.class);
                    intentAlarm.putExtra("ssid" , _ssid);
                    intentAlarm.putExtra("username" , _username);
                    intentAlarm.putExtra("password" ,_password);
                    intentAlarm.putExtra("type" , _type);
                    PendingIntent APintent = PendingIntent.getBroadcast(ctx,Constants.changeApRequestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

                    my_am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + (Integer.parseInt(_timer)*1000) ,APintent );

                }
                else if(action.compareTo(Constants.action_bringAppInForeground)==0){

                    SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, ctx.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    int duration = Integer.parseInt(jsonMap.get("duration")); //duration in seconds
                    long foreground_timeout =duration*1000;
                    editor.putInt(ctx.getString(R.string.move_to_foreground), Constants.true_value);
                    editor.commit();
                    ctx.startService(new Intent(ctx, MainService.class) );

                    //set an alarm for x seconds; the app will go to background after this much time
                    Intent intentAlarm = new Intent(ctx, BackgroundAppReceiver.class);
                    int is_in_foreground = sharedPref.getInt(ctx.getString(R.string.is_running_in_foreground), Constants.false_value);
                    if(is_in_foreground==Constants.true_value){
                        //cancel the previous alarm before setting a new one.
                        my_am.cancel( PendingIntent.getBroadcast(ctx,  Constants.ForegroundAppRequestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
                    }

                    my_am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + foreground_timeout, PendingIntent.getBroadcast(ctx, Constants.ForegroundAppRequestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

                }

                else if (action.compareTo(Constants.getLogFiles) == 0) {
                    msg +="\n getLogFiles Action received ";
                    Log.d(Constants.LOGTAG, "Eventrunner: get log files msg received..");

                    Runnable r = new Runnable() {
                        public void run() {
                            Threads.sendLogFilesBackground(ctx);
                        }
                    };
                    Thread t = new Thread(r);
                    t.start();
                }

                else if(action.compareTo(Constants.action_controlFile) == 0 ){
                    String control_msg = jsonMap.get(Constants.message).toString();
                    String fileId = jsonMap.get(Constants.fileid).toString();
                    String status = Threads.saveControlFile(fileId, control_msg);
                    if(status != " control file write success\n"){
                        // ack
                        String ack = "{\"action\":\"" + Constants.acknowledgement + "\",\"" + Constants.fileid + "\":\"" + fileId + "\"}";
                        ConnectionManager.writeToStream(ack ,true);
                        Log.d(Constants.LOGTAG, "Control file Ack sent: " + ack);
                    }
                    else{
                        // TODO: negative ack
                    }


                }

                else if (action.compareTo(Constants.startExperiment) == 0) {


                    MainActivity.heartbeat_enabled = false;  //stop heartbeat during an experiment.

                    //start HB timer: Its the time after which the HB service will restart even if it doesn't receive stop experiment signal
                    Calendar cal = Calendar.getInstance();
                    Intent intentAlarm = new Intent(ctx, restartHB.class);
                    long restart_hb_timeout = Integer.parseInt(jsonMap.get(Constants.hb_timer))*1000 ;
                    Log.d(Constants.LOGTAG, "THE HB restart timer is : " + restart_hb_timeout);
                    my_am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + restart_hb_timeout, PendingIntent.getBroadcast(ctx, Constants.restartHBRequestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

                    MyBrowser.selective_logging = Boolean.parseBoolean(jsonMap.get(Constants.selectiveLog));

                    msg+="\n Disabling Heartbeat as experiment is being started. EXP N0 : ";

                    MainActivity.serverTimeInMillis = Long.parseLong(jsonMap.get(Constants.serverTime));
                    localTimeInMillis= Calendar.getInstance().getTimeInMillis();
                    MainActivity.serverTimeDelta = MainActivity.serverTimeInMillis - localTimeInMillis;
                    msg += "\n ServerTimeDelta : "+  MainActivity.serverTimeDelta / 1000 + " seconds";

                    SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, ctx.MODE_PRIVATE);
                    int expt_running = sharedPref.getInt(ctx.getString(R.string.running), Constants.false_value);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    if (expt_running == Constants.true_value) {
                        //this should not happen. As one experiment is already running Send 300 response
                        Log.d(Constants.LOGTAG, "Experiment running but received another control file request");
                        msg+="\n Experiment running but received another control file request";
                        //TODO: Should be reset the experiment here.. problem with receiving stop experiment signal.. never received..
                        return null;
                    }
                    else {
                        editor.putInt(ctx.getString(R.string.running), Constants.true_value);
                        editor.commit();
                        Log.d(Constants.LOGTAG, "Committed the running_variable value ");

                        Intent startExperimentIntent = new Intent(ctx, Experiment.class);
                        startExperimentIntent.putExtra("textfollow", Boolean.parseBoolean((jsonMap.get(Constants.textFileFollow))));
                        startExperimentIntent.putExtra("fileid", "" + jsonMap.get(Constants.fileid).toString());
                        startExperimentIntent.putExtra("exptno", "" + jsonMap.get(Constants.exptid));

                       // int starting_delay = 30; //in seconds
                        long starting_delay =  Integer.parseInt(jsonMap.get(Constants.expStartDelay));
                        PendingIntent pIntent = PendingIntent.getService(ctx, Constants.startExpRequestCode, startExperimentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                      //  my_am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + (starting_delay * 1000), pIntent);

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
                            // for kitkat and above versions
                            my_am.setExact(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + (starting_delay * 1000), pIntent);

                        } else{
                            // for phones running an SDK before kitkat
                            my_am.set(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + (starting_delay * 1000), pIntent);
                        }

                        /*starting foreground service:Foreground service is required because we don't want the activity manager in android to force close the app,
                        which it can do with a background service
                        */

                        final String exptid=jsonMap.get(Constants.exptid);
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                String ack = "{\"action\":\"" + "expack" + "\",\"" + Constants.experimentNumber + "\":\"" + exptid + "\"}";
                                ConnectionManager.writeToStream(ack,true);
                                Log.d(Constants.LOGTAG, " Expt. Ack sent: " + ack);
                                Threads.writeLog(Constants.debugLogFilename,"Expt. Ack sent: " + ack);

                            }
                        };
                        thread.start();

                        Log.d(Constants.LOGTAG, "Set alarm for experiment ");

                    }

                } else if (action.compareTo(Constants.action_stopExperiment) == 0) {
                    Log.d(Constants.LOGTAG, "MainActivity.running boolean set to false. Reset()");
                    msg+=" Stop Experiment Received : ";
                    Log.d(Constants.LOGTAG, " Stopping the service from my event runnner : stop expt received.. : ");
                    boolean was_running = ctx.stopService(new Intent(ctx, Experiment.class));
                    SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, ctx.MODE_PRIVATE);
                    int expt_running = sharedPref.getInt(ctx.getString(R.string.running), Constants.false_value);
                    if (expt_running==Constants.true_value && MainActivity.load != null) {
                        msg +=" Calling MainActivity.reset() ";
                        MainActivity.reset(ctx);
                    }
                }
                else if(action.compareTo(Constants.action_updateAvailable) == 0){

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

                    PendingIntent intent2 = PendingIntent.getActivity(ctx, Constants.updateAvailableRequestCode,myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(intent2);
                    NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

                    mNotificationManager.notify(1, mBuilder.build());

                }

                else {
                    //Log.d(Constants.LOGTAG, "eventRunner() : Wrong action code");
                    msg +=" Wrong Action Code ";
                }

            }

        } catch (Exception e) {

            //Somehow experiment could not be started due to some IOException in socket transfer. So again reset running variable to false

            SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, ctx.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(ctx.getString(R.string.running),Constants.false_value );
            editor.commit();
            msg+= "\n Somehow experiment could not be started due to some Exception: " + e.toString();
            e.printStackTrace();
        }


        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename , msg);
        return null;

    }

    @Override
    protected void onProgressUpdate(Void... values) {

        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        EventRunner runEvent = new EventRunner(ctx);
        runEvent.execute();

    }
}

