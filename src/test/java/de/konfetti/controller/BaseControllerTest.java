package de.konfetti.controller;

import de.konfetti.Application;
import de.konfetti.data.Party;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.subethamail.wiser.Wiser;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

/**
 * Created by relampago on 11.10.16.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BaseControllerTest {

    protected Wiser wiser;

    protected int emailPort;

    protected TestHelper testHelper;

    @Value("${konfetti.sendFromMailAddress}")
    protected String emailFrom;

    @Value("${konfetti.replyToMailAddress}")
    protected String replyToAddress;

    @Autowired
    protected MessageSource messageSource;

    @Value("${local.server.port}")
    int serverPort;

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
    public void testDummy(){
    }

    protected RequestSpecification myGiven(){
        return given().port(serverPort).log().ifValidationFails(LogDetail.ALL);
    }

    protected RequestSpecification myGivenAdmin(){
        return myGiven()
                .header(ControllerSecurityHelper.HEADER_ADMIN_PASSWORD, "admin");
    }

    protected void createUser(String email, String password) {
        myGiven()
                .param("mail", email.toLowerCase()).param("pass", password)
                .when().post(UserController.REST_API_MAPPING)
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("email", equalToIgnoringCase(email))
                .body("password", isEmptyOrNullString())
        ;
    }

    protected ValidatableResponse createAndInsertParty(String partyName) {
        Party party = testHelper.getParty(partyName);
        ValidatableResponse validatableResponse = myGiven()
                .contentType(ContentType.JSON)
                .body(party)
                .when().post(PartyController.REST_API_MAPPING)
                .then().statusCode(HttpStatus.OK.value())
                .body("name", equalToIgnoringCase(partyName));
        return validatableResponse;
    }
}


