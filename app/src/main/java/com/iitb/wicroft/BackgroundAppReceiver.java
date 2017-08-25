package com.iitb.wicroft;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by swinky on 7/3/17.
 */

//Received when the foregrounf timer of the app expires and the app is again moved to background here.
public class BackgroundAppReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.LOGTAG, "BackgroundAppReceiver : the alarm recieved");
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.appPref, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(context.getString(R.string.move_to_background), Constants.true_value);
        editor.commit();

        Intent startServiceIntent = new Intent(context, MainService.class);
        context.startService(startServiceIntent);
    }
}
