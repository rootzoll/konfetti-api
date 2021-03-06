package de.konfetti.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * Use to send push notifications to apps.
 */
@Service
@Configuration
@Slf4j
public class PushManager {

	public static final String PLATFORM_ONESIGNAL = "ONESIGNAL";

	@Value("${konfetti.pushID}")
	private String pushId = "";

	@Value("${konfetti.pushAuth}")
	private String pushAuth = "";
		
	public boolean isAvaliable() {
		log.info("PushManager: isAvaliable pushId("+this.pushId+") pushAuth("+this.pushAuth+")");
		if (this.pushId==null) return false;
		if (this.pushId.trim().length()==0) return false;
		if (this.pushAuth==null) return false;
		if (this.pushAuth.trim().length()==0) return false;
		return true;
	}
	
	public boolean sendNotification(String platformUSEFINALS, String userPushID, String textShort, String locale, String textDefaultEn, String meta) {

		if (!isAvaliable()) {
			log.warn("PushManager not configured - not possible");
			return false;
		}

		try {
			// PREPARE JSON DATA

			String defaultEnglish = "";
			if (!"en".equals(locale)) defaultEnglish = ", \"en\": \""+textDefaultEn+"\"";
			String json = "{\"app_id\": \""+this.pushId +"\",\"include_player_ids\":[\""+userPushID+"\"],\"data\": "+meta+",\"contents\": {\""+locale+"\": \""+textShort+"\""+defaultEnglish+"}}";

			// HTTP REQUEST --> ONESIGNAL REST API
			URL url = new URL("https://onesignal.com/api/v1/notifications");
			  HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			  httpCon.setDoInput(true);
			  httpCon.setDoOutput(true);
			  httpCon.setRequestProperty("Content-Type", "application/json");
			  httpCon.setRequestProperty("Authorization", "Basic "+this.pushAuth);
			  httpCon.setRequestMethod("POST");
			  OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
			  out.append(json);
			  out.flush();
			  int resultCode = httpCon.getResponseCode();
			  String resultMessage = httpCon.getResponseMessage();
			  out.close();

			if (resultCode!=200) {
				log.warn("FAIL HTTP REQUEST POST https://onesignal.com/api/v1/notifications");
				log.warn(json);
				log.warn("(" + resultCode + ") => '" + resultMessage + "'");
				try {
					 // read the output from the server
					  BufferedReader reader = new BufferedReader(new InputStreamReader(httpCon.getErrorStream()));
					  StringBuilder stringBuilder = new StringBuilder();
				      String line = null;
				      while ((line = reader.readLine()) != null) {stringBuilder.append(line + "\n");}
					  log.warn("ERROR BODY --> "+stringBuilder.toString());
				} catch (Exception e) {
					  log.warn("ERROR BODY EXCEPTION --> "+e.getMessage());
				}
				return false;
			  } else {
				log.info("OK PushNotification ("+this.pushId+") -> https://onesignal.com/api/v1/notifications");
			  }

			return true;

		} catch (Exception e) {
			log.warn("FAIL on sending push message", e);
			e.printStackTrace();
			return false;
		}
	}

}
