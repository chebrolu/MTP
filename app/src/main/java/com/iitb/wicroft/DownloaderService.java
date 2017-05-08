package com.iitb.wicroft;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
        if(!MainActivity.running){
            msg+=" entered. But experiment not running";
            return;
        }

        if(MainActivity.load == null){
            msg+=" load null";
            return;
        }

        //Log.d(Constants.LOGTAG, "DownloaderService : just entered");
        Bundle bundle = intent.getExtras();
        final int eventid = bundle.getInt("eventid");
        Log.d(" Downloader Service ", "Event id = "+eventid);
        final RequestEvent event = MainActivity.load.events.get(eventid-1);

        //final RequestEvent e = MainActivity.load.events.get(eventid);

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


            //final String url = event.url;
            MainActivity.logfilename = "" + MainActivity.load.loadid;

            msg+= "HandleEvent : just entered thread";


            uiHandler.post(new Runnable() {

                @Override
                public void run() {

                    WebView webview = new WebView(getApplicationContext());
                    webview.setWebViewClient(new MyBrowser(eventid, event.url,getApplicationContext()));
                    // WebSettings settings = webview.getSettings();
                    //settings.setJavaScriptEnabled(true);
                    //settings.setJavaScriptEnabled(false);
                    //settings.setPluginsEnabled(false);

                    MainActivity.webViewMap.put(eventid, webview);
                    webview.loadUrl(event.url + "##" + event.postDataSize + "##" + event.type);


                }
            });

        }
        else{
            msg+= "Incorrect Download mode specified";
        }

        Log.d(Constants.LOGTAG, msg);
        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + msg);
        }

        EventAlarmReceiver.completeWakefulIntent(intent);
    }
}