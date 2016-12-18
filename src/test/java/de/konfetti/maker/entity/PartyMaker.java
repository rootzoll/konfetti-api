package de.konfetti.maker.entity;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import de.konfetti.data.Party;
import de.konfetti.data.enums.PartyReviewLevelEnum;
import de.konfetti.data.enums.PartyVisibilityEnum;

import static com.natpryce.makeiteasy.Property.newProperty;
import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;

/**
 * Created by relampago on 05.10.16.
 */
public class PartyMaker {

    public static final Property<Party, String> name = newProperty();
    public static final Property<Party, String> detailText = newProperty();
    public static final Property<Party, String> contact = newProperty();
    public static final Property<Party, PartyVisibilityEnum> visibility = newProperty();
    public static final Property<Party, PartyReviewLevelEnum> reviewLevel = newProperty();
    public static final Property<Party, Integer> newRequestMinKonfetti = newProperty();
    public static final Property<Party, Long> welcomeBalance = newProperty();
    public static final Property<Party, Float> lon = newProperty();
    public static final Property<Party, Float> lat = newProperty();
    public static final Property<Party, Integer> meters = newProperty();

    public static final Instantiator<Party> ExampleParty = new Instantiator<Party>() {
        @Override
        public Party instantiate(PropertyLookup<Party> propertyLookup) {
            Party party = new Party();
            party.setName(propertyLookup.valueOf(name, "testParty"));
            party.setDetailText(propertyLookup.valueOf(detailText, "testPartyDetailsText"));
            party.setContact(propertyLookup.valueOf(contact, "testPartyContact"));
            party.setVisibility(propertyLookup.valueOf(visibility, VISIBILITY_PUBLIC));
            party.setReviewLevel(propertyLookup.valueOf(reviewLevel, REVIEWLEVEL_NONE));
            party.setNewRequestMinKonfetti(propertyLookup.valueOf(newRequestMinKonfetti, Integer.parseInt("0")));
            party.setWelcomeBalance(propertyLookup.valueOf(welcomeBalance, Long.parseLong("0")));
            party.setLon(propertyLookup.valueOf(lon, Float.parseFloat("0")));
            party.setLat(propertyLookup.valueOf(lat, Float.parseFloat("0")));
            party.setMeters(propertyLookup.valueOf(meters, Integer.parseInt("10000")));
            return party;
        }
    };

}
