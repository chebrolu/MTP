package com.iitb.wicroft;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by swinky on 3/4/17.
 */

//Implements Change AP functionality : disconnects and connects to the said AP.
public class ChangeAp extends BroadcastReceiver {
    static WifiManager wm;
    @Override
    public void onReceive(Context context, Intent intent) {

        wm = (WifiManager)context.getSystemService(context.WIFI_SERVICE);
        String msg="";
        boolean flag =true;
        String _ssid = intent.getStringExtra("ssid");
        String _username = intent.getStringExtra("username");
        String _password = intent.getStringExtra("password");
        String _type = intent.getStringExtra("type");

        if (_ssid.length() == 0) {
            msg += " NULL values in change AP config";
        }
        else {

            WifiInfo info = wm.getConnectionInfo();
            String currentBssid = info.getBSSID();
            String currentssid = info.getSSID();
            int currentNetworkId = info.getNetworkId();;
            int newNetworkId = 0;

            if(currentssid.equalsIgnoreCase("\"" + _ssid + "\"")){
                msg += "\n Already Connected to Same WiFi Network:"+info.getSSID();
                wm.disconnect();
//               MainActivity.wifimanager.disableNetwork(currentNetworkId);
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:ENABLE NEW NETWORK: " + "\n");
                wm.enableNetwork(currentNetworkId,true);
            }else {

                boolean confFound = false;
                boolean deleteConf = true;
                List<WifiConfiguration> list = wm.getConfiguredNetworks();

                for (WifiConfiguration i : list) {
                    if (i.SSID.equalsIgnoreCase("\"" + _ssid + "\""))// || i.BSSID.equalsIgnoreCase(_bssid)){
                    {
                        newNetworkId = i.networkId;
                        boolean stats = wm.removeNetwork(newNetworkId);
                        if (!stats) {
                            msg += "\n Remove N/w:Failed";
                            deleteConf = false;

                            wm.disconnect();
                            Calendar cal = Calendar.getInstance();
                            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                            Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:LOST: " + "\n");

                            cal = Calendar.getInstance();
                            format1 = new SimpleDateFormat("yyyy-MM-dd");
                            Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:ENABLE NEW NETWORK: " + "\n");

                            wm.enableNetwork(newNetworkId, true);
                            break;
                        }
                        //   confFound = true;
                        //break;
                    }
                }

                if(deleteConf){

                    msg += "\nWiFi Conf :Creating New";//Old network conf deleted");
                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + _ssid + "\"";

                    if (_type.equalsIgnoreCase("open")) { // open network

                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        conf.allowedAuthAlgorithms.clear();
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                    } else if (_type.equalsIgnoreCase("wep")) { // wep network

                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                        conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);


                        if(_password.matches("^[0-9A-F]+$")){
                            conf.wepKeys[0] = _password;
                        }else{
                            conf.wepKeys[0] = "\"".concat(_password).concat("\"");
                        }

                        conf.wepTxKeyIndex = 0;

                    } else if (_type.equalsIgnoreCase("wpa-psk")) { // wpa psk network

                        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                        conf.preSharedKey = "\"".concat(_password).concat("\"");

                    } else if (_type.equalsIgnoreCase("eap")) {  // eap network


                        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                        conf.enterpriseConfig.setIdentity(_username.toString());
                        conf.enterpriseConfig.setPassword(_password.toString());
                        conf.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
                        conf.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);


                    } else {
                        msg += "\nInvalid Type";
                        flag = false;
                    }


                    if (flag == true) {
                        try {

                            int netId = wm.addNetwork(conf);
                            if (netId == -1) {
                                msg += "\n Adding Conf to Network Failed";
                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                                Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:ENABLE NEW NETWORK: " + "\n");

                                wm.enableNetwork(currentNetworkId, true);
                                msg += "\n Connecting to previous WiFi!";
                            } else {
                                msg += "\n Adding Conf to Network Success";
                                wm.disconnect();
                                String msgconn = "Connection lost to AP in EventRunner";
                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                                Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:LOST: " + msgconn + "\n");
                                Threads.writeLog(Constants.debugLogFilename , ": DEBUG_CONNECTION_WIFI_CHANGEAP:LOST: " + msgconn + "\n");

                                wm.saveConfiguration();
                                cal = Calendar.getInstance();
                                format1 = new SimpleDateFormat("yyyy-MM-dd");
                                Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:ENABLE NEW NETWORK: " + "\n");

                                boolean result = wm.enableNetwork(netId, true);
                                if (!result) {
                                    cal = Calendar.getInstance();
                                    format1 = new SimpleDateFormat("yyyy-MM-dd");
                                    Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) + ": DEBUG_CONNECTION_WIFI_CHANGEAP:ENABLE NEW NETWORK: " + "\n");

                                    wm.enableNetwork(currentNetworkId, true);
                                    msg += "\n Connecting to previous WiFi!!";
                                }else{
                                    msg += "\n Enable new COnf : "+result;
                                }
                            }


                        } catch (Exception ex) {
                            msg += "\n Exception : " + ex.toString();
                        }
                    }


                }
                else{
                    wm.enableNetwork(newNetworkId, true);
                    msg += "\n Connecting to existing WiFi conf!!";
                }

            }

            Calendar cal = Calendar.getInstance();
            Intent intentAlarm = new Intent(context, EnsureWifiConnection.class);
            long timeout = 60000 ;  //if doesnot connect in 60 seconds.
            Log.d(Constants.LOGTAG, "THE wait timer is : " + timeout);
            intentAlarm.putExtra("curr_ssid", currentssid);
            intentAlarm.putExtra("curr_networkid" , Integer.toString(currentNetworkId));
            intentAlarm.putExtra("_ssid" , _ssid);
            PendingIntent APintent = PendingIntent.getBroadcast(context, Constants.EnsureWifiRequestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + timeout, APintent);

            cal = Calendar.getInstance();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Threads.writeToLogFile(Constants.connLogFilename, "\n" + format1.format(cal.getTime()) + " " + Utils.sdf.format(cal.getTime()) +"Change AP: "+ msg + "\n");
            Log.d("ChangeAP ",msg);



        }






    }
}
