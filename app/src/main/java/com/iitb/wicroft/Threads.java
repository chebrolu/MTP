package com.iitb.wicroft;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class Threads {

    //send the log file specified by given name
    @SuppressWarnings("deprecation")
    static int sendLog(String logFileName){
        int statusCode = 404;

        String temp_name = Utils.getMACAddress() ; //this would be the name at the server
        String logFilePath = Constants.logDirectory + "/" + logFileName;
        String url = "http://" + "wicroft.cse.iitb.ac.in" + ":" + MainActivity.port + "/" + Constants.SERVLET_NAME + "/receiveLogFile.jsp";
        Log.d(Constants.LOGTAG, "Upload url " + url);
//		String url = "http://192.168.0.107/fup.php";

        File logFile = new File(logFilePath);
        if(!logFile.exists()){
            Log.d(Constants.LOGTAG, "sendLog : File not found " + logFilePath + " May be sent earlier");
            return 200; //already sent sometime earlier
        }

        MultipartEntity mpEntity  = new MultipartEntity();
        HttpClient client = Utils.getClient();

        try {
            mpEntity.addPart("expID", new StringBody(logFileName));

            if(logFileName.equals(MainActivity.debugfilename) || logFileName.equals("ConnectionLog") ) {

                SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
                String format = s.format(new Date());
                temp_name +="_"+format;

            }
            mpEntity.addPart(Constants.macAddress, new StringBody(temp_name));
            mpEntity.addPart("file", new FileBody(logFile));

            HttpPost httppost = new HttpPost(url);
            httppost.setEntity(mpEntity);
            try {
                HttpResponse response = client.execute( httppost );
                statusCode = response.getStatusLine().getStatusCode();
                if(statusCode == 200){
                    Log.d(Constants.LOGTAG, "Log file named " + logFileName + " deleted");
                    logFile.delete(); //now deleting log file
                }
                else{

                    Log.d(Constants.LOGTAG, "Sending Log file " + logFileName + " failed with statuscode : "+statusCode);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return statusCode;
    }


    //write log content to log file specified
    static synchronized String writeToLogFile(String logfilename, String log){
        File logfile = new File(MainActivity.logDir, logfilename);
        long length = logfile.length();
        Log.d("Logfile_write" , "File size in bytes"+length);
        if(length >= 51200) //50KB
        {
            Log.d("Logfile_write" , "****deleting****file** : Size limit exceeded.");
            logfile.delete();
            logfile = new File(MainActivity.logDir, logfilename);

        }
        BufferedWriter logwriter = null;
        String msg = "";
        try {
            logwriter = new BufferedWriter(new FileWriter(logfile, true));
            logwriter.append(log);
            logwriter.close();
            msg += " local log write success\n";
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            Log.d(Constants.LOGTAG, "HandleEvent() : can't open log file for writing " + logfilename);
            msg += " Couldn't open log file " + logfilename;
            e1.printStackTrace();
        }
        return msg;
    }


    //save control file sent by server, using fileid received as filename

    static synchronized String saveControlFile(String controlfilename, String controlinfo){
        File controlfile = new File(MainActivity.controlDir, controlfilename);
        if(controlfile.exists())
            controlfile.delete();
        BufferedWriter controlwriter = null;
        String msg = "";
        try {
            controlwriter = new BufferedWriter(new FileWriter(controlfile, true));
            controlwriter.write(controlinfo);
           // controlwriter.append(controlinfo);
            controlwriter.close();
            msg += " control file write success\n";
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            Log.d(Constants.LOGTAG, "HandleEvent() : can't open control file for writing " + controlfilename);
            msg += " Couldn't open control file " + controlfilename;
            e1.printStackTrace();
        }
        return msg;
    }

    //read and return control file when experiment start received.
    static String getcontrolfile(String controlFileName){
        String file = "";
        String controlFilePath = Constants.controlFileDirectory + "/" + controlFileName;

        File controlFile = new File(controlFilePath);
        if(!controlFile.exists()){
            Log.d(Constants.LOGTAG, "getcontrolFile : File not found " + controlFilePath);

        }

        try {
            FileInputStream fis = new FileInputStream(controlFile);
            byte[] data = new byte[(int) controlFile.length()];
            fis.read(data);
            fis.close();
            String str = new String(data, "UTF-8");
            file+=str;
        }
        catch (Exception ex){
            Log.d(Constants.LOGTAG , "Exception caught in file reading");

        }

        return file;
    }





    //completes request specified in given event(identified by eventid)
    //also writes log to logfile about progress
    //if all downloads over, send the log file
    static int HandleEvent(RequestEvent event, final Context context){
        //Log file will be named   <eventid> . <loadid>
        if(!MainActivity.running){
            Log.d(Constants.LOGTAG, "HandleEvent : But experiment not running");
            return -1;
        }


        Load currentLoad = MainActivity.load;

        if(currentLoad == null){
            return -1;
        }

        //RequestEvent event = currentLoad.events.get(eventid);
        String logfilename = "" + currentLoad.loadid;


        Log.d(Constants.LOGTAG, "HandleEvent : just entered thread");

        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        String filename = "unknown"; //file name of file to download in request
        //BufferedWriter logwriter = null;
        boolean success = false;

        Calendar startTime = null, endTime = null;
        long responseTime = -1;
        StringBuilder logwriter = new StringBuilder();

        try {
            URL url = new URL(event.url);

            logwriter.append("details: " + currentLoad.loadid + " " + event.event_id + " SOCKET" + "\n");
            logwriter.append("url: " + url + "\n");

            filename = event.url.substring(event.url.lastIndexOf('/') + 1);

            Log.d(Constants.LOGTAG, "HandleEvent : " + event.url + " " + filename);

            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000); //10 seconds timeout for reading from input stream
            connection.setConnectTimeout(10000); //10 seconds before connection can be established

            //note start time
            startTime = Utils.getServerCalendarInstance();

            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(Constants.LOGTAG, "HandleEvent : " + " connection response code error");
                endTime = Utils.getServerCalendarInstance();

                String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
                String endTimeFormatted =  Utils.sdf.format(endTime.getTime());

                logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [ERROR] " + "[ET = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
                        "[code " + connection.getResponseCode() + "]" + "\n");
                logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
            }
            else{
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                logwriter.append("length: " + Integer.toString(fileLength) + " \n");

                Log.d(Constants.LOGTAG, "HandleEvent : " + " filelength " + fileLength);

                // download the file
                input = connection.getInputStream();
                Log.d(Constants.LOGTAG,"storinghere... "+Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);
                output = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                int oldprogress = 0, currprogress = 0;

                Log.d(Constants.LOGTAG, "HandleEvent : " + " file opened on sd card");
                logwriter.append(Utils.getTimeInFormat() + " " + currprogress + "% " + total + "\n"); //first progress 0%
                while ((count = input.read(data)) != -1) {
                    //				Log.d(Constants.LOGTAG, "HandleEvent : " + " Received chunk of size " + count);
                    total += count;

                    // publishing the progress....
                    if (fileLength > 0){ // only if total length is known
                        currprogress = (int) (total * 100 / fileLength);
                        if(currprogress > oldprogress){
                            oldprogress = currprogress;
                            Log.d(Constants.LOGTAG, currprogress + "% " + total + "\n");
                            logwriter.append(Utils.getTimeInFormat() + " " + currprogress + "% " + total + "\n");
                        }
                        //publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
                //File download over
                success = true;

                //note end time take the difference as response time
                endTime = Utils.getServerCalendarInstance();

                responseTime = endTime.getTimeInMillis() - startTime.getTimeInMillis();
                String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
                String endTimeFormatted =  Utils.sdf.format(endTime.getTime());
                logwriter.append("RT " +  responseTime + "\n");
                logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [SUCCESS] " + "[RT = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
                        "[content-length = " + fileLength + "]" + "\n");

                logwriter.append("success\n");
                logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log

            }
        } catch (IOException e) {
            endTime = Utils.getServerCalendarInstance();

            String startTimeFormatted =  Utils.sdf.format(startTime.getTime());
            String endTimeFormatted =  Utils.sdf.format(endTime.getTime());

            logwriter.append(Constants.SUMMARY_PREFIX + event.url + " [ERROR] " + "[ET = " + (endTime.getTimeInMillis()-startTime.getTimeInMillis()) + "]" + " [" + startTimeFormatted + " , " + endTimeFormatted + "] " +
                    "[" + e.getMessage() + "]" + "\n");
            logwriter.append("failure\n");
            logwriter.append(Constants.SUMMARY_PREFIX + Constants.LINEDELIMITER); //this marks the end of this log
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }

        String msg = "GET #" + event.event_id + " File : " + filename;
        if(!success) msg += "FAILED connection problem/timeout";
        else msg += " SUCCESS with RT=" + responseTime + "\n";


        int num = MainActivity.numDownloadOver++;
        Log.d(Constants.LOGTAG, "handle event thread : END . Incrementing numDownloadOver to " + MainActivity.numDownloadOver + " #events is "+ currentLoad.events.size());
        if(num+1 == currentLoad.events.size()){
            //send the consolidated log file
            String n = Integer.toString(currentLoad.events.size());
            msg += "Experiment over : all GET requests (" + n + " of " + n + ") completed\n";
            //msg += "Trying to send log file\n";

            logwriter.append(Constants.EOF); //this indicates that all GET requests have been seen without interruption from either user/server

            String logString = logwriter.toString(); //get the content of stringbuilder into a string

            String retmsg = writeToLogFile(logfilename, logString); //write the log to file. This is a synchronized operation, only one thread can do it at a time
            msg += retmsg;

            int response = 0;
            String expOver = Utils.getExpOverJson();
            // String expOver = "{\"action\":\"expOver\",\"ip\":" + MainActivity.myIp + "\",\"port\":" + MainActivity.myPort + "\",\"macAddress\":" + Utils.getMACAddress() + "\"}";
            response = ConnectionManager.writeToStream(expOver);
            Log.d(Constants.LOGTAG, "Experiment Over Signal sent to server:" + Integer.toString(response));

            MainActivity.running = false;
            //added just to make sure that the expt is over.. locally..

            boolean was_running =  MainActivity.context.stopService(MainActivity.startExperimentIntent);
            Log.d(Constants.LOGTAG, " Stopping the service from my browser.. : "+ was_running);

            // Send stop experiment..
            ConnectionManager.writeToStream(Constants.experimentOver);

        }
        else{//just write the log to log file
            String logString = logwriter.toString(); //get the content of stringbuilder into a string

            String retmsg = writeToLogFile(logfilename, logString); //write the log to file. This is a synchronized operation, only one thread can do it at a time
            msg += retmsg;
        }

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.BROADCAST_MESSAGE, msg);

        // Broadcasts the Intent to receivers in this application.
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

        return 0;
    }


    //send log files pending. this is called from a background thread.
    //looks into log folder and sends all log files(except that of current experiment)
    static void sendLogFilesBackground(final Context ctx){
        File storage = new File(Constants.logDirectory); //log dir has already been created in onCreate
        File[] files = storage.listFiles();
        int sent = 0;
        int errors = 0;

        String currExpLogFile = "-1" ;
        if(MainActivity.load != null) {
            currExpLogFile = Long.toString(MainActivity.load.loadid);
        }

        for(int i=0; i<files.length; i++){
            File c = files[i];
            String logFileName = c.getName();


            if(!logFileName.equals(currExpLogFile)){ //pending log file is not current experiment's log
                int status = Threads.sendLog(logFileName);
                if(status == 200){
                    sent++;
                }
                else{
                    errors++;
                }
            }
        }
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.BROADCAST_MESSAGE,
                        "Background log file sending : success "+ sent + " Fail " + errors + "\n");

        // Broadcasts the Intent to receivers in this application.
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
    }





}
