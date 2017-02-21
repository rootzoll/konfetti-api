package de.konfetti.utils;

public class UTF8ToAscii {

    public static String unicodeEscape(String s) {
    	
    return "=?UTF-8?B?"+org.springframework.util.Base64Utils.encodeToString(s.getBytes())+"?=";
    	
    }
}