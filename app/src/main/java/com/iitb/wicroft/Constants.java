package com.iitb.wicroft;

import android.os.Environment;


public class Constants {
	static final String action = "action";
	static final String serverTime = "serverTime";
	static final String sessionID = "sessionID";
	static final String ip = "ip";
	static final String port = "port";
	static final String osVersion = "osVersion";
	static final String wifiVersion = "wifiVersion";
	static final String macAddress = "macAddress";
	static final String numberOfCores = "numberOfCores";
	static final String memory = "memory";				//in MB
	static final String processorSpeed = "processorSpeed";		//in GHz
	static final String wifiSignalStrength = "wifiSignalStrength";
	static final String storageSpace = "storageSpace";		//in MB
	static final String packetCaptureAppUsed = "packetCaptureAppUsed";
	static final String action_controlFile = "controlFile";
	static final String startExperiment = "startExperiment";
	static final String fileid ="fileid";
	static final String textFileFollow = "textFileFollow";
	static final String action_stopExperiment = "stopExperiment";
	static final String action_clearRegistration = "clearRegistration";
	static final String action_refreshRegistration = "refreshRegistration";
	static final String action_hbDuration = "hbDuration";
	static final String action_changeServer = "action_changeServer";
	static final String action_updateAvailable = "action_updateAvailable";
	static final String action_bringAppInForeground = "wakeup";
	static final String serverip = "serverip";
	static final String connport = "connectionport";
	static final String serverport = "serverport";
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
	static final String hearbeat_duration = "hearbeat_duration";

	static final int alarmRequestCode = 192837;
	static final int timeoutAlarmRequestCode = 123343;
	//static final int killTimeoutDuration = 180; //in minutes

	static final String logDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WiCroft/logs";
	static final String controlFileDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WiCroft/controlFiles";
	static final String action_connectToAp = "apSettings";
	static final String username = "username";
	static final String password = "pwd";
	
	//temporary
	static final String LOGTAG = "WiCroft";
	static final String LINEDELIMITER = "* * * * * *\n";
	static final String SUMMARY_PREFIX = "### ";
	static final String EOF = "\nEOF\n";
	
	// Defines a custom intent for alarm receiver
	public static final String BROADCAST_ALARM_ACTION = "com.iitb.WiCroft.BROADCAST_ALARM";
	
	// Defines a custom Intent action
  public static final String BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST";
    
    // Defines the key for the status "extra" in an Intent
  public static final String BROADCAST_MESSAGE = "com.example.android.threadsample.STATUS";
    
    public static final int timeoutConnection = 3000; //timeout in milliseconds until a connection is established.
    public static final int timeoutSocket = 5000; //timeout for waiting for data.
    public static final String SERVLET_NAME = "serverplus";
    public static final String SHARED_PREFS = "serverDetails";
    public static final String keyServerAdd = "serverAdd";
    public static final String keyServerPort = "serverPort";
    public static final String keyServerSession = "serverSession";
}
