package de.konfetti.utils;

import de.konfetti.controller.vm.ResponseZip2Gps;

import java.net.URL;
import java.util.Scanner;

/**
 * Created by relampago on 10.10.16.
 */
public class GpsConverter {

    public ResponseZip2Gps fromZipCode(String country, String code){
        ResponseZip2Gps result = new ResponseZip2Gps();
        result.resultCode = -1;
        try {
            System.out.println("ZIP2GPS country("+country+") zip("+code+") -->");
            Scanner scanner = new Scanner(new URL("https://maps.googleapis.com/maps/api/geocode/json?address="+code+","+country).openStream(), "UTF-8");
            String json = scanner.useDelimiter("\\A").next();
            scanner.close();
            int i = json.indexOf("\"location\" : {");
            int e = 0;
            if (i>0) {
                i+=14;
                json = json.substring(i);
                i = json.indexOf("\"lat\" : ");
                if (i>0) {
                    i+=8;
                    e = json.indexOf(",", i);
                    String latStr = json.substring(i,e).trim();
                    System.out.println("LAT("+latStr+")");
                    result.lat = Double.parseDouble(latStr);
                }
                i = json.indexOf("\"lng\" : ");
                if (i>0) {
                    i+=8;
                    e = json.indexOf("}", i);
                    String lngStr = json.substring(i,e).trim();
                    System.out.println("LNG("+lngStr+")");
                    result.lon = Double.parseDouble(lngStr);
                }
                result.resultCode = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
