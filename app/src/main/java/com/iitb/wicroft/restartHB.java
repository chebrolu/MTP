package com.iitb.wicroft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by swinky on 14/9/16.
 */


public class restartHB extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //This intent is received when the HB-timer specified with start-expt command is over. HB is not sent to server during the expt, soas to not impact the environment
        MainActivity.heartbeat_enabled = true;
        Threads.writeLog(Constants.debugLogFilename , " restartHB: HBtimer over, retsrting HB here");
    }
}
