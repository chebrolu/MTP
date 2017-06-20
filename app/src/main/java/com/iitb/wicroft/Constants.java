package com.iitb.wicroft;

import android.os.Environment;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class Constants {

	public static final String SERVLET_NAME = "wicroft";
	public static final String appPref = "appPref";
	public static final String server = "wicroft.cse.iitb.ac.in";

	public static int serverport = 8001;
	public static int port = 8080;
	public static SimpleDateFormat sdf = new SimpleDateFormat("ZZZZ HH:mm:s:S", Locale.US);
	public static int heartbeat_duration = 60; //by default the device sends one heartbeat per 60 seconds.

	public static boolean debugging_on = true;
	public static String debugLogFilename = "Debug_logs";
	public static String  connLogFilename = "ConnectionLog";

	static final String action = "action";
	static final String serverTime = "serverTime";
	static final String macAddress = "macAddress";
	static final String numberOfCores = "numberOfCores";
	static final String memory = "memory";				//in MB
	static final String processorSpeed = "processorSpeed";		//in GHz
	static final String storageSpace = "storageSpace";		//in MB
	static final String action_controlFile = "controlFile";
	static final String startExperiment = "startExperiment";
	static final String fileid ="fileid";
	static final String textFileFollow = "textFileFollow";
	static final String action_stopExperiment = "stopExperiment";
	static final String action_updateAvailable = "action_updateAvailable";
	static final String action_bringAppInForeground = "wakeup";
	static final String getLogFiles = "getLogFiles";
	static final String experimentOver = "expOver";
	static final String hb_timer = "timeout";
	static final String selectiveLog = "selectiveLog";
	static final String security ="security";
	static final String exptid = "exptid";
	static final String message = "message";
	static final String acknowledgement = "ack";
	static final String experimentNumber ="exp";
	static final String rssi = "rssi";
	static final String bssid = "bssid";
	static final String ap_timer = "timer";
	static final String ssid = "ssid";
	static final String linkSpeed = "linkSpeed";

	static final int alarmRequestCode = 192837;
	static final int timeoutAlarmRequestCode = 123343;
	public static final int hbRequestCode = 123;

	static final String logDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WiCroft/logs";
	static final String controlFileDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WiCroft/controlFiles";
	static final String action_connectToAp = "apSettings";
	static final String username = "username";
	static final String password = "pwd";
	static final String expStartDelay = "expStartTime";
	
	//temporary
	static final String LOGTAG = "WiCroft";
	static final String LINEDELIMITER = "* * * * * *\n";
	static final String SUMMARY_PREFIX = "### ";
	static final String EOF = "\nEOF\n";

	static final int notifyIdMainService = 11111;
	static final int notifyIdExperiment = 1234;
	
	// Defines a custom intent for alarm receiver
	public static final String BROADCAST_ALARM_ACTION = "com.iitb.WiCroft.BROADCAST_ALARM";
	
	// Defines a custom Intent action
  public static final String BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST";
    
    // Defines the key for the status "extra" in an Intent
  public static final String BROADCAST_MESSAGE = "com.example.android.threadsample.STATUS";
    
    public static final int timeoutConnection = 3000; //timeout in milliseconds until a connection is established.
    public static final int timeoutSocket = 5000; //timeout for waiting for data.


	public static final int changeApRequestCode=1986;
	public static final int ForegroundAppRequestCode=1112;
	public static final int restartHBRequestCode=66666;
	public static final int updateAvailableRequestCode=7777;
	public static final int EnsureWifiRequestCode = 10007;
	public static final int startExpRequestCode = 786786;

	public static final int true_value = 1;
	public static final int false_value = 0;
}
