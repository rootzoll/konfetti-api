package de.konfetti.controller;

import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.subethamail.wiser.Wiser;

import static io.restassured.RestAssured.given;

/**
 * Created by relampago on 11.10.16.
 */
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

    protected RequestSpecification myGiven(){
        return given().port(serverPort).log().ifValidationFails(LogDetail.ALL);
    }

    protected RequestSpecification myGivenAdmin(){
        return myGiven()
                .header(ControllerSecurityHelper.HEADER_ADMIN_PASSWORD, "admin");
    }

}
