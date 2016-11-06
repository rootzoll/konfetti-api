package de.konfetti.maker;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.data.Party;
import de.konfetti.data.enums.PartyReviewLevelEnum;
import de.konfetti.data.enums.PartyVisibilityEnum;

import static com.natpryce.makeiteasy.Property.newProperty;
import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;

/**
 * Created by relampago on 05.10.16.
 */
public class PartyResponseMaker {

    public static final Property<PartyResponse, String> name = newProperty();
    public static final Property<PartyResponse, String> detailText = newProperty();
    public static final Property<PartyResponse, String> contact = newProperty();
    public static final Property<PartyResponse, PartyVisibilityEnum> visibility = newProperty();
    public static final Property<PartyResponse, PartyReviewLevelEnum> reviewLevel = newProperty();
    public static final Property<PartyResponse, Integer> newRequestMinKonfetti = newProperty();
    public static final Property<PartyResponse, Long> welcomeBalance = newProperty();
    public static final Property<PartyResponse, Float> lon = newProperty();
    public static final Property<PartyResponse, Float> lat = newProperty();
    public static final Property<PartyResponse, Integer> meters = newProperty();

    public static final Instantiator<PartyResponse> ExamplePartyResponse = new Instantiator<PartyResponse>() {
        @Override
        public PartyResponse instantiate(PropertyLookup<PartyResponse> propertyLookup) {
            PartyResponse partyResponse = new PartyResponse(null);
            partyResponse.setName(propertyLookup.valueOf(name, "testParty"));
            partyResponse.setDetailText(propertyLookup.valueOf(detailText, "testPartyDetailsText"));
            partyResponse.setContact(propertyLookup.valueOf(contact, "testPartyContact"));
            partyResponse.setVisibility(propertyLookup.valueOf(visibility, VISIBILITY_PUBLIC));
            partyResponse.setReviewLevel(propertyLookup.valueOf(reviewLevel, REVIEWLEVEL_NONE));
            partyResponse.setNewRequestMinKonfetti(propertyLookup.valueOf(newRequestMinKonfetti, Integer.parseInt("0")));
            partyResponse.setWelcomeBalance(propertyLookup.valueOf(welcomeBalance, Long.parseLong("0")));
            partyResponse.setLon(propertyLookup.valueOf(lon, Float.parseFloat("0")));
            partyResponse.setLat(propertyLookup.valueOf(lat, Float.parseFloat("0")));
            partyResponse.setMeters(propertyLookup.valueOf(meters, Integer.parseInt("10000")));
            return partyResponse;
        }
    };

}
