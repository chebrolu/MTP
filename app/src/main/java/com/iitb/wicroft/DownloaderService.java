package com.iitb.wicroft;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;


/**
 * Created by swinky on 3/7/16.
 */
//called by alarm receiver to start serving next download event
public class DownloaderService extends IntentService{

    Handler uiHandler;

    public DownloaderService() {
        super("DownloaderService");
        // TODO Auto-generated constructor stub
    }


    public void onCreate(){
        super.onCreate();
       uiHandler = new Handler(); // This makes the handler attached to UI Thread
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        String msg=" DownloaderService : ";
        SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        int expt_running = sharedPref.getInt(getString(R.string.running), Constants.false_value);
        if(expt_running==Constants.false_value){
            msg+=" entered. But experiment not running";
            Log.d(Constants.LOGTAG, msg);
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

        if(MainActivity.load == null){
            msg+=" load null";
            Log.d(Constants.LOGTAG, msg);
            Threads.writeLog(Constants.debugLogFilename , msg);
            return;
        }

        Bundle bundle = intent.getExtras();
        final int eventid = bundle.getInt("eventid");
        Log.d(" Downloader Service ", "Event id = "+eventid);
        final RequestEvent event = MainActivity.load.events.get(eventid-1);

        Log.d(Constants.LOGTAG , "THe download mode is"+event.mode);
        msg +=" Handling event " + eventid + "in a thread ... ";

        boolean webviewon = true;
        if(event.mode == DownloadMode.SOCKET){
            Runnable r = new Runnable() {
                public void run() {
                    Threads.HandleEvent(event, getApplicationContext());
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
        else if(event.mode == DownloadMode.WEBVIEW){

            MainActivity.logfilename = "" + MainActivity.load.loadid;
            msg+= "HandleEvent : just entered thread";
            uiHandler.post(new Runnable() {

                @Override
                public void run() {
                    WebView webview = new WebView(getApplicationContext());
                    webview.setWebViewClient(new MyBrowser(eventid, event.url,getApplicationContext()));
                    MainActivity.webViewMap.put(eventid, webview);
                    webview.loadUrl(event.url + "##" + event.postDataSize + "##" + event.type);
                }
            });

        }
        else if(event.mode == DownloadMode.EXO) {
            //start videoplayer service
            Intent videoplayerIntent = new Intent(getApplicationContext(), VideoPlayer.class);
            videoplayerIntent.putExtra("eventid", (int)eventid);
            getApplicationContext().startService(videoplayerIntent);
        }
        else{
            msg+= "Incorrect Download mode specified";
        }

        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename , msg);

        EventAlarmReceiver.completeWakefulIntent(intent);
    }
}