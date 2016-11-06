package de.konfetti.service;

import de.konfetti.controller.TestHelper;
import de.konfetti.data.Party;
import de.konfetti.data.PartyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PRIVATE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

/**
 * Created by catarata02 on 08.11.15.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PartyServiceImplItTest {

    private final TestHelper testHelper = new TestHelper();

    @Autowired
    private PartyRepository partyRepository;

    private PartyService partyService;

    @Before
    public void setUp() throws Exception {
    	
        partyService = new PartyServiceImpl(partyRepository);
        partyRepository.deleteAll();
    }

    @Test
    public void testCreateParty() throws Exception {
    	
        Party testParty = testHelper.getTestParty1();
        Party createdParty = partyService.create(testParty);

        // assert all values correctly stored
        assertNotNull("id not null", createdParty.getId());
        assertTrue("id not 0", createdParty.getId() > 0);
    }

    @Test
    public void testUpdateParty() throws Exception {
    	
        Party testParty = partyService.create(testHelper.getTestParty1());
        String modiefiedName = "modiefiedName";
        testParty.setName(modiefiedName);
        Party modfiedParty = partyService.update(testParty);

        // assert all values correctly stored
        assertNotNull("id not null", modfiedParty.getId());
        assertTrue("id not 0", modfiedParty.getId() > 0);
    }

    @Test
    public void testDeleteParty() throws Exception {
    	
        Party testParty = partyService.create(testHelper.getTestParty1());
        partyService.delete(testParty.getId());

        // assert that list is not existing anymore
        List<Party> lists = partyService.getAllParties();
        assertEquals("no list found", 0, lists.size());
    }

    @Test
    public void testFindByVisibiity() throws Exception {
        Party visibleParty = testHelper.getTestParty1();
        visibleParty.setVisibility(VISIBILITY_PUBLIC);
        visibleParty = partyService.create(visibleParty);
        Party invisibleParty = testHelper.getTestParty2();
        invisibleParty.setVisibility(VISIBILITY_PRIVATE);
        invisibleParty = partyService.create(invisibleParty);

        List<Party> visibleParties = partyRepository.findByVisibility(VISIBILITY_PUBLIC);
        assertThat("one visible Party found", visibleParties.size(), is(1));
        assertThat("visible Party found", visibleParties.get(0), equalTo(visibleParty));
    }

}