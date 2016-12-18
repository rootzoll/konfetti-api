package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.data.Party;

/**
 * Created by relampago on 06.11.16.
 */
public class PartyMapper {

    public PartyResponse toPartyResponse(Party party) {
        if (party == null) {
            return null;
        }
        PartyResponse partyResponse = new PartyResponse(party.getId());
        partyResponse.setName(party.getName());
        partyResponse.setDetailText(party.getDetailText());
        partyResponse.setContact(party.getContact());
        partyResponse.setVisibility(party.getVisibility());
        partyResponse.setReviewLevel(party.getReviewLevel());
        partyResponse.setNewRequestMinKonfetti(party.getNewRequestMinKonfetti());
        partyResponse.setWelcomeBalance(party.getWelcomeBalance());
        partyResponse.setLon(party.getLon());
        partyResponse.setLat(party.getLat());
        partyResponse.setMeters(party.getMeters());
        partyResponse.setSendKonfettiMode(party.getSendKonfettiMode());
        return partyResponse;
    }

    public Party fromPartyResponse(PartyResponse partyResponse) {
        if (partyResponse == null) {
            return null;
        }
        Party party = new Party();
        party.setId(partyResponse.getId());
        party.setName(partyResponse.getName());
        party.setDetailText(partyResponse.getDetailText());
        party.setContact(partyResponse.getContact());
        party.setVisibility(partyResponse.getVisibility());
        party.setReviewLevel(partyResponse.getReviewLevel());
        party.setNewRequestMinKonfetti(partyResponse.getNewRequestMinKonfetti());
        party.setWelcomeBalance(partyResponse.getWelcomeBalance());
        party.setLon(partyResponse.getLon());
        party.setLat(partyResponse.getLat());
        party.setMeters(partyResponse.getMeters());
        party.setSendKonfettiMode(partyResponse.getSendKonfettiMode());
        return party;
    }
}
