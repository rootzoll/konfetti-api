package de.konfetti.utils;

import de.konfetti.controller.vm.ResponseZip2Gps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Scanner;

/**
 * Created by relampago on 10.10.16.
 */
public class GpsConverter {

    private static final Logger logger = LoggerFactory.getLogger(GpsConverter.class);

    public ResponseZip2Gps fromZipCode(String country, String code){
        ResponseZip2Gps result = new ResponseZip2Gps();
        result.setResultCode(-1);
        try {
            logger.debug("ZIP2GPS country("+country+") zip("+code+") -->");
            Scanner scanner = new Scanner(new URL("https://maps.googleapis.com/maps/api/geocode/json?address="+code+","+country).openStream(), "UTF-8");
            String json = scanner.useDelimiter("\\A").next();
            scanner.close();
            int i = json.indexOf("\"location\" : {");
            int e;
            if (i>0) {
                i+=14;
                json = json.substring(i);
                i = json.indexOf("\"lat\" : ");
                if (i>0) {
                    i+=8;
                    e = json.indexOf(",", i);
                    String latStr = json.substring(i,e).trim();
                    System.out.println("LAT("+latStr+")");
                    result.setLat(Double.parseDouble(latStr));
                }
                i = json.indexOf("\"lng\" : ");
                if (i>0) {
                    i+=8;
                    e = json.indexOf("}", i);
                    String lngStr = json.substring(i,e).trim();
                    System.out.println("LNG("+lngStr+")");
                    result.setLon(Double.parseDouble(lngStr));
                }
                result.setResultCode(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
