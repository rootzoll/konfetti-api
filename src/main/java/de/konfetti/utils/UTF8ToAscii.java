package de.konfetti.utils;

public class UTF8ToAscii {

    public static String unicodeEscape(String s) {
    	
    	return "=?UTF-8?B?"+org.springframework.util.Base64Utils.encodeToString(s.getBytes())+"?=";
    	
    }
    
    public static String escapeDeutscheUmlaute(String s) {
    	s = s.replace("ä", "ae");
       	s = s.replace("ö", "oe");
       	s = s.replace("ü", "ue");
    	s = s.replace("Ä", "Ae");
       	s = s.replace("Ö", "Oe");
       	s = s.replace("Ü", "Ue");
       	s = s.replace("ß", "ss");
    	return s;
    }
}