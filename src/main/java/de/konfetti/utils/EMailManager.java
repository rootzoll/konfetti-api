package de.konfetti.utils;

import de.konfetti.data.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.activation.URLDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

/*
 * Use to send eMails.
 *
 * To get Spring JavaMailSender user AutoWire in Component classes:
 * @Autowired
 * private JavaMailSender javaMailSender;
 *
 * --> see application.properties file for configuration
 */
@Slf4j
@Service
@Configuration
public class EMailManager {

	public static final String EMAIL_SUBJECT_TAG = "[Konfetti]";
	
    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${konfetti.sendFromMailAddress}")
    private String fromEmailAddress;

    @Value("${konfetti.replyToMailAddress}")
    private String replyToAddress;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private MessageSource messageSource;

    @Value("${konfetti.server.url}")
    private String serverUrl;

    /**
     * Sending an eMail with and optional attachment.
     *
     * @param toAddress
     * @param subjectKey
     * @param bodyText
     * @param urlAttachment HTTP URL string to attachment file - if NULL = no attachment
     * @return
     */
    public boolean sendMail(String toAddress, String subjectText, String bodyText, String urlAttachment) {
        fromEmailAddress = fromEmailAddress.trim();
        replyToAddress = replyToAddress.trim();
        if (replyToAddress == null) replyToAddress = fromEmailAddress;
 
        if ((toAddress == null) || (toAddress.length() <= 3)) {
            log.warn("failed sending email because toAdrress(" + toAddress + ") is unvalid");
            return false;
        }

        if ((fromEmailAddress == null) || (fromEmailAddress.length() == 0) || fromEmailAddress.equals("test@test.de")) {
            log.warn("eMail not configured in application.properties - skipping sending to " + toAddress);
            return false;
        }

        toAddress = toAddress.trim();

        MimeMessage mail = javaMailSender.createMimeMessage();
        try {
            log.info("EMailManager - sending eMail to(" + toAddress + ") ...");
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, CharEncoding.UTF_8);
            helper.setTo(toAddress);
            helper.setReplyTo(replyToAddress);
            helper.setFrom(fromEmailAddress);
            helper.setText(bodyText);
            //helper.setSubject(UTF8ToAscii.unicodeEscape(EMAIL_SUBJECT_TAG + " " + subjectText));
            mail.setSubject(UTF8ToAscii.unicodeEscape(EMAIL_SUBJECT_TAG + " " + subjectText), CharEncoding.US_ASCII);
            if (urlAttachment != null)
                helper.addAttachment("KonfettiCoupons.pdf", new URLDataSource(new URL(urlAttachment)));
            javaMailSender.send(mail);
            log.info("EMailManager - OK sending eMail to(" + toAddress + ")");
            return true;
        } catch (MessagingException e) {
            log.warn("EMailManager - FAIL sending eMail to(" + toAddress + "): " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            log.warn("EMailManager - FAIL sending eMail to(" + toAddress + ") attachementURL(" + urlAttachment + "): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.warn("EMailManager - FAIL sending eMail to(" + toAddress + ") attachementURL(" + urlAttachment + ") mailserver(" + mailHost + ":" + mailPort + "): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public String getFromEmailAddress() {
        return fromEmailAddress;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug("Send e-mail[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
                isMultipart, isHtml, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, CharEncoding.UTF_8);
            message.setTo(to);
            message.setReplyTo(replyToAddress);
            message.setFrom(fromEmailAddress);
            //message.setSubject(UTF8ToAscii.unicodeEscape(EMAIL_SUBJECT_TAG + " " + subject));
            mimeMessage.setSubject(UTF8ToAscii.unicodeEscape(EMAIL_SUBJECT_TAG + " " + subject), CharEncoding.US_ASCII);
            message.setText(content);
            javaMailSender.send(mimeMessage);
            log.debug("Sent e-mail to User '{}'", to);
        } catch (Exception e) {
            log.warn("E-mail could not be sent to user '{}', exception is: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset e-mail to '{}'", user.getEMail());
        Locale locale = Locale.forLanguageTag(user.decideWichLanguageForUser());
        Context context = new Context(locale);
        if (user.getName() == null) {
            user.setName("Konfetti User");
        }
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, serverUrl);
        String content = templateEngine.process("passwordResetEmail", context);
        String subject = messageSource.getMessage("email.reset.title", null, locale);
        sendEmail(user.getEMail(), subject, content, false, true);
    }
}
