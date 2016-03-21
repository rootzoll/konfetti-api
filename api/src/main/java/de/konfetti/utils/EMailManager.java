package de.konfetti.utils;

import java.net.MalformedURLException;
import java.net.URL;

import javax.activation.URLDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


/*
 * Use to send eMails.
 * 
 * To get Spring JavaMailSender user AutoWire in Component classes:
 * @Autowired
 * private JavaMailSender javaMailSender;
 * 
 * --> see application.properties file for configuration
 */
public class EMailManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EMailManager.class);
		
    private static EMailManager singletonInstance = null;
    
    // private constructor - Singleton
    private EMailManager() {}
    
    // Singleton Getter
    public static EMailManager getInstance() {
    	if (singletonInstance==null) singletonInstance = new EMailManager();
    	return singletonInstance;
    }
    
    /**
     * Sending an eMail with and optional attachment.
     * @param toAddress
     * @param subjectText
     * @param bodyText
     * @param urlAttachment HTTP URL string to attachment file - if NULL = no attachment
     * @return
     */
	public boolean sendMail(JavaMailSender javaMailSender, String toAddress, String subjectText, String bodyText, String urlAttachment) {
    	
		String fromAddress = Helper.getPropValues("konfetti.sendFromMailAddress");
		String replyAddress = Helper.getPropValues("konfetti.replyToMailAddress");
		if ((fromAddress==null) || (fromAddress.trim().length()==0) || fromAddress.trim().equals("test@test.de")) {
			LOGGER.warn("eMail not configured in application.properties - skipping sending to "+toAddress);
			return false;
		}
		if (replyAddress==null) replyAddress = fromAddress;
		fromAddress = fromAddress.trim();
		replyAddress = replyAddress.trim();
		toAddress = toAddress.trim();
		
		MimeMessage mail = javaMailSender.createMimeMessage();
        try {
        	LOGGER.info("EMailManager - sending eMail to("+toAddress+") ...");
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setTo(toAddress);
            helper.setReplyTo(replyAddress);
            helper.setFrom(fromAddress);
            helper.setSubject(subjectText);
            helper.setText(bodyText);
    		if (urlAttachment!=null) helper.addAttachment("KonfettiCoupons.pdf", new URLDataSource(new URL(urlAttachment)));
            javaMailSender.send(mail);
        	LOGGER.info("EMailManager - OK sending eMail to("+toAddress+")");
            return true;
        } catch (MessagingException e) {
        	LOGGER.warn("EMailManager - FAIL sending eMail to("+toAddress+"): "+e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
        	LOGGER.warn("EMailManager - FAIL sending eMail to("+toAddress+") attachementURL("+urlAttachment+"): "+e.getMessage());
        	e.printStackTrace();
        } catch (Exception e) {
        	LOGGER.warn("EMailManager - FAIL sending eMail to("+toAddress+") attachementURL("+urlAttachment+") mailserver("+Helper.getPropValues("spring.mail.host")+":"+Helper.getPropValues("spring.mail.port")+"): "+e.getMessage());
        	e.printStackTrace();
        }
        return false;
	}
	
}
