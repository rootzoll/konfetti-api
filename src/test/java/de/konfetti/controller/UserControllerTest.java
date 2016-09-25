package de.konfetti.controller;

import de.konfetti.Application;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.subethamail.wiser.Wiser;

import static de.konfetti.utils.WiserAssertions.assertReceivedMessage;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

/**
 * Created by relampago on 25.09.16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerTest {

    @Value("${konfetti.sendFromMailAddress}")
    private String fromEmailAddress;

    @Value("${konfetti.replyToMailAddress}")
    private String replyToAddress;

    private Wiser wiser;

    @Value("${local.server.port}")
    int serverPort;

    private int emailPort;

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
    public void createUser() throws Exception {
        String testEmail = "testCreateUser@test.de";
        String testPassword = "createUser";
        // check user is created
        myGiven()
                .param("mail", testEmail).param("pass", testPassword)
                .when().post("konfetti/api/account")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("email", equalToIgnoringCase(testEmail))
                .body("password", isEmptyOrNullString())
        ;

        // check email was sent to the user
//        MessageSourceResourceBundle.getBundle("messages", locale).getString(subjectKey);
        assertReceivedMessage(wiser)
                .from(fromEmailAddress)
                .to(testEmail)
//                .withSubject(Message)
        ;
    }

    @Test
    public void login() throws Exception {

    }

    @Test
    public void recover() throws Exception {

    }

    @Test
    public void updateUser() throws Exception {

    }

    protected RequestSpecification myGiven(){
        return given().port(serverPort).log().ifValidationFails(LogDetail.ALL);
    }

}