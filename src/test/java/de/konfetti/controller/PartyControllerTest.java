package de.konfetti.controller;

import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.maker.PartyResponseMaker;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static de.konfetti.maker.PartyResponseMaker.ExamplePartyResponse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by relampago on 06.11.16.
 */
public class PartyControllerTest extends BaseControllerTest {

    @MockBean
    ControllerSecurityHelper controllerSecurityHelperMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        when(controllerSecurityHelperMock.checkAdminLevelSecurity(any(HttpServletRequest.class))).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void createParty() throws Exception {
        PartyResponse party = make(an(ExamplePartyResponse).but(with(PartyResponseMaker.name, "createParty")));
        ValidatableResponse validatableResponse = myGiven()
                .contentType(ContentType.JSON)
                .body(party)
                .when().post(PartyController.REST_API_MAPPING)
                .then().statusCode(HttpStatus.OK.value());

        validatableResponse
                .body("id", is(greaterThan(0)))
                .body("name", is(party.getName()))
                .body("detailText", is(party.getDetailText()))
                .body("contact", is(party.getContact()))
                .body("visibility", is(party.getVisibility().name()))
                .body("reviewLevel", is(party.getReviewLevel().name()))
                .body("newRequestMinKonfetti", is(party.getNewRequestMinKonfetti()))
                .body("welcomeBalance", is((int)party.getWelcomeBalance()))
                .body("lon", is(party.getLon()))
                .body("lat", is(party.getLat()))
                .body("meters", is(party.getMeters()))
                .body("distanceToUser", is(party.getDistanceToUser()))
                .body("sendKonfettiMode", is(party.getSendKonfettiMode().name()))
                .body("sendKonfettiWhiteList", hasSize(party.getSendKonfettiWhiteList().length))
                .body("konfettiCount", is((int)party.getKonfettiCount()))
                .body("konfettiTotal", is((int)party.getKonfettiTotal()))
                .body("topPosition", is(party.getTopPosition()))
                .body("requests", hasSize(party.getRequests().size()))
                .body("notifications", hasSize(party.getNotifications().size()))
                ;
    }

}
