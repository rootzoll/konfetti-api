package de.konfetti.controller;

import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.controller.vm.UserResponse;
import de.konfetti.data.enums.RequestStateEnum;
import de.konfetti.maker.dto.PartyResponseMaker;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static de.konfetti.maker.dto.PartyResponseMaker.ExamplePartyResponse;
import static de.konfetti.maker.dto.RequestVmMaker.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
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
        when(controllerSecurityHelperMock.getClientFromRequestWhileCheckAuth(any(HttpServletRequest.class), anyObject())).thenCallRealMethod();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void createParty() throws Exception {
        @SuppressWarnings("unchecked")
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
                .body("welcomeBalance", is((int) party.getWelcomeBalance()))
                .body("lon", is(party.getLon()))
                .body("lat", is(party.getLat()))
                .body("meters", is(party.getMeters()))
                .body("distanceToUser", is(party.getDistanceToUser()))
                .body("sendKonfettiMode", is(party.getSendKonfettiMode().name()))
                .body("sendKonfettiWhiteList", hasSize(party.getSendKonfettiWhiteList().length))
                .body("konfettiCount", is((int) party.getKonfettiCount()))
                .body("konfettiTotal", is((int) party.getKonfettiTotal()))
                .body("topPosition", is(party.getTopPosition()))
                .body("requests", hasSize(party.getRequests().size()))
                .body("notifications", hasSize(party.getNotifications().size()))
        ;
    }


    @Test
    public void createRequest() throws IOException {
        @SuppressWarnings("unchecked")
		PartyResponse party = make(an(ExamplePartyResponse).but(with(PartyResponseMaker.name, "createRequest")));
        PartyResponse partyResponse = createParty(party);
        UserResponse userResponse = createUser("createRequest", "createRequestPassword");

        @SuppressWarnings("unchecked")
		RequestVm requestToSend = make(an(ExampleRequestVm)
                .but(with(userId, userResponse.getId()))
                .but(with(partyId, partyResponse.getId()))
        );


        ValidatableResponse validatableResponse = myGivenUser(userResponse)
                .contentType(ContentType.JSON)
                .body(requestToSend)
                .pathParam("partyId", partyResponse.getId())
                .pathParam("langCode", "en")
                .when().post(PartyController.REST_API_MAPPING + "/{partyId}/{langCode}/request")
                .then().statusCode(HttpStatus.OK.value());

        RequestVm requestVmResponse = objectMapper.readValue(validatableResponse.extract().response().prettyPrint(), RequestVm.class);
        assertRequestVmEqual(requestToSend, requestVmResponse);
    }


    @Test
    public void updateRequest() throws IOException {
        @SuppressWarnings("unchecked")
		PartyResponse party = make(an(ExamplePartyResponse).but(with(PartyResponseMaker.name, "updateRequest")));
        PartyResponse partyResponse = createParty(party);
        UserResponse userResponse = createUser("updateRequest", "updateRequestPassword");

        @SuppressWarnings("unchecked")
		RequestVm requestToSend = make(an(ExampleRequestVm)
                .but(with(userId, userResponse.getId()))
                .but(with(partyId, partyResponse.getId()))
        );

        RequestVm requestVmUpdate = createRequest(requestToSend, userResponse);

        requestVmUpdate.setTitle("changedTitle");
        requestVmUpdate.setTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        requestVmUpdate.setKonfettiAdd(Long.valueOf(50L));
        requestVmUpdate.setKonfettiCount(Long.valueOf(150L));


        ValidatableResponse validatableResponse = myGivenAdmin()
                .contentType(ContentType.JSON)
                .body(requestVmUpdate)
                .pathParam("partyId", requestVmUpdate.getPartyId())
                .when().put(PartyController.REST_API_MAPPING + "/{partyId}/request")
                .then().statusCode(HttpStatus.OK.value());

        RequestVm requestVmUpdateResponse = objectMapper.readValue(validatableResponse.extract().response().prettyPrint(), RequestVm.class);
        assertRequestVmEqual(requestVmUpdate, requestVmUpdateResponse);
    }

    private void assertRequestVmEqual(RequestVm expecteRequestVm, RequestVm actualRequestVm) throws IOException {

        assertThat(actualRequestVm.getId(), notNullValue());
        assertThat(actualRequestVm.getUserId(), is(expecteRequestVm.getUserId()));
        assertThat(actualRequestVm.getPartyId(), is(expecteRequestVm.getPartyId()));
        assertThat(actualRequestVm.getState(), is(RequestStateEnum.STATE_OPEN));
        assertThat(actualRequestVm.getTitle(), is(expecteRequestVm.getTitle()));
        assertThat(actualRequestVm.getTitleMultiLangRef(), notNullValue());
        assertThat(actualRequestVm.getTitle(), is(expecteRequestVm.getTitle()));
        // TODO: check time is close to SystemTime
        assertThat(actualRequestVm.getTime(), notNullValue());
        assertThat(actualRequestVm.getMediaItemIds(), is(expecteRequestVm.getMediaItemIds()));
        assertThat(actualRequestVm.getUserName(), is(expecteRequestVm.getUserName()));
        assertThat(actualRequestVm.getImageMediaID(), is(expecteRequestVm.getImageMediaID()));
        assertThat(actualRequestVm.getSpokenLangs(), is(expecteRequestVm.getSpokenLangs()));
        assertThat(actualRequestVm.getKonfettiCount(), is(expecteRequestVm.getKonfettiCount()));
        assertThat(actualRequestVm.getKonfettiAdd(), is(expecteRequestVm.getKonfettiAdd()));
        assertThat(actualRequestVm.getChats(), is(expecteRequestVm.getChats()));
        assertThat(actualRequestVm.getInfo(), is(expecteRequestVm.getInfo()));
        assertThat(actualRequestVm.getTitleMultiLang(), is(expecteRequestVm.getTitleMultiLang()));
        assertThat(actualRequestVm.getKonfettiAmountSupport(), is(expecteRequestVm.getKonfettiAmountSupport()));
        assertThat(actualRequestVm.getKonfettiAmountReward(), is(expecteRequestVm.getKonfettiAmountReward()));
    }

}
