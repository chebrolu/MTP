package com.iitb.wicroft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.simple.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by swinky on 1/8/16.
 */


public class WifiScan extends BroadcastReceiver {

    static String apInfo = "";

    public static int sendDeviceInfo(Context context , Intent intent){
        JSONObject obj = Utils.getMyDetailsJson(context);
        obj.put("bssidList", apInfo);
        int statuscode = ConnectionManager.writeToStream(obj.toJSONString(),false);
        Threads.writeLog(Constants.debugLogFilename , "HB status code : " + statuscode );
        AlarmReceiver.completeWakefulIntent(intent);
        return  statuscode;
    }

    public void onReceive(final Context ctx, final Intent intent) {
        String msg =" WiFiScan : ";
        String all_bssid_formatted = "";
        WifiManager wm = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
        String wifis[];
        try {
            List<ScanResult> wifiScanList = wm.getScanResults();
            wifis = new String[wifiScanList.size()];

            for (int i = 0; i < wifiScanList.size(); i++) {
                wifis[i] = ((wifiScanList.get(i)).toString());
               //todo : log properly
                all_bssid_formatted += get_bssidInfo(wifis[i]);
            }
            apInfo = all_bssid_formatted;
        } catch (Exception e) {
            msg += "Exception in Scan Results. "+e.toString();

        }

        Threads.writeLog(Constants.debugLogFilename , msg);
        Log.d(Constants.LOGTAG , msg);

        //SENDING HB TO SERVER HERE
        Thread thread = new Thread() {
            public void run() {
                String msg = "WiFiScan : Sending Device Info Thread";

                int status = sendDeviceInfo(ctx , intent);
                msg+= " Status Code"+status;

                if (status != 200) {
                    try {
                        MainActivity.serverConnection = new Socket("wicroft.cse.iitb.ac.in", Constants.serverport);
                        msg += " Creating New Socket and Setting Data Streams ";

                        try {
                            MainActivity.dis = new DataInputStream(MainActivity.serverConnection.getInputStream());
                            MainActivity.dout = new DataOutputStream(MainActivity.serverConnection.getOutputStream());
                        } catch (Exception e) {
                            msg += "\n Exception handling while initalizing datastream objects for a new connection "+ e.toString();
                        }
                    } catch (Exception e) {
                        msg += "\n Exception handling while creating a new connection" + e.toString();
                        e.printStackTrace();
                    }

                } else {
                    msg += " Heartbeat Sent Successfully.";
                }

                Threads.writeLog(Constants.debugLogFilename , msg);
                Log.d(Constants.LOGTAG , msg);

            }
        };
        thread.start();
    }

    public static String get_bssidInfo( String bssid_info)
    {
        String ssid="";
        String bssid="";
        String level="";
        StringTokenizer st = new StringTokenizer(bssid_info,",");
        while (st.hasMoreTokens()) {
            String temp = st.nextToken();
            if(temp.contains("SSID:") && !temp.contains("BSSID:"))
                ssid = temp.replaceAll("SSID: ","" );
            if(temp.contains("BSSID:"))
                bssid = temp.replaceAll("BSSID: ","" );
            if(temp.contains("level:"))
                level = temp.replaceAll("level: ","" );
        }
        return (ssid+","+bssid+","+level+";");
    }


}

