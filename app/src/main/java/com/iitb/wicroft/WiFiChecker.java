package com.iitb.wicroft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

        // TODO: This method is called when the BroadcastReceiver is receiving

        // an Intent broadcast.

        String action = intent.getAction();

        // for identifying disconnection, re-connetion to wifi AP (when wifi is enabled)
        if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){


            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            Log.d(Constants.LOGTAG, "***************TYPE_WIFI*********"+ConnectivityManager.TYPE_WIFI);

            if (netInfo != null && netInfo.getType()==ConnectivityManager.TYPE_WIFI){

                int ip;
                String bssid ="";
                    try {
                         ip = MainActivity.wifimanager.getConnectionInfo().getIpAddress();
                        bssid = MainActivity.wifimanager.getConnectionInfo().getBSSID();

                    } catch (Exception e) {
                        Log.d(Constants.LOGTAG, "Wifichecker: getIP() :  Exception caught " + e.toString());
                        ip =0;
                    }


                    MainActivity.myIp = Formatter.formatIpAddress(ip);
                    MainActivity.bssid = bssid;

                String msg = "IP:"+MainActivity.myIp+" BSSID:"+MainActivity.bssid;
                if(MainActivity.debugging_on) {
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    if(MainActivity.running == false)
                        Threads.writeToLogFile("ConnectionLog" ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:CONNECTED "+msg+"\n");
                    else
                        Threads.writeToLogFile(MainActivity.logfilename ,"\n** "+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:CONNECTED "+msg+"\n");


                }

                //MainActivity.output.append("\n WifiReceiver Have Wifi Connection");

                /*
                connection/re-connection to AP is happens.
                here need to reconnect to the server [update the socket]
                 */

            }else{
                /*
                When connection to AP is lost.
                 */
               // MainActivity.output.append("\n WifiReceiver Don't have Wifi Connection");
                String msg = "Connection lost to AP" + " IP: "+MainActivity.myIp+" BSSID: "+MainActivity.bssid;
                if(MainActivity.debugging_on) {
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    if(MainActivity.running == false)
                        Threads.writeToLogFile("ConnectionLog" ,"\n"+format1.format(cal.getTime()) +" "+ Utils.sdf.format(cal.getTime())+": DEBUG_CONNECTION_WIFI:LOST: "+msg+"\n");
                    else {
                        Threads.writeToLogFile(MainActivity.logfilename, "\n** " + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI:LOST: " + msg + "\n");

                    }
                }
                Log.d(Constants.LOGTAG,"\n WifiReceiver Don't have Wifi Connection");


            }




            // for identifying enabling disabling of wifi option
        }else if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){


            int _action = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            switch(_action){
                case WifiManager.WIFI_STATE_DISABLED: // when WiFi of phone is disabled is disabled

                    //MainActivity.output.append("\n WIFI_STATE_DISABLED");

                    /*

                    Here need to check whether any experiment is running,
                    if there is any exp running keep the wifi enabled by following
                    stmt.


                    WifiManager wifimgr= (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    wifimgr.setWifiEnabled(true);

                     */


                    break;
                case WifiManager.WIFI_STATE_DISABLING:;
                    //MainActivity.output.append("\n WIFI_STATE_DISABLING");
                    break;
                case WifiManager.WIFI_STATE_ENABLED:;
                   // MainActivity.output.append("\n WIFI_STATE_ENABLED");
                    break;
                case WifiManager.WIFI_STATE_ENABLING:;
                   // MainActivity.output.append("\n WIFI_STATE_ENABLING");
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:;
                   // MainActivity.output.append("\n WIFI_STATE_UNKNOWN");
                    break;
            }


        }else {
           // MainActivity.output.append("\n::::::Other Actions::::::");
        }


    }
}
