package com.iitb.wicroft;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
//import android.app.ActivityManager;
//import android.content.Context;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileFilter;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
//import java.io.UnsupportedEncodingException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.ContainerFactory;

import android.util.Log;


/**
 * Created by swinky on 16/6/16.
 */
public class Utils {


    //time formatter
    static SimpleDateFormat sdf = new SimpleDateFormat("ZZZZ HH:mm:s.S", Locale.US);


    //returns current time in proper format as defined above
    static String getTimeInFormat(){
        Calendar cal = Utils.getServerCalendarInstance();
        return sdf.format(cal.getTime());
    }

    //pings the given network
    public static boolean ping(String net){;
        Runtime runtime = Runtime.getRuntime();
        try
        {
            String pingcommand = "/system/bin/ping -c 1 " + net;
            Process  mIpAddrProcess = runtime.exec(pingcommand);
            int exitValue = mIpAddrProcess.waitFor();
            if(exitValue==0){ //exit value 0 means normal termination
                return true;
            }else{
                return false;
            }
        }
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
        return false;
    }

    //returns httpclient object setting the default timeout params
    static HttpClient getClient(){

        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, Constants.timeoutConnection);
        HttpConnectionParams.setSoTimeout(httpParameters, Constants.timeoutSocket);

        DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

        return httpClient;
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    public static String getIP(WifiManager wifimanager){
        WifiInfo info;
        int ip=0;
        int port;

        try {
            info = wifimanager.getConnectionInfo();
            ip = info.getIpAddress();

        } catch (Exception e) {
            Log.d(Constants.LOGTAG, "Utils: getIP() :  Exception caught " + e.toString());
            ip =0;
            Threads.writeLog(Constants.debugLogFilename," Utils : getIp() " + e.toString());

        }

        return Formatter.formatIpAddress(ip);
    }


//used for getting mac for android 6 and above , which otherwise returns a constant string 02:00:00:00:00:00
    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            Log.d(Constants.LOGTAG,ex.toString());
        }
        return "02:00:00:00:00:00";
    }




    public static String getMACAddress(WifiManager wifimanager){
        WifiInfo info;
        String address;
        try {
            info = wifimanager.getConnectionInfo();
            address = info.getMacAddress();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(Constants.LOGTAG, "Utils: getMacAddress() :  Exception caught " + e.toString());
            address ="";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Call for marshmallow and above
            String macAddress = getMacAddr(); //version 6

            Log.d(Constants.LOGTAG, "Getting mac for version 6 and above : " + macAddress);

            return macAddress;
        } else {
            return address;
        }

    }

    public static String getWifiStrength(WifiManager wifimanager){
        WifiInfo info = wifimanager.getConnectionInfo();
        int level;

        try {
            level = WifiManager.calculateSignalLevel(info.getRssi(), 10);
        } catch (Exception e) {
            Log.d(Constants.LOGTAG, "Utils: getWifiStrength() : rssi : Exception caught " + e.toString());
            level = 0;
        }

        return Integer.toString(level);
    }

    public static String getAvailableStorage(){
        File path = Environment.getDataDirectory(); //internal storage
        StatFs sf = new StatFs(path.getPath());
        @SuppressWarnings("deprecation")
        int blocks = sf.getAvailableBlocks();
        @SuppressWarnings("deprecation")
        int blocksize = sf.getBlockSize();
        long availStorage = blocks * blocksize/(1024 * 1024); //Mega bytes
        return Long.toString(availStorage);
    }

    public static String getTotalRAM() {
        RandomAccessFile reader = null;
        String load = "0";
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();
            String[] tokens = load.split(" +");
            load = tokens[1].trim(); //here is the memory
            int ram = Integer.parseInt(load); //KB
            ram = ram/1024;
            load = Integer.toString(ram);
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return load;
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    public static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Default to return 1 core
            return 1;
        }
    }

    public static String getProcessorSpeed() {
        RandomAccessFile reader = null;
        String load = "0";
        try {
            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            load = reader.readLine();
            int speed = Integer.parseInt(load); //Khz
            speed = speed / 1000; //Mhz
            load = Integer.toString(speed);
            reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return load;
    }


    public static String getExpOverJson(WifiManager wifimanager)
    {
        JSONObject obj = new JSONObject();

        obj.put("action", "expOver");
        obj.put("exp", MainActivity.load.loadid);
        obj.put("ip", getIP(wifimanager));
        if(MainActivity.serverConnection == null) {
            obj.put("port", "");
        }
        else {

            obj.put("port", Integer.toString(MainActivity.serverConnection.getLocalPort()));
        }
        obj.put("macAddress" ,Utils.getMACAddress(wifimanager));
        String jsonString = obj.toJSONString();
        return  jsonString;

    }

    public static String getAppVersion(Context context){
        String version ="";
        int verCode =-1;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
            verCode = pInfo.versionCode;
            Log.d("Utils" , "Version Name : "+ version+ "  Version Number : "+ Integer.toString(verCode));
        }
        catch (Exception ex){
            Log.d("Utils" , "Version NUmber exception");

        }

        return version;
    }

    public static String getBSSID(WifiManager wifimanager){
        String bssid="";
        try {
            bssid = wifimanager.getConnectionInfo().getBSSID();
            bssid = bssid.replaceAll("\"","");
        } catch (Exception e) {
            Log.d(Constants.LOGTAG,"Utils: bssid : Exception caught "+e.toString() );
            bssid = "";
        }
        return bssid;

    }

    public static String getSSID(WifiManager wifimanager){
        String ssid="";
        try {
            ssid = wifimanager.getConnectionInfo().getSSID();
            ssid= ssid.replaceAll("\"", "");
        } catch (Exception e) {
            Log.d(Constants.LOGTAG,"Utils: ssid : Exception caught "+e.toString() );
            ssid = "";
        }
        return ssid;

    }

    public static int getLinkspeed(WifiManager wifimanager){
        int linkSpeed=0;
        try {
            linkSpeed = wifimanager.getConnectionInfo().getLinkSpeed();
        } catch (Exception e) {
            Log.d(Constants.LOGTAG,"Utils: linkspeed : Exception caught "+e.toString() );
            linkSpeed = 0;
        }
        return linkSpeed;

    }

    public static int getRSSI (WifiManager wifimanager){
        int rssi=0;
        try {
            rssi = wifimanager.getConnectionInfo().getRssi();
        } catch (Exception e) {
            Log.d(Constants.LOGTAG,"Utils: rssi : Exception caught "+e.toString() );
            rssi = 0;
        }
        return rssi;

    }


    public static JSONObject getMyDetailsJson(Context context)
    {
        WifiManager wifimanager = (WifiManager)context.getSystemService(context.WIFI_SERVICE);
        JSONObject obj = new JSONObject();


        obj.put("action", "heartBeat");
        obj.put("ip", getIP(wifimanager));
        obj.put("appversion", getAppVersion(context)) ;
        obj.put("androidVersion" , Integer.toString(Build.VERSION.SDK_INT));
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.appPref, context.MODE_PRIVATE);
        int is_in_foreground = sharedPref.getInt(context.getString(R.string.is_running_in_foreground), Constants.false_value);
        if(is_in_foreground==Constants.true_value)
            obj.put("isInForeground", String.valueOf(true));
        else
            obj.put("isInForeground", String.valueOf(false));
        obj.put("devicename", getDeviceName()) ;

        if(MainActivity.serverConnection == null) {
            obj.put("port", "");
        }
        else {

            obj.put("port", Integer.toString(MainActivity.serverConnection.getLocalPort()));
        }
        obj.put("macAddress", Utils.getMACAddress(wifimanager));

        obj.put(Constants.rssi,Integer.toString(getRSSI(wifimanager)));
        obj.put(Constants.bssid, getBSSID(wifimanager));
        obj.put(Constants.ssid, getSSID(wifimanager));
        obj.put(Constants.linkSpeed, Integer.toString(getLinkspeed(wifimanager)));

        obj.put(Constants.processorSpeed, getProcessorSpeed());
        obj.put(Constants.numberOfCores, Integer.toString(getNumCores()));
       // obj.put(Constants.wifiSignalStrength, getWifiStrength());

        obj.put(Constants.storageSpace, getAvailableStorage());
        obj.put(Constants.memory, getTotalRAM());

        return  obj;

    }


    static Calendar getServerCalendarInstance(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, (int) MainActivity.serverTimeDelta);
        return cal;
    }



    //parse the json string sent by server in form of map of key value pairs
    @SuppressWarnings("unchecked")
    static Map<String, String> ParseJson(String json){
        Map<String, String> jsonMap = null;
        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory(){
            @SuppressWarnings("rawtypes")
            public List creatArrayContainer() {
                return new LinkedList();
            }
            @SuppressWarnings("rawtypes")
            public Map createObjectContainer() {
                return new LinkedHashMap();
            }

        };

        try {
            jsonMap = (Map<String, String>) parser.parse(json, containerFactory);
        } catch (ParseException e) {
            System.out.println();
            e.printStackTrace();
        }
        return jsonMap;
    }

    //Establishes a connection to server
    public static class NewConnection extends AsyncTask< Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            MainActivity.heartbeat_enabled = true ;
            if(MainActivity.serverConnection != null){
                try{
                    MainActivity.serverConnection.close();
                    MainActivity.serverConnection = null;
                    MainActivity.dis = null;
                    MainActivity.dout = null;
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

                    try {
                    MainActivity.serverConnection = new Socket(Constants.server, Constants.serverport);
                    Log.d(Constants.LOGTAG, "Utils : creating new socket...");

                    try {
                        MainActivity.dis = new DataInputStream( MainActivity.serverConnection.getInputStream());
                        MainActivity.dout = new DataOutputStream( MainActivity.serverConnection.getOutputStream());
                       // MainActivity.ConnectionCondition.open();
                        Log.d(Constants.LOGTAG ,"Utils : setting data streams...");
                    }
                    catch (Exception e) {

                        Log.d( Constants.LOGTAG , "Utils : Exceptionwhile initalizing datastream " + e.toString());

                       // e.printStackTrace();
                    }

                    } catch (Exception e) {
                    Log.d(Constants.LOGTAG , "Utils: Exceptionwhile creating a new connection" + e.toString());
                    //e.printStackTrace();
                        }

            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d("Utils New COnnection :", "I am in post Exceute");
            super.onPostExecute(aVoid);

        }
    }



}
