package de.konfetti.utils;

import de.konfetti.service.BaseTest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.subethamail.wiser.Wiser;

import java.util.Locale;
import java.util.ResourceBundle;

import static de.konfetti.utils.WiserAssertions.assertReceivedMessage;

/**
 * Created by relampago on 20.06.16.
 */
public class EMailManagerTest extends BaseTest {

    @Autowired
    private EMailManager eMailManager;

    private Wiser wiser;

    private int emailPort;

    private String email ="myEmail@test.de";
    private String password = "myPassword";

    private String toEmail = "testEmail@test.de";
    private String subjectKeyAccountCreated;


    @Before
    public void setUp() throws Exception {
        emailPort = 2500;
        wiser = new Wiser(emailPort);
        wiser.start();
    }

    @After
    public void tearDown() throws Exception {
        wiser.stop();
    }

    @Test
    public void sendMail() throws Exception {
        String bodyText = "username: " + email + "\npass: " + password + "\n\nkeep email or write password down";

        boolean success = eMailManager.sendMail(toEmail, "test", bodyText, null);
        Assert.assertTrue("Email send successfully", success);
        assertReceivedMessage(wiser)
                .from(eMailManager.getFromEmailAddress())
                .to(toEmail)
                .withSubject("test");
    }

}