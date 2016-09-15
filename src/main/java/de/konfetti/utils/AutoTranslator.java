package de.konfetti.utils;

import com.google.api.GoogleAPI;
import com.google.api.translate.Language;
import com.google.api.translate.Translate;
import de.konfetti.data.mediaitem.LangData;
import de.konfetti.data.mediaitem.MultiLang;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;


@Service
@Configuration
@Slf4j
public class AutoTranslator {

	/*
	 * LANGUAGE CODES
	 */
	// TODO make enums
	public final String LANGCODE_GERMAN = "de";
	public final String LANGCODE_ENGLISH = "en";
	public final String LANGCODE_ARABIC = "ar";
	public final String[] SUPPORTED_LANGCODES = {LANGCODE_GERMAN, LANGCODE_ENGLISH, LANGCODE_ARABIC};

	@Value("${googletranslate.apikey}")
	private final String googleTranslatorApiKey = null;

	@PostConstruct
	public void init() {
		if (googleTranslatorApiKey==null) throw new RuntimeException("please set 'googletranslate.apikey' in 'application.properties'");
		log.info("Got API-KEY: " + googleTranslatorApiKey);

		// TODO: get API key from config not stored on GitHub
		GoogleAPI.setHttpReferrer("http://www.konfettiapp.de/api");
		GoogleAPI.setKey(googleTranslatorApiKey);
	}
	
	public MultiLang translate(String langCode, String text) throws Exception {
		
		// check if language is supported
		if (!isLanguageSupported(langCode)) throw new Exception("language("+langCode+") is not supported by AutoTranslator");
		
		// prepare basic result object
		MultiLang result = new MultiLang();
		LangData org = new LangData();
		org.text = text;
		org.translator = LangData.ORIGIN_ORGINAL;
		result.put(langCode, org);
		
		// go thru all supported languages
		for (String toLangCode : SUPPORTED_LANGCODES) {
			
			if (toLangCode.equals(langCode)) continue;
			
			try {
				
				Language fromLang = getGoogleLanguageFromLangCode(langCode);
				Language toLang = getGoogleLanguageFromLangCode(toLangCode);
				String translatedText = Translate.DEFAULT.execute(text, fromLang, toLang);
				log.info("Google Translate " + langCode + "->" + toLangCode + " text(" + text + ") --> (" + translatedText + ")");
				
				LangData langData = new LangData();
				langData.text = translatedText;
				langData.translator = LangData.ORIGIN_GOOGLE;
				result.put(toLangCode, langData);
				
			} catch (Exception e) {
				log.warn("FAIL translate: " + e.getMessage());
			}
			
		}
				
		return result;
	}
	
	public MultiLang reTranslate(MultiLang multiLang) throws Exception {
		// find orginal
		String orgLangCode = null;
		String orgText = null;
		Set<String> keys = multiLang.keySet();
		for (String key : keys) {
			LangData langData = multiLang.get(key);
			if (langData.translator==LangData.ORIGIN_ORGINAL) {
				orgLangCode = key;
				orgText = langData.text;
			}
		}
		// exception if no original was found
		if ((orgLangCode==null) || (orgText==null)) throw new Exception("no translator==0 found");
		
		return translate(orgLangCode, orgText);
	}
	
	public boolean isLanguageSupported(String langCode) {
		boolean isSupported = false;
		for (int i=0; i<SUPPORTED_LANGCODES.length; i++) {
			if (SUPPORTED_LANGCODES[i].equals(langCode)) {
				isSupported = true;
				break;
			}
		}
		return isSupported;
	}
	
	private Language getGoogleLanguageFromLangCode(String langCode) throws Exception {
		if (langCode.equals(LANGCODE_ENGLISH)) return Language.ENGLISH;
		if (langCode.equals(LANGCODE_GERMAN)) return Language.GERMAN;			
		if (langCode.equals(LANGCODE_ARABIC)) return Language.ARABIC;	
		throw new Exception("TODO: map own langCode("+langCode+") to GOOGLE LANGUAGE object");
	}
	
}
