package com.iitb.wicroft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;


/**
 * Created by swinky on 3/7/16.
 */

public class MyBrowser extends WebViewClient {
    static String LOGTAG = "DEBUG_MY_BROWSER";
    static WifiManager wifimanager;
    static boolean selective_logging = false;   //for background-traffic logging or not.
    public int eventid;
    public static Context ctx;
    boolean loggingOn;
    String baseURL;
    int totalResponseTime;

    MyBrowser(int id, String tbaseURL ,Context my_ctx){
        eventid = id;
        ctx = my_ctx;
        wifimanager = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
        loggingOn = true;
        baseURL = tbaseURL;
        totalResponseTime = 0;
        if(MainActivity.load == null) return;
    }

    //stopping the experiment when all requests are done
    public static void wrap_up_experiment(final Context ctx){

        String msg = "Wrapping up Experiment";
        Log.d(LOGTAG, "Now wrapping up the experiment");
        //Dummy ending of all requests - assuming only one request

        msg += "Experiment over : all GET/POST requests completed\n";

        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);

        Thread thread = new Thread() {
            @Override
            public void run() {
                WifiManager wifimanager = (WifiManager)ctx.getSystemService(ctx.WIFI_SERVICE);
                int response = 0;
                String expOver = Utils.getExpOverJson(wifimanager);
                response = ConnectionManager.writeToStream(expOver, true);
                Log.d(LOGTAG, "Experiment Over Signal sent to server:" + Integer.toString(response));
            }
        };
        thread.start();

        SharedPreferences sharedPref = ctx.getSharedPreferences(Constants.appPref, ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(ctx.getString(R.string.running), Constants.false_value);
        editor.commit();    //added just to make sure that the expt is over locally.

        boolean was_running = ctx.stopService(new Intent(ctx, Experiment.class));
        Log.d(Constants.LOGTAG, " Stopping the service from my browser.. : " + was_running);
        Log.d(Constants.LOGTAG, msg);

    }

    public static void cancelDependentEvents(int eid ,String endTimeFormatted , String other_info){
        Queue dep_list = new LinkedList();
        do {
            if (MainActivity.load.url_dependency_graph.containsKey(eid)) {
                Iterator<RequestEvent> itr = MainActivity.load.url_dependency_graph.get(eid).iterator();
                while (itr.hasNext()) {
                    RequestEvent evt = itr.next();
                    dep_list.add(evt.event_id);
                    Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + evt.url +
                            " CANCELLED :due to URL dependency failure of event " + eid + " Start_Time:" + endTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time: 0" + " Error+msg:" +
                            " URL dependency in control file failed." + other_info);

                    MainActivity.numDownloadOver++;
                    Log.d(Constants.LOGTAG ," Incrementing numDownload in cancel Dependency");
                    Log.d("MyBrowser", "numDownloadover : " + MainActivity.numDownloadOver);
                }

            }
            eid=-1;
            if(!dep_list.isEmpty())
                eid = (Integer)dep_list.remove();
        }while(eid!=-1);
        int loadSize = MainActivity.load.events.size();
        int num = MainActivity.numDownloadOver;
        if (num == loadSize) {
            wrap_up_experiment(ctx);
        }
    }


    @Override
    public void  onLoadResource(WebView view, String url)  {
//		logwriter.append("Inside onLoadResource : "+url+" \n");
    }


    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        //logwriter.append("Inside onPageStarted : "+url+ "\n Favicon : " +favicon+" \n");
         super.onPageStarted(view, url, null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(Constants.LOGTAG, "shouldOverrideUrlLoading: "+url+" \n");
        view.loadUrl(url);
        return true;//return true means this webview has handled the request. Returning false means host(system browser) takes over control
    }

    public static void writeToFile(String url){
    }

    @Override
    public WebResourceResponse  shouldInterceptRequest (WebView view, String url){

        if (url.startsWith("http")) {
            WebResourceResponse obj = getResource(eventid ,url);
            return obj;
        }
        Log.d(LOGTAG + "shouldInterceptReqFALSE", "returning NULL " + url);
        return null; //returning null means webview will load the url as usual.
    }


    //for requests for POST type
    public static WebResourceResponse postResource(int eventid ,String url){
        String ip_addr = Utils.getIP(wifimanager);
        String mac_addr = Utils.getMACAddress(wifimanager);
        String bssid,ssid,rssi,linkSpeed;
        bssid=Utils.getBSSID(wifimanager);
        ssid=Utils.getSSID(wifimanager);
        rssi = Integer.toString(Utils.getRSSI(wifimanager));
        linkSpeed = Integer.toString(Utils.getLinkspeed(wifimanager));

        String other_info = " IP:" + ip_addr + " " +
                "MAC:" + mac_addr + " " +
                "RSSI:" + rssi + "dBm " +
                "BSSID:" + bssid + " " +
                "SSID:" + ssid + " " +
                "LINK_SPEED:" + linkSpeed + "Mbps ";

        String  []st = url.split("##");
        url = st[0];

        String newURL = null;
        String debugfile_msg = " MyBrowser :";
        try {
            newURL = getURL(url).toString();
        } catch (MalformedURLException e1){
            debugfile_msg+= "url malformed " + url + " Exception: "+ e1.toString();
            Threads.writeLog(Constants.debugLogFilename ,debugfile_msg);
            return null;
        } catch (URISyntaxException e1){
            debugfile_msg+= "url malformed " + url + " Exception: "+ e1.toString();
            Threads.writeLog(Constants.debugLogFilename ,debugfile_msg);
            return null;
        }

        Calendar start = Utils.getServerCalendarInstance();
        long startTime= start.getTimeInMillis();

        try {
            URL urlObj = new URL(newURL);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setReadTimeout(5000000);         //5000 seconds timeout for reading from input stream, setting to large values so that during nw congestion this factor is not a bottleneck.
            connection.setConnectTimeout(5000000);      //5000 seconds before connection can be established
            connection.setRequestMethod("POST");        //add request header

            int sizeOfData = Integer.parseInt(st[1]);
            String  _str = "";  //genertating dummydata for post
            for (int i = 0;i<sizeOfData-5;i++)
                _str += "A";

            startTime = start.getTimeInMillis();        //start time of the request
            String urlParameters = "data=" + _str;
            connection.setDoOutput(true);               // Send post request
            DataOutputStream wr1 = new DataOutputStream(connection.getOutputStream());
            wr1.writeBytes(urlParameters);
            wr1.flush();
            wr1.close();

            //some error occurred : Log the error
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                debugfile_msg+="PostResource : " + " connection response code error";
                Calendar end = Utils.getServerCalendarInstance();
                String startTimeFormatted =  Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());

                if(selective_logging == true && url.contains("bgtraffic"))
                    ;
                else {
                    Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + url + " Post_Data_Size:" + urlParameters.length() +
                            " ERROR Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (end.getTimeInMillis() - start.getTimeInMillis()) + " " +
                            "Status_Code:" + connection.getResponseCode() + other_info);

                    // caught an error cancel all the events dependent on it , and write log
                    cancelDependentEvents(eventid ,endTimeFormatted ,other_info);


                }

                Log.d(LOGTAG, "HandleEvent : " + " connection response code error");
            }
            else{
                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();

                Log.d(Constants.LOGTAG, "PostResource : " + " filelen " + fileLength);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = input.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                Calendar end = Utils.getServerCalendarInstance();
                long endTime = end.getTimeInMillis();

                buffer.flush();

                byte[] responseData = buffer.toByteArray();

                String startTimeFormatted = Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());
                if(selective_logging == true && url.contains("bgtraffic"))
                    ;
                else {
                    Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + url + " Post_Data_Size:" + urlParameters.length() +
                            " SUCCESS Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (endTime - startTime) + " " +
                            "Received-Content-Length:" + fileLength + other_info);

                    //success of the request , trigger all the alarms related to this request
                    if (MainActivity.load.url_dependency_graph.containsKey(eventid)){
                        Iterator< RequestEvent> itr = MainActivity.load.url_dependency_graph.get(eventid).iterator();
                        while(itr.hasNext()) {
                            RequestEvent evt = itr.next();
                            EventAlarmReceiver.schedule_event(ctx, evt);
                            Log.d("MyBrowser" , "Scheduling next alram" + evt.event_id);

                        }

                    }


                }

                InputStream stream = new ByteArrayInputStream(responseData);
                WebResourceResponse wr = new WebResourceResponse("", "utf-8", stream);
                return wr;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Calendar end = Utils.getServerCalendarInstance();
            long endTime = end.getTimeInMillis();
            String startTimeFormatted =  Utils.sdf.format(start.getTime());
            String endTimeFormatted =  Utils.sdf.format(end.getTime());
            if(selective_logging == true && url.contains("bgtraffic"))
                ;
            else
            Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST "+url+
                    " ERROR :"+e.toString()+" Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (endTime - startTime) + " Error+msg:" +
                    e.getMessage() + other_info);

           // e.printStackTrace();
            debugfile_msg+=" IOException caught in post resource "+e.toString();

            // caught an error cancel all the events dependent on it , and write log
            cancelDependentEvents(eventid ,endTimeFormatted ,other_info);
            }

        Log.d(Constants.LOGTAG, debugfile_msg);
        Threads.writeLog(Constants.debugLogFilename , debugfile_msg);
        return null;
    }

    //for requests for GET type
    public static WebResourceResponse getResource(int eventid , String url){
        //if a post request call postResource else get continue
            if(url.endsWith("##POST")){
            WebResourceResponse obj = postResource(eventid ,url);
            return obj;
        }

        String ip_addr = Utils.getIP(wifimanager);
        String mac_addr = Utils.getMACAddress(wifimanager);
        String bssid,ssid,rssi,linkSpeed;
        bssid=Utils.getBSSID(wifimanager);
        ssid=Utils.getSSID(wifimanager);
        rssi = Integer.toString(Utils.getRSSI(wifimanager));
        linkSpeed = Integer.toString(Utils.getLinkspeed(wifimanager));

        String other_info = " IP:" + ip_addr + " " +
                "MAC:" + mac_addr + " " +
                "RSSI:" + rssi + "dBm " +
                "BSSID:" + bssid + " " +
                "SSID:" + ssid + " " +
                "LINK_SPEED:" + linkSpeed + "Mbps ";



        String []st = url.split("##");
        url = st[0];
        Threads.writeToLogFile(MainActivity.logfilename, "\n\nGET " + url + " ");

        HttpClient client = new DefaultHttpClient();
        HttpGet request = null;
        String newURL = null;
        try {
            newURL = getURL(url).toString();

        } catch (MalformedURLException e1){

            // TODO Auto-generated catch block
            e1.printStackTrace();
            Log.d(LOGTAG + "-MALFORMED", "url malformed " + url);
            return null;
        } catch (URISyntaxException e1){
            // TODO Auto-generated catch block
            e1.printStackTrace();
            Log.d(LOGTAG + "-MALFORMED", "url malformed " + url);
            return null;
        }
        Calendar start = Utils.getServerCalendarInstance();
        long startTime = start.getTimeInMillis();

        try {
            URL urlObj = new URL(newURL);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setReadTimeout(5000000); //5000 seconds timeout for reading from input stream
            connection.setConnectTimeout(5000000); //15000 seconds before connection can be established

            connection.connect();


            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(Constants.LOGTAG, "getResource : " + " connection response code error");
                Calendar end = Utils.getServerCalendarInstance();

                String startTimeFormatted =  Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());

                Threads.writeToLogFile(MainActivity.logfilename, " ERROR Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (end.getTimeInMillis() - start.getTimeInMillis()) + " " +
                        "Status_Code:" + connection.getResponseCode() + other_info);

                // caught an error cancel all the events dependent on it , and write log
                cancelDependentEvents(eventid, endTimeFormatted, other_info);


                Log.d(LOGTAG, "HandleEvent : " + " connection response code error");
            }
            else{
                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();

                Log.d(Constants.LOGTAG, "getResource : " + " filelen " + fileLength);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = input.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                Calendar end = Utils.getServerCalendarInstance();
                long endTime = end.getTimeInMillis();

                buffer.flush();

                byte[] responseData = buffer.toByteArray();

                String startTimeFormatted =  Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());

                Threads.writeToLogFile(MainActivity.logfilename, " SUCCESS Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (endTime - startTime) + " " +
                        "Received-Content-Length:" + fileLength + other_info);

                InputStream stream = new ByteArrayInputStream(responseData);
                WebResourceResponse wr = new WebResourceResponse("", "utf-8", stream);
                return wr;
            }



        } catch (IOException e) {
            // TODO Auto-generated catch block
            Calendar end = Utils.getServerCalendarInstance();
            long endTime = end.getTimeInMillis();
            String startTimeFormatted =  Utils.sdf.format(start.getTime());
            String endTimeFormatted =  Utils.sdf.format(end.getTime());
            Threads.writeToLogFile(MainActivity.logfilename, "ERROR Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (endTime - startTime) + " " +
                   " Error_msg:" + e.getMessage() + other_info);

            // caught an error cancel all the events dependent on it , and write log
            cancelDependentEvents(eventid, endTimeFormatted, other_info);

            e.printStackTrace();
        }
        return null;
    }

    public static URL getURL(String rawURL) throws MalformedURLException, URISyntaxException{
        URL url = new URL(rawURL);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        url = uri.toURL();
        return url;
    }

   /*
   @Override
   public void onPageStarted(WebView view, String url, Bitmap favicon) {
   	//return null;
   }  */

    @Override
    public void onPageFinished(WebView view, String url) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        Log.d("Debugging" , format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + "on page finished url : "+baseURL);

        String debugfile_msg ="MyBrowser: On page finished ";
        debugfile_msg+="called for url " + baseURL;
        Log.d(Constants.LOGTAG, debugfile_msg);
        Threads.writeLog(Constants.debugLogFilename , debugfile_msg);
        super.onPageFinished(view, url);
        if (loggingOn) {
            loggingOn = false; //no more log collection


            Runnable r = new Runnable() {
                public void run() {
                    Log.d(Constants.LOGTAG ," Incrementing numDownload in Finished page");
                    MainActivity.numDownloadOver++;
                    int num = MainActivity.numDownloadOver;
                    if (MainActivity.load == null) {
                        Log.d(Constants.LOGTAG, "DownloaderService : load null");
                        return;
                    }
                    int loadSize = MainActivity.load.events.size();
                    Log.d(Constants.LOGTAG ," The value of numDownloadOver :"+Integer.toString(num) );
                    Log.d(Constants.LOGTAG ," The loadsize is :"+Integer.toString(loadSize) );
                    Threads.writeToLogFile(MainActivity.logfilename, "\n");

                    if (num == loadSize) {
                        Threads.writeToLogFile(MainActivity.logfilename, Constants.EOF); //this indicates that all GET requests have been seen without interruption from either user/server
                    }
                    String msg = "";

                    Log.d(LOGTAG , " num , loadSize"+num+" "+loadSize);
                    if (num == loadSize) {
                        wrap_up_experiment(ctx);
                    }

                MainActivity.removeWebView(eventid); //remove the reference to current this webview so that it gets garbage collected
                Log.d("MyBrowser", "I am done here.. removed form webview... ");
                }
            };

            Thread t = new Thread(r);

            t.start();
        }
            //MainActivity.webview1.setVisibility(View.VISIBLE);
            //MainActivity.progressBar.setVisibility(View.GONE);
            //MainActivity.goButton.setText("GO");
       /*if(!MainActivity.js.isEmpty()){
    	   Log.d(LOGTAG, "onPageFinished() : loading js = " + MainActivity.js);
    	   MainActivity.webview1.loadUrl(MainActivity.js);
    	   MainActivity.js = "";
       }*/
            //MainActivity.textview.setText(MainActivity.js);
        }



}

