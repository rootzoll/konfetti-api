package de.konfetti.controller;

import de.konfetti.Application;
import de.konfetti.controller.vm.KeyAndPasswordVM;
import de.konfetti.data.Party;
import de.konfetti.data.User;
import de.konfetti.data.UserRepository;
import de.konfetti.service.PartyService;
import io.restassured.http.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Locale;

import static de.konfetti.utils.WiserAssertions.assertReceivedMessage;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

/**
 * Created by relampago on 25.09.16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerTest extends BaseControllerTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PartyService partyService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void createUser() throws Exception {
        String testEmail = "testcreateuser@test.de";
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
                .param("mail", email.toLowerCase()).param("pass", password)
                .when().post("konfetti/api/account")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("email", equalToIgnoringCase(email))
                .body("password", isEmptyOrNullString())
        ;
    }

    @Test
    public void testRequestPasswordReset() throws Exception {
        User user = testHelper.getUser("testRequestPasswordReset");
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
        User user = testHelper.getUser("testLogin");
        createUser(user.getEMail(), user.getPassword());
        myGiven()
                .param("mail", user.getEMail())
                .param("pass", user.getPassword())
                .get(UserController.REST_API_MAPPING + "/login")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void loginWrongPassword() throws Exception {
        User user = testHelper.getUser("testLoginWrongPassword");
        createUser(user.getEMail(), user.getPassword());
        myGiven()
                .param("mail", user.getEMail())
                .param("pass", "wrongPassword")
                .get(UserController.REST_API_MAPPING + "/login")
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void loginEmailNotExisting() throws Exception {
        User user = testHelper.getUser("testLoginEmailNotExisting");
        createUser(user.getEMail(), user.getPassword());
        myGiven()
                .param("mail", "wrongEmail@test.de")
                .param("pass", user.getPassword())
                .get(UserController.REST_API_MAPPING + "/login")
                .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void testGenerateCodesAdmin() throws Exception {
        Party partyCreated = partyService.create(testHelper.getParty("partyTestGenerateCodesAdmin"));
        myGiven()
                .header(ControllerSecurityHelper.HEADER_ADMIN_PASSWORD, "admin")
                .pathParam("partyId", partyCreated.getId())
                .get(UserController.REST_API_MAPPING + "/codes-admin/{partyId}")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void recover() throws Exception {

    }

    @Test
    public void updateUser() throws Exception {

    }

}