package de.konfetti.controller;

import de.konfetti.Application;
import de.konfetti.controller.vm.KeyAndPasswordVM;
import de.konfetti.data.User;
import de.konfetti.data.UserRepository;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.subethamail.wiser.Wiser;

import java.time.ZonedDateTime;
import java.util.Locale;

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
    private String emailFrom;

    @Value("${konfetti.replyToMailAddress}")
    private String replyToAddress;

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    protected UserRepository userRepository;

    private Wiser wiser;

    @Value("${local.server.port}")
    int serverPort;

    private int emailPort;

    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        testHelper = new TestHelper();
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
        createUser(testEmail, testPassword);

        // check email was sent to the user
//        MessageSourceResourceBundle.getBundle("messages", locale).getString(subjectKey);
        assertReceivedMessage(wiser)
                .from(emailFrom)
                .to(testEmail)
//                .withSubject(Message)
        ;
    }

    private void createUser(String email, String password) {
        myGiven()
                .param("mail", email).param("pass", password)
                .when().post("konfetti/api/account")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("email", equalToIgnoringCase(email))
                .body("password", isEmptyOrNullString())
        ;
    }

    @Test
    public void testRquestPasswordReset() throws Exception {
        User user = testHelper.getUser("testRquestPasswordReset");
        createUser(user.getEMail(), user.getPassword());
        myGiven()
                .contentType(ContentType.TEXT)
                .body(user.getEMail())
                .post(UserController.REST_API_MAPPING + "/reset_password/init")
                .then().statusCode(200);

        assertReceivedMessage(wiser)
                .from(emailFrom)
                .to(user.getEMail())
                .withSubject(messageSource.getMessage("email.reset.title", null, Locale.ENGLISH));
    }

    @Test
    public void testRquestPasswordFinish() throws Exception {
        User user = testHelper.getUser("testRquestPasswordFinish");
        String resetKey = "123456";
        user.setResetKey(resetKey);
        user.setResetDate(ZonedDateTime.now());
        User persistedUser = userRepository.save(user);
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM(resetKey, "newPassword");

        myGiven()
                .contentType(ContentType.JSON)
                .body(keyAndPasswordVM)
                .post(UserController.REST_API_MAPPING + "/reset_password/finish")
                .then().statusCode(200);
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