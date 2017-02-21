package de.konfetti.utils;

import de.konfetti.service.BaseTest;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.subethamail.wiser.Wiser;

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
    
    @SuppressWarnings("unused")
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

        /*
        boolean success = eMailManager.sendMail(toEmail, "test", bodyText, null);
        Assert.assertTrue("Email send successfully", success);
        assertReceivedMessage(wiser)
                .from(eMailManager.getFromEmailAddress())
                .to(toEmail)
                .withSubject(UTF8ToAscii.unicodeEscape(EMailManager.EMAIL_SUBJECT_TAG+" test"));
        */
    }

}