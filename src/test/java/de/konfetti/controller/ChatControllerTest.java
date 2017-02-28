package de.konfetti.controller;

import de.konfetti.controller.vm.ChatDto;
import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.controller.vm.UserResponse;
import de.konfetti.maker.dto.RequestVmMaker;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static de.konfetti.maker.dto.ChatDtoMaker.*;
import static de.konfetti.maker.dto.PartyResponseMaker.ExamplePartyResponse;
import static de.konfetti.maker.dto.PartyResponseMaker.name;
import static de.konfetti.maker.dto.RequestVmMaker.ExampleRequestVm;
import static de.konfetti.maker.dto.RequestVmMaker.userId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by relampago on 18.12.16.
 */
public class ChatControllerTest extends BaseControllerTest {


    @Test
    public void setChatPartnerInfoOn() throws Exception {

    }

    @Test
    public void createChat() throws Exception {
        // Arrange
        String testName = "createChat";
        UserResponse user = createUser("createChat@test.de", "test1234");
        UserResponse chatPartner = createUser("createChatPartner@test.de", "test1234");

        @SuppressWarnings("unchecked")
		PartyResponse party = createParty(make(an(ExamplePartyResponse).but(with(name, testName))));
        @SuppressWarnings("unchecked")
		RequestVm requestVm = make(an(ExampleRequestVm).but(with(userId, user.getId()), with(RequestVmMaker.partyId, party.getId())));
        RequestVm requestResponse = createRequest(requestVm, user);

        @SuppressWarnings("unchecked")
		ChatDto chatDto = make(an(ExampleChatDto)
                .but(with(hostId, user.getId()),
                        with(partyId, party.getId()),
                        with(requestId, requestResponse.getId()),
                        with(members, new Long[]{chatPartner.getId()})));


        // Act
        ValidatableResponse validatableResponse = myGivenUser(user)
                .contentType(ContentType.JSON)
                .body(chatDto)
                .when().post(ChatController.REST_API_MAPPING)
                .then().statusCode(HttpStatus.OK.value());


        // Assert
        ChatDto chat = objectMapper.readValue(validatableResponse.extract().response().prettyPrint(), ChatDto.class);
        assertThat(chat.getId(), notNullValue());
        assertThat(chat.getRequestId(), is(requestResponse.getId()));
        assertThat(chat.getPartyId(), is(party.getId()));
        assertThat(chat.getHostId(), is(user.getId()));
        assertThat(chat.getMembers(), hasItemInArray(chatPartner.getId()));
        assertThat(chat.getMuted(), is(false));
        assertThat(chat.getLastTSperMember(), notNullValue());
        assertThat(chat.getMessages(), notNullValue());
        assertThat(chat.getChatPartnerId(), is(chatPartner.getId()));
        assertThat(chat.getChatPartnerName(), nullValue());
        assertThat(chat.getChatPartnerImageMediaID(), nullValue());
        assertThat(chat.getChatPartnerSpokenLangs(), hasItemInArray("en"));
        assertThat(chat.isUnreadMessage(), is(false));
    }

    @Test
    public void getChat() throws Exception {

    }

    @Test
    public void addMessage() throws Exception {

    }

    @Test
    public void actionMessage() throws Exception {

    }

}