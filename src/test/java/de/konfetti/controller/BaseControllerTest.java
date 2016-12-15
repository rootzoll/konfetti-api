package de.konfetti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.konfetti.Application;
import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.controller.vm.UserResponse;
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

import java.io.IOException;

import static io.restassured.RestAssured.given;

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

    protected ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        testHelper = new TestHelper();
        emailPort = 2500;
        wiser = new Wiser(emailPort);
        wiser.start();
        objectMapper = new ObjectMapper();
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

    protected RequestSpecification myGivenUser(UserResponse userResponse){
        return myGiven()
                .header("X-CLIENT-ID", userResponse.getId())
                .header("X-CLIENT-SECRET", userResponse.getClientSecret())
                ;
    }

    protected ValidatableResponse insertUser(String email, String password) {
        return myGiven()
                .param("mail", email.toLowerCase()).param("pass", password)
                .when().post(UserController.REST_API_MAPPING)
                .then()
        ;
    }

    protected PartyResponse insertParty(PartyResponse party) throws IOException {
        ValidatableResponse validatableResponse = myGiven()
                .contentType(ContentType.JSON)
                .body(party)
                .when().post(PartyController.REST_API_MAPPING)
                .then();
        return objectMapper.readValue(validatableResponse.extract().response().prettyPrint(), PartyResponse.class);
    }

    protected RequestVm insertRequest(RequestVm requestVm, UserResponse userResponse) throws IOException {
        ValidatableResponse validatableResponse = myGivenUser(userResponse)
                .contentType(ContentType.JSON)
                .body(requestVm)
                .pathParam("partyId", requestVm.getPartyId())
                .pathParam("langCode", "en")
                .when().post(PartyController.REST_API_MAPPING + "/{partyId}/{langCode}/request")
                .then().statusCode(HttpStatus.OK.value());

        RequestVm requestVmResponse = objectMapper.readValue(validatableResponse.extract().response().prettyPrint(), RequestVm.class);
        return requestVmResponse;
    }


}


