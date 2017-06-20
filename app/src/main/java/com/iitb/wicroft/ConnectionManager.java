package com.iitb.wicroft;

import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by swinky on 9/7/16.
 */
public class ConnectionManager {

    private static ConnectionManager sync_obj = new ConnectionManager();

   public static String readFromStream() {

        String data  = "";
       String msg =" ConnectionManager Read :";
        try {
            int length = MainActivity.dis.readInt();
            msg += " Reading "+length+ " Bytes from the socket";
            for (int i = 0; i < length; ++i) {
                data += (char) MainActivity.dis.readByte();

            }
        }catch(IOException ex){
            data = null;
            if(!MainActivity.heartbeat_enabled)
                MainActivity.heartbeat_enabled = true; // some socket error is there. enable heartbeat so that it can create a new connection
            Log.d(Constants.LOGTAG, ex.toString());

        }catch(Exception ex){
            data = null;
            if(!MainActivity.heartbeat_enabled)
                MainActivity.heartbeat_enabled = true;
            Log.d(Constants.LOGTAG,ex.toString());
        }

       msg +="The received data is :"+data ;
       Log.d(Constants.LOGTAG, msg);
       Threads.writeLog(Constants.debugLogFilename,msg);
       return data;
    }


    public static int writeToStream(String  data, boolean urgent) {
        //return dout;
        int response = -1;
        int tries=0;
        String msg =" ConnectionManager Write:";
        while (response==-1 && tries<=3 &&urgent || tries==0) {

            try {
                synchronized (sync_obj) {
                    msg += " Length : " + data.length();
                    Log.d(Constants.LOGTAG, "Socket write----length : " + data.length() + "Data :" + data);
                    MainActivity.dout.writeInt(data.length());
                    MainActivity.dout.writeBytes(data);
                    MainActivity.dout.flush();
                    response = 200;
                }

            } catch (IOException ex) {
                response = -1;

                if(urgent)
                    new Utils.NewConnection().execute();
                ex.printStackTrace();
                msg += " Caught an exception while writing to Socket" + ex.toString();
            } catch (Exception ex) {
                response = -1;
                if(urgent)
                    new Utils.NewConnection().execute();
                msg += " Caught an exception 2 while writing to Socket" + ex.toString();
            }

            tries+=1;
            if(response==200 || urgent==false)
                break;
            else {

                try {

                    Thread.sleep(10000);
                    Log.d(Constants.LOGTAG, "sleep in WriteToSocket(): Will retry after 10s");
                }catch (Exception e){
                    Log.d(Constants.LOGTAG, "Caught Exception in sleep in WriteToSocket()");
                }
            }
        }
        msg+="\n response Code : "+response;
        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename, msg);
        return response;
    }

}
