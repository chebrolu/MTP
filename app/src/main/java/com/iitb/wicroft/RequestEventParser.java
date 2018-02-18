package com.iitb.wicroft;

/**
 * Created by swinky on 28/6/16.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import android.util.Log;

enum RequestType{
    GET,
    POST,
    DASH,
    MP4,
    NONE
};

enum DownloadMode{
    SOCKET,
    WEBVIEW,
    EXO,
    NONE
};


//Event class. stores time(as Calendar object), url, request type
class RequestEvent{
    int event_id;
    Calendar cal;
    String url;
    RequestType type;
    DownloadMode mode;
    String postDataSize;
    int url_dependency ; //if 0 no dependency schedule on given time ; else if = x, schedule it only when x completes
    int relative_time =0;

    RequestEvent(int id ,Calendar tcal, String turl, RequestType ttype, DownloadMode tmode,int url_dep){
        event_id = id;
        cal = tcal;
        url = turl;
        type = ttype;
        mode = tmode;
        url_dependency = url_dep;
    }

    RequestEvent(RequestEvent e)
    {
        event_id = e.event_id;
        cal = e.cal;
        url = e.url;
        type = e.type;
        mode = e.mode;
        postDataSize = e.postDataSize;
        url_dependency = e.url_dependency;
        relative_time = e.relative_time;
    }

    RequestEvent(int id,Calendar tcal, String turl, RequestType ttype, DownloadMode tmode, String size , int url_dep , int relative_t){
        event_id = id;
        cal = tcal;
        url = turl;
        type = ttype;
        mode = tmode;
        postDataSize = size;
        url_dependency = url_dep;
        relative_time = relative_t;
    }


}

//Load contains all information about an experiment i.e event id and list of all events
class Load{
    String loadid;
    Vector<RequestEvent> events;
    Vector<RequestEvent> independent_events;
    Map< Integer , Vector<RequestEvent>> url_dependency_graph;
    int total_events;

    int request_completed ;
    Load(String tloadid,int tcount, Vector<RequestEvent> all_events, Vector<RequestEvent> tevents , Map< Integer , Vector<RequestEvent>> dep_graph){
        loadid = tloadid;
        events = all_events;
        total_events = tcount;
        independent_events = tevents;
        url_dependency_graph = dep_graph;
        request_completed =0;
    }
}

//Parser class which parses lines from control file and creates event objects
public class RequestEventParser {

    private static final Map<String, RequestType> typeMap;
    private static final Map<String, DownloadMode> modeMap;
    static
    {   typeMap = new HashMap<String, RequestType>();
        typeMap.put("GET", RequestType.GET);
        typeMap.put("POST", RequestType.POST);
        typeMap.put("DASH", RequestType.DASH);
        typeMap.put("MP4", RequestType.MP4);

        modeMap = new HashMap<String, DownloadMode>();
        modeMap.put("SOCKET", DownloadMode.SOCKET);
        modeMap.put("WEBVIEW", DownloadMode.WEBVIEW);
        modeMap.put("EXO",DownloadMode.EXO);
    }

    public static RequestType getRequestEnum(String key){
        RequestType type = typeMap.get(key);
        if(type == null){
            return RequestType.NONE;
        }
        return type;
    }

    public static DownloadMode getDownloadModeEnum(String key){
        DownloadMode type = modeMap.get(key);
        if(type == null){
            return DownloadMode.NONE;
        }
        return type;
    }

    //Just for testing parseLine
    public static String generateLine(int seconds){
        String line = "GET ";

        Calendar cal = Utils.getServerCalendarInstance();
        cal.add(Calendar.SECOND, seconds);
        line += cal.get(Calendar.YEAR) + " ";
        line += cal.get(Calendar.MONTH) + " ";
        line += cal.get(Calendar.DAY_OF_MONTH) + " ";
        line += cal.get(Calendar.HOUR_OF_DAY) + " ";
        line += cal.get(Calendar.MINUTE) + " ";
        line += cal.get(Calendar.SECOND) + " ";
        line += cal.get(Calendar.MILLISECOND) + " ";

        line += "http://www.cse.iitb.ac.in/~ashishsonone/serve.php?user=ashish@" + seconds;
        return line;
    }

    public static RequestEvent parseLine(String line){

        Log.d(" RequestEventParser" , " in parse line :"+line);
        String[] fields = line.split(" ");
        Log.d("RequestEventParser ", fields[0] + fields.length);

        if(fields.length < 4) {
            Log.d("RequestEventParser:", "No of fields less than expected");
            return null; //No valid event could be found
        }
        // atleast there are 5 fields(DOWNLOADMODE CAN BE "SOCKET/WEBVIEW"
        // TYPE relative_time DOWNLOADMODE URL postsize url_dependency
        // 0     1              2           3    4          5
        RequestType type = getRequestEnum(fields[0]);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( Calendar.getInstance().getTimeInMillis() + (Integer.parseInt(fields[1]) *1000 )) ;
        DownloadMode mode = getDownloadModeEnum(fields[2]);
        String url = fields[3];
        int url_dep = Integer.parseInt(fields[fields.length-1]);
        int relative_t =0;
        if(url_dep !=0){
            relative_t = Integer.parseInt(fields[1]);
        }

        if(fields.length == 5){
            return new RequestEvent(0,cal, url, type, mode,"0",url_dep,relative_t);
        } else if (fields.length == 6){
            return new RequestEvent(0,cal, url, type, mode, fields[4],url_dep,relative_t);
        }
        return null;
    }

    public static Load parseEvents(String s , String exptno){
        String msg =" RequestEventParser : Entered Here. ";
        Vector<RequestEvent> independent_events = new Vector<RequestEvent>();
        Vector<RequestEvent> all_events = new Vector<RequestEvent>();
        Map< Integer , Vector<RequestEvent>> dep_graph = new HashMap<>() ;
        Scanner scanner = new Scanner(s);
        String line ;
        int count =0; // total number of events
        String id = exptno ;
        while (scanner.hasNextLine()) {
            count++;
            line = scanner.nextLine();

            RequestEvent event = parseLine(line);
            if(event != null) {
                event.event_id = count;
                all_events.add(event);
                if(event.url_dependency ==0)
                independent_events.add(event);
                else{
                    if (dep_graph.containsKey(event.url_dependency))
                        dep_graph.get(event.url_dependency).add(event);
                    else {
                        Vector<RequestEvent> l = new Vector<RequestEvent>();
                        l.add(event);
                        dep_graph.put(event.url_dependency, l);
                    }
                }

            }

        }
        scanner.close();
        msg+=" Return Event: "+ independent_events.size() +"";

        Log.d(Constants.LOGTAG, msg);
        Threads.writeLog(Constants.debugLogFilename ,msg);

        return new Load(id,count,all_events, independent_events ,dep_graph );
    }
}
