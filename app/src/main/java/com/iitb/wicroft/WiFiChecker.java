package com.iitb.wicroft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WiFiChecker extends BroadcastReceiver {
    public WiFiChecker() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        WifiManager wm = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.appPref, context.MODE_PRIVATE);
        int expt_running = sharedPref.getInt(context.getString(R.string.running), Constants.false_value);

        // for identifying disconnection, re-connetion to wifi AP (when wifi is enabled)
        if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){

            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            Log.d(Constants.LOGTAG, "*TYPE_WIFI* : "+ConnectivityManager.TYPE_WIFI);

            if (netInfo != null && netInfo.getType()==ConnectivityManager.TYPE_WIFI){

                String ip;
                String bssid ="";
                    try {
                        ip = Utils.getIP(wm);
                        bssid = Utils.getBSSID(wm);

                    } catch (Exception e) {
                        Log.d(Constants.LOGTAG, "Wifichecker: getIP() :  Exception caught " + e.toString());
                        ip ="0";
                    }


                String msg = "IP:"+ip+" BSSID:"+bssid;

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                if(expt_running == Constants.false_value)
                    Threads.writeToLogFile(Constants.connLogFilename ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:CONNECTED "+msg+"\n");
                else
                    Threads.writeToLogFile(MainActivity.logfilename ,"\n** "+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:CONNECTED "+msg+"\n");

            }else{
               // When connection to AP is lost.
                String ip;
                String bssid ="";
                try {
                    ip = Utils.getIP(wm);
                    bssid = Utils.getBSSID(wm);
                } catch (Exception e) {
                    Log.d(Constants.LOGTAG, "Wifichecker: getIP() :  Exception caught " + e.toString());
                    ip ="0";
                }
                String msg = "Connection lost to AP" + " IP: "+ip+" BSSID: "+bssid;

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                if(expt_running == Constants.false_value)
                    Threads.writeToLogFile(Constants.connLogFilename ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:LOST: "+msg+"\n");
                else
                    Threads.writeToLogFile(MainActivity.logfilename, "\n** " + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI:LOST: " + msg + "\n");

                Log.d(Constants.LOGTAG,"\n WifiReceiver Don't have Wifi Connection");
            }
            // for identifying enabling disabling of wifi option
        }else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
            int _action = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            switch(_action){
                case WifiManager.WIFI_STATE_DISABLED: // when WiFi of phone is disabled is disabled
                    /* //Here need to check whether any experiment is running,if there is any exp running keep the wifi enabled by following
                    stmt.
                    WifiManager wifimgr= (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    wifimgr.setWifiEnabled(true);
                     */
                    break;
                case WifiManager.WIFI_STATE_DISABLING:;
                    break;
                case WifiManager.WIFI_STATE_ENABLED:;
                    break;
                case WifiManager.WIFI_STATE_ENABLING:;
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:;
                    break;
            }


        }else {
           // other Actions
        }


    }
}
