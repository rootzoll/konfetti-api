package de.konfetti.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.konfetti.Application;
import de.konfetti.controller.vm.KeyAndPasswordVM;
import de.konfetti.data.ClientRepository;
import de.konfetti.data.Party;
import de.konfetti.data.User;
import de.konfetti.data.UserRepository;
import de.konfetti.service.ClientService;
import de.konfetti.service.PartyService;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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
import java.util.List;
import java.util.Locale;

import static de.konfetti.utils.WiserAssertions.assertReceivedMessage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
    protected ClientService clientService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ClientRepository clientRepository;

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
        ValidatableResponse validatableResponse = insertUser(testEmail, testPassword)
                .statusCode(200);

        validatableResponse
                .body("id", notNullValue())
                .body("email", equalToIgnoringCase(testEmail))
                .body("password", isEmptyOrNullString())
                .body("clientId", greaterThan(0))
        ;

        // make sure client object is persisted and linked with user object
        assertThat(userRepository.findByEMail("testcreateuser@test.de").getClients().size(), greaterThan(0));
        assertThat(clientRepository.findAll().get(0).getUser(), notNullValue());
    }

    @Test
    public void createUserWithExistingEmail() throws Exception {
        String testEmail = "createuserwithexistingemail@test.de";
        String testPassword = "createUser";
        // check user is created
        insertUser(testEmail, testPassword);

        // should fail
        ValidatableResponse response = insertUser(testEmail, testPassword);
        response
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(is("\"User already exists with this email\""));
    }

    @Test
    public void testRequestPasswordReset() throws Exception {
        User user = testHelper.getUser("testRequestPasswordReset");
        insertUser(user.getEMail(), user.getPassword());
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
    public void testRequestPasswordFinish() throws Exception {
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
        insertUser(user.getEMail(), user.getPassword());
        myGiven()
                .param("mail", user.getEMail())
                .param("pass", user.getPassword())
                .get(UserController.REST_API_MAPPING + "/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("name", nullValue())
                .body("password", isEmptyOrNullString())
                .body("imageMediaID", nullValue())
                .body("spokenLangs", hasItem("en"))
                .body("activeOnParties", is(empty()))
                .body("adminOnParties", is(empty()))
                .body("reviewerOnParties", is(empty()))
                .body("lastActivityTS", greaterThan(1000L))
                .body("pushActive", is(false))
                .body("pushSystem", nullValue())
                .body("pushID", nullValue())
                .body("resetKey", nullValue())
                .body("resetDate", nullValue())
                .body("clientId", notNullValue())
                .body("clientSecret", notNullValue())
                .body("email", is(user.getEMail()))
        ;
    }

    @Test
    public void loginWrongPassword() throws Exception {
        User user = testHelper.getUser("testLoginWrongPassword");
        insertUser(user.getEMail(), user.getPassword());
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
        insertUser(user.getEMail(), user.getPassword());
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
    public void redeemAdminCode() throws Exception {
        Party partyCreated = partyService.create(testHelper.getParty("partyTestGenerateCodesAdmin"));
        ValidatableResponse responseCreateCode = myGiven()
                .header(ControllerSecurityHelper.HEADER_ADMIN_PASSWORD, "admin")
                .pathParam("partyId", partyCreated.getId())
                .get(UserController.REST_API_MAPPING + "/codes-admin/{partyId}")
                .then()
                .statusCode(HttpStatus.OK.value());

        String jsonString = responseCreateCode.extract().response().print();
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> listOfCodes = objectMapper.readValue(jsonString, new TypeReference<List<String>>(){});
        assertThat(listOfCodes, hasSize(1));

        String email = "redeemadmincode@test.de";
        insertUser(email, "test123");
        User createdUser = userRepository.findByEMail(email);

        ValidatableResponse responseRedeemCode = myGiven()
                .header(ControllerSecurityHelper.HEADER_ADMIN_PASSWORD, "admin")
                .header("X-CLIENT-ID", createdUser.getId())
                .header("X-CLIENT-SECRET", clientService.findById(createdUser.getId()).getSecret())
                .pathParam("code", listOfCodes.get(0))
                .get(UserController.REST_API_MAPPING + "/redeem/{code}")
                .then()
                .statusCode(HttpStatus.OK.value());

        responseRedeemCode
                .body("actions", hasSize(2))
                .body("feedbackHtml", is("You are now ADMIN on the following party."))
        ;
    }

    @Test
    public void recover() throws Exception {

    }

    @Test
    public void updateUser() throws Exception {

    }

}