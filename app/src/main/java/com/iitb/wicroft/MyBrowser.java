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
    static long SECONDS_MILLISECONDS = 1000;
    static String LOGTAG = "DEBUG_MY_BROWSER";
    static WifiManager wifimanager;
    static boolean selective_logging = false;   //for background-traffic logging or not.

    public static String js;


    public int eventid;
    public static Context ctx;
    static StringBuffer logwriter  = new StringBuffer();
   // static StringBuilder logwriter  = new StringBuilder();
    boolean loggingOn;
    String baseURL;
    int totalResponseTime;
    Calendar pageStartTime = null;

    MyBrowser(int id, String tbaseURL ,Context my_ctx){
        eventid = id;
        ctx = my_ctx;
        logwriter = new StringBuffer();
        loggingOn = true;
        baseURL = tbaseURL;
        totalResponseTime = 0;
        if(MainActivity.load == null) return;
        //logwriter.append("\ndetails: " + MainActivity.load.loadid + " " + eventid + " WEBVIEW" + "\n");
        //logwriter.append("url:" + baseURL + " ");
    }

    @Override
    public void  onLoadResource(WebView view, String url)  {
//		logwriter.append("Inside onLoadResource : "+url+" \n");
    }

    /*
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        Log.d("MyBrowser" , "On receive error called");
        super.onReceivedError(view, request, error);
    }
    */

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        //logwriter.append("Inside onPageStarted : "+url+ "\n Favicon : " +favicon+" \n");

            super.onPageStarted(view, url, null);


    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        //  logwriter.append("Inside SHOULD OVERRIDE URL LOADING : "+url+" \n");
        Log.d(LOGTAG, "Inside SHOULD OVERRIDE URL LOADING : "+url+" \n");

        view.loadUrl(url);
        return true;
        //return true means this webview has handled the request. Returning false means host(system browser) takes over control
    }


    public static void writeToFile(String url){

        //	logwriter.append("Check URL : "+url+" \n");
    }

    @Override
    public WebResourceResponse  shouldInterceptRequest (WebView view, String url){

        //  logwriter.append("Inside SHOULD INTERCEPT REQUEST : "+url+" \n");
        //Log.d(LOGTAG + "-shouldInterceptReques-THREADID", url + " on " + android.os.Process.myTid());
        if (url.startsWith("http")) {
            //logwriter.append("Inside HTTP \n");
            //Log.d(LOGTAG + "-shouldInterceptRequest TRUE", "url= " +  url);
            WebResourceResponse obj = getResource(eventid ,url);
            //  		logwriter.append("Inside SHOULD INTERCEPT Response: "+obj+" \n");
            return obj;
        }
        Log.d(LOGTAG + "shouldInterceptReqFALSE", "returning NULL " + url);
        return null; //returning null means webview will load the url as usual.
    }



    public static WebResourceResponse postResource(int eventid ,String url){
        String ip_addr = Utils.getIP();
        String mac_addr = Utils.getMACAddress();
        String my_port = "";
        String  []st = url.split("##");
        url = st[0];

       // HttpClient client = new DefaultHttpClient();
      //  HttpGet request = null;
        String newURL = null;
        String debugfile_msg = " MyBrowser :";
        try {
            newURL = getURL(url).toString();
        } catch (MalformedURLException e1){
            //Log.d(LOGTAG + "-MALFORMED", "url malformed " + url);
            debugfile_msg+= "url malformed " + url + " Exception: "+ e1.toString();
            return null;
        } catch (URISyntaxException e1){
           // Log.d(LOGTAG + "-MALFORMED", "url malformed " + url);
            debugfile_msg+= "url malformed " + url + " Exception: "+ e1.toString();
            return null;
        }

        Calendar start = Utils.getServerCalendarInstance();
        long startTime= start.getTimeInMillis();


        try {
            URL urlObj = new URL(newURL);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setReadTimeout(5000000); //5000 seconds timeout for reading from input stream
            connection.setConnectTimeout(5000000); //5000 seconds before connection can be established

            my_port = Integer.toString(urlObj.getPort());
            Log.d("MyBrowser" , "my_port "+my_port);
            //add reuqest header
            connection.setRequestMethod("POST");

            int sizeOfData = Integer.parseInt(st[1]);
            String  _str = "";
            for (int i = 0;i<sizeOfData-5;i++){
                _str += "A";
            }
            startTime = start.getTimeInMillis();

            String urlParameters = "data=" + _str;
            // Send post request
            connection.setDoOutput(true);
            DataOutputStream wr1 = new DataOutputStream(connection.getOutputStream());
            wr1.writeBytes(urlParameters);
            wr1.flush();
            wr1.close();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                //Log.d(Constants.LOGTAG, "getResource : " + " connection response code error");
                debugfile_msg+="PostResource : " + " connection response code error";
                Calendar end = Utils.getServerCalendarInstance();

                String startTimeFormatted =  Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());

                if(selective_logging == true && url.contains("bgtraffic"))
                    ;
                else {
                    Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + url + " Post_Data_Size:" + urlParameters.length() +
                            " ERROR Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (end.getTimeInMillis() - start.getTimeInMillis()) + " " +
                            "Status_Code:" + connection.getResponseCode() + " IP:" + ip_addr + " " + 
                            "MAC:" + mac_addr + " " +
                            "RSSI:" + MainActivity.rssi + "dBm " +
                            "BSSID:" + MainActivity.bssid + " " +
                            "SSID:" + MainActivity.ssid + " " +
                            "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");


                    Log.d("testing" , "Handling event id : "+eventid);
                    Queue dep_list = new LinkedList();
                    int eid = eventid;
                    // caught an error cancel all the events dependent on it , and write log
                    do {
                        if (MainActivity.load.url_dependency_graph.containsKey(eid)) {
                            Log.d("testing" , "Dependencies are present  for event id : "+eventid);
                            Iterator<RequestEvent> itr = MainActivity.load.url_dependency_graph.get(eventid).iterator();
                            while (itr.hasNext()) {
                                RequestEvent evt = itr.next();
                                dep_list.add(evt.event_id);
                                Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + evt.url +
                                        " CANCELLED :due to URL dependency failure of event " + eid + " Start_Time:" + endTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time: 0" + " Error+msg:" +
                                        " URL dependency in control file failed." + " IP:" + ip_addr + " " +
                                        "MAC:" + mac_addr + " " +
                                        "RSSI:" + MainActivity.rssi + "dBm " +
                                        "BSSID:" + MainActivity.bssid + " " +
                                        "SSID:" + MainActivity.ssid + " " +
                                        "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");

                                MainActivity.numDownloadOver++;
                                Log.d("MyBrowser", "numDownloadover : " + MainActivity.numDownloadOver);
                            }

                        }
                        if(!dep_list.isEmpty())
                            eid = (Integer)dep_list.remove();
                    }while(!dep_list.isEmpty());

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
                            "Received-Content-Length:" + fileLength + " IP:" + ip_addr + " " +
                            "MAC:" + mac_addr + " " +
                            "RSSI:" + MainActivity.rssi + "dBm " +
                            "BSSID:" + MainActivity.bssid + " " +
                            "SSID:" + MainActivity.ssid + " " +
                            "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");

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
                    e.getMessage() + " IP:" + ip_addr + " " + 
                    "MAC:" + mac_addr + " " +
                    "RSSI:" + MainActivity.rssi + "dBm " +
                    "BSSID:" + MainActivity.bssid + " " +
                    "SSID:" + MainActivity.ssid + " " +
                    "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");

           // e.printStackTrace();
            debugfile_msg+=" IOException caught in post resource "+e.toString();

            /*
             * caught an error cancel all the events dependent on it , and write log
             */
            Queue dep_list = new LinkedList();
            int eid = eventid;
            // caught an error cancel all the events dependent on it , and write log
            do {
                if (MainActivity.load.url_dependency_graph.containsKey(eid)) {
                    Iterator<RequestEvent> itr = MainActivity.load.url_dependency_graph.get(eventid).iterator();
                    while (itr.hasNext()) {
                        RequestEvent evt = itr.next();
                        dep_list.add(evt.event_id);
                        Threads.writeToLogFile(MainActivity.logfilename, "\n\nPOST " + evt.url +
                                " CANCELLED :due to URL dependency failure of event " + eid + " Start_Time:" + endTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time: 0" + " Error+msg:" +
                                " URL dependency in control file failed." + " IP:" + ip_addr + " " + 
                                "MAC:" + mac_addr + " " +
                                "RSSI:" + MainActivity.rssi + "dBm " +
                                "BSSID:" + MainActivity.bssid + " " +
                                "SSID:" + MainActivity.ssid + " " +
                                "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");

                        MainActivity.numDownloadOver++;
                        Log.d("MyBrowser", "numDownloadover : " + MainActivity.numDownloadOver);
                    }

                }
                if(!dep_list.isEmpty())
                    eid = (Integer)dep_list.remove();
            }while(!dep_list.isEmpty());

            }

        Log.d(Constants.LOGTAG, debugfile_msg);
        if(MainActivity.debugging_on) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + debugfile_msg);
        }
        return null;
    }


    public static WebResourceResponse getResource(int eventid , String url){
        String ip_addr = Utils.getIP();
        String my_port ="";
        String mac_addr = Utils.getMACAddress();
        Log.d("Debugging " , " **************printing the url here******* url : "+url);
        if(url.endsWith("##POST")){

            WebResourceResponse obj = postResource(eventid ,url);
            return obj;
        }

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
            connection.setReadTimeout(10000); //10 seconds timeout for reading from input stream
            connection.setConnectTimeout(10000); //10 seconds before connection can be established

            connection.connect();
            my_port = Integer.toString( urlObj.getPort() );

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(Constants.LOGTAG, "getResource : " + " connection response code error");
                Calendar end = Utils.getServerCalendarInstance();

                String startTimeFormatted =  Utils.sdf.format(start.getTime());
                String endTimeFormatted =  Utils.sdf.format(end.getTime());

                Threads.writeToLogFile(MainActivity.logfilename, " ERROR Start_Time:" + startTimeFormatted + " End_time:" + endTimeFormatted + " Response_Time:" + (end.getTimeInMillis() - start.getTimeInMillis()) + " " +
                        "Status_Code:" + connection.getResponseCode() + " IP:" + ip_addr + " " +
                        "MAC:" + mac_addr + " " +
                        "RSSI:" + MainActivity.rssi + "dBm " +
                        "BSSID:" + MainActivity.bssid + " " +
                        "SSID:" + MainActivity.ssid + " " +
                        "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");


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
                        "Received-Content-Length:" + fileLength + " IP:" + ip_addr + " " + 
                       "MAC:" + mac_addr + " " +
                       "RSSI:" + MainActivity.rssi + "dBm " +
                       "BSSID:" + MainActivity.bssid + " " +
                       "SSID:" + MainActivity.ssid + " " +
                       "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");

	/*			wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = wifimanager.getConnectionInfo();
				int rssi = info.getRssi();
				String bssid = info.getBSSID();
				int frequency = info.getFrequency();   // MHz
				String  ssid = info.getSSID();
				int linkSpeed = info.getLinkSpeed();   // Mbps */



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
                   " Error_msg:" + e.getMessage() + " IP:" + ip_addr + " " + 
                    "MAC:" + mac_addr + " " +
                    "RSSI:" + MainActivity.rssi + "dBm " +
                    "BSSID:" + MainActivity.bssid + " " +
                    "SSID:" + MainActivity.ssid + " " +
                    "LINK_SPEED:" + MainActivity.linkSpeed + "Mbps ");


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

            //  		logwriter.append("Inside On PAGE Finished : "+url+"\n");
           // Log.d(LOGTAG, "########## onPageFinished() called for url " + baseURL);
        debugfile_msg+="called for url " + baseURL;
        Log.d(Constants.LOGTAG, debugfile_msg);
        if(MainActivity.debugging_on) {
            cal = Calendar.getInstance();
            format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + debugfile_msg);
        }

            super.onPageFinished(view, url);

            if (loggingOn) {
                loggingOn = false; //no more log collection

                Runnable r = new Runnable() {
                    public void run() {
                        int num = MainActivity.numDownloadOver++;
                        if (MainActivity.load == null) {
                            Log.d(Constants.LOGTAG, "DownloaderService : load null");
                            return;
                        }
                        int loadSize = MainActivity.load.events.size();
                        //		logwriter.append(Constants.SUMMARY_PREFIX + "summary total RT = " +  totalResponseTime + "\n");
                        //		logwriter.append("success\n");
                        //		logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
                        Threads.writeToLogFile(MainActivity.logfilename, "\n");

                        if (num + 1 == loadSize) {
                            Threads.writeToLogFile(MainActivity.logfilename, Constants.EOF); //this indicates that all GET requests have been seen without interruption from either user/server
                        }
                        String msg = "";

                        Log.d(LOGTAG , " num , loadSize"+num+" "+loadSize);
                        if (num + 1 == loadSize) {
                            Log.d(LOGTAG, "Now wrapping up the experiment");
                            //Dummy ending of all requests - assuming only one request

                            msg += "Experiment over : all GET/POST requests completed\n";
                            //msg += "Trying to send log file\n";


                            Log.d(Constants.LOGTAG, msg);
                            if(MainActivity.debugging_on) {
                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                                Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + msg);
                            }

                            int response = 0;
                            String expOver = Utils.getExpOverJson();
                           // String expOver = "{\"action\":\"expOver\",\"ip\":" + MainActivity.myIp + "\",\"port\":" + MainActivity.myPort + "\",\"macAddress\":" + Utils.getMACAddress() + "\"}";
                            response = ConnectionManager.writeToStream(expOver);
                            Log.d(LOGTAG, "Experiment Over Signal sent to server:" + Integer.toString(response));

                            MainActivity.running = false;
                            //added just to make sure that the expt is over.. locally..

                           boolean was_running =  MainActivity.context.stopService(MainActivity.startExperimentIntent);
                            Log.d(Constants.LOGTAG, " Stopping the service from my browser.. : "+ was_running);

                            Log.d(Constants.LOGTAG, msg);
                            if(MainActivity.debugging_on) {
                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                                Threads.writeToLogFile(MainActivity.debugfilename, format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + "My Browser :Experiment Over Signal sent to server:" + Integer.toString(response));
                            }

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

