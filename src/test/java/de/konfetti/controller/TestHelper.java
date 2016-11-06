package de.konfetti.controller;

import de.konfetti.data.*;
import de.konfetti.maker.PartyMaker;
import de.konfetti.maker.UserMaker;

import java.util.Date;

import static com.natpryce.makeiteasy.MakeItEasy.an;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static de.konfetti.data.enums.CodeActionTypeEnum.ACTION_TYPE_KONFETTI;
import static de.konfetti.maker.MediaMaker.ExampleMediaItem;
import static de.konfetti.maker.PartyMaker.ExampleParty;
import static de.konfetti.maker.UserMaker.ExampleUser;
import static de.konfetti.maker.UserMaker.name;

public class TestHelper {
	
    public TestHelper() {
    }

    public User getUser(String username){
        String email = username + "@test.de";
        return make(an(ExampleUser).but(with(name, username), with(UserMaker.email, email.toLowerCase())));
    }


    public Party getParty(String partyName){
        Party party = make(an(ExampleParty).but(with(PartyMaker.name, partyName)));
        party.setId(null);
        return party;
    }

    public MediaItem getMediaItem(){
        return make(an(ExampleMediaItem));
    }

    public Party getTestParty1() {
        Party testParty =  new Party();
        testParty.setName("testParty1");
        testParty.setLat(new Float("0.0"));
        testParty.setLon(new Float("0.0"));
        return  testParty;
    }

    public Party getTestParty2() {
        Party testParty =  new Party();
        testParty.setName("testParty2");
        testParty.setLat(new Float("0.0"));
        testParty.setLon(new Float("0.0"));
        return  testParty;

    }

    public Request getTestRequest1(Party party){
        Request request = new Request();
        request.setTitle("testPartyTitle1");
        request.setKonfettiAdd(10);
        request.setKonfettiCount(20);
        request.setPartyId(party.getId());
        return request;
    }

    public Code getTestCodeKonfetti1(long partyId, long userId, String codeString){
        Code code = new Code();
        code.setAmount((long) 1000);
        code.setActionType(ACTION_TYPE_KONFETTI);
        code.setTimestamp(new Date().getTime());
        code.setCode(codeString);
        code.setPartyID(partyId);
        code.setUserID(userId);
        return code;
    }

//    public boolean equalPartys(Party actual, Party expected) {
//        if (actual.getName() != null ? !actual.getName().equals(expected.getName()) : expected.getName() != null) return false;
//        if (actual.getAddress() != null ? !actual.getAddress().equals(expected.getAddress()) : expected.getAddress() != null) return false;
//        return !(actual.getPerson() != null ? !actual.getPerson().equals(expected.getPerson()) : expected.getPerson() != null);
//    }

    public boolean equalRequests(Request actual, Request expected){
        // TODO: repair to work again -> link between party and requests
        // if (actual.getPartyId() != expected.getPartyId()) return false;
//        if (actual.getTime() != null ? !actual.getTime().equals(expected.getTime()) : expected.getTime() != null) return false;
        if (actual.getTitle() != null ? !actual.getTitle().equals(expected.getTitle()) : expected.getTitle() != null) return false;
        return !(actual.getImageMediaID() != expected.getImageMediaID());
    }

}