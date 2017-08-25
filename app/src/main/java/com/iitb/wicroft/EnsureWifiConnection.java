package com.iitb.wicroft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by swinky on 4/4/17.
 */

//If  phone hasn't connected even after 60 seconds of changeAP command, ensure to connects to some Ap.
public class EnsureWifiConnection extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);

        Log.d("EnsureWifiConn"," Alarm Received");
        String msg="";
            try{

                WifiInfo info = wifimanager.getConnectionInfo();
                String _ssid = intent.getStringExtra("_ssid");
                String currentssid = intent.getStringExtra("curr_ssid");
                int currentNetworkId = Integer.parseInt(intent.getStringExtra("curr_networkid"));
                msg += "\n SSid1  "+_ssid;
                msg += "\n SSidP  "+info.getSSID();
                msg += "\n SSid   "+currentssid;


                if(info == null || (!info.getSSID().equalsIgnoreCase("\"" + _ssid + "\"") && !info.getSSID().equalsIgnoreCase(currentssid))){
                    msg += "\n Connecting to old AP ";
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:Enable.Connecting to old AP after sleep. " + "\n");

                    wifimanager.enableNetwork(currentNetworkId, true);
                }else{
                    msg += "\n Connected to Correct AP ";
                }
            }catch (Exception ex){
                msg += "\n Exception : " + ex.toString();
            }


    }
}
