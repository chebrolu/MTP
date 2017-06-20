package com.iitb.wicroft;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.app.IntentService;
import android.content.Intent;
import android.util.Patterns;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * Created by swinky on 16/6/16.
 */


public class DeviceInfo extends IntentService {


    public DeviceInfo() {
        super("DeviceInfo");
        // TODO Auto-generated constructor stub
    }

    protected void send_user_email(String mac){

        //getting an email information of the user
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(this).getAccounts();
        String possibleEmail = "";
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                possibleEmail = account.name;
                Log.d(Constants.LOGTAG, "DeviceInfo : Account Info : " + possibleEmail);
            }
        }
        String email_msg = "{\"action\":\"" + "userEmail" + "\",\"" + "macAddress"+"\":\"" +mac + "\",\""  + "email" + "\":\"" + possibleEmail + "\"}";
        int write_status;
        write_status = ConnectionManager.writeToStream(email_msg,false);
        if(write_status == 200 ){
            SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.email_flag),Constants.true_value );
            editor.commit();
        }

    }


    @Override
    protected void onHandleIntent(Intent intent) {

        WifiManager wm = (WifiManager) this.getSystemService(this.WIFI_SERVICE);
        String mac =Utils.getMACAddress(wm);
        SharedPreferences sharedPref = getSharedPreferences(Constants.appPref, MODE_PRIVATE);
        int email_flag = sharedPref.getInt(getString(R.string.email_flag), Constants.false_value);

        //send the email
        if(email_flag==Constants.false_value )
            send_user_email( mac);

        String msg = "DeviceInfo : ";
        if(MainActivity.heartbeat_enabled) {
            msg += "Starting Wifi Scan.";
            try {
                wm.startScan();
            } catch (NullPointerException e) {
                msg+= " Starting scan : wifimanager is null";
            }
        }
        else
            msg += "Heartbeat is Disabled.";

        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);
    }



}
