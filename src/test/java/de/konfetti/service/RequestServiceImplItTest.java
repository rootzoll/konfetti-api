package de.konfetti.service;

import de.konfetti.Application;
import de.konfetti.controller.TestHelper;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.*;
import de.konfetti.maker.entity.PartyMaker;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.*;
import static de.konfetti.maker.entity.PartyMaker.ExampleParty;
import static de.konfetti.maker.entity.UserMaker.ExampleUser;
import static de.konfetti.maker.entity.UserMaker.name;
import static org.junit.Assert.*;

/**
 * Created by catarata02 on 08.11.15.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class RequestServiceImplItTest {

    private final TestHelper testHelper = new TestHelper();

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MediaRepository mediaRepository;

    private RequestService requestService;

    private PartyService partyService;

    @Autowired
    private UserRepository userRepository;

    @Before
    public void setUp() throws Exception {
        requestService = new RequestServiceImpl(partyRepository, requestRepository, accountRepository, mediaRepository);
        partyService = new PartyServiceImpl(partyRepository);

        partyRepository.deleteAll();
    }

    @Test
    public void testCreateRequest() throws Exception {
        String testName = "testCreateRequest";
        User user = persistUser(testName);
        Party party = persistParty(testName);

        Request testRequest = testHelper.getTestRequest1(party, user);
        Request createdRequest = requestService.create(testRequest);

        // assert all values correctly stored
        assertTrue("Request created successfully", testHelper.equalRequests(createdRequest, testRequest));
        assertNotNull("id not null", createdRequest.getId());
        assertTrue("id not 0", createdRequest.getId() > 0);
        // TODO: should work again the link betweeen parties and requests
        // assertEquals("same party in request", party, createdRequest.getPartyId());
        // assertTrue("same request in party", testHelper.equalRequests(createdRequest, partyService.findByName(party.getName());
    }



    @Ignore
    @Test
    public void testUpdateRequest() throws Exception {
        String testName = "testUpdateRequest";
        Party party = persistParty(testName);
        User user = persistUser(testName);

        Request testRequest = testHelper.getTestRequest1(party, user);
        Request createdRequest = requestService.create(testRequest);

        String modiefiedTitle = "modiefiedTitle";
        createdRequest.setTitle(modiefiedTitle);
        Long modifiedImageUrl = new Long(1234);
        createdRequest.setImageMediaID(modifiedImageUrl);
        Request updatedRequest = requestService.update(createdRequest);

        // assert all values correctly stored
        assertTrue("Party updated successfully", testHelper.equalRequests(updatedRequest, createdRequest));
        assertNotNull("id not null", updatedRequest.getId());
        assertTrue("id not 0", updatedRequest.getId() > 0);
    }

    @Ignore
    @Test
    public void testDeleteRequest() throws Exception {
        String testName = "testUpdateRequest";
        Party party = persistParty(testName);
        User user = persistUser(testName);

        Request testRequest = testHelper.getTestRequest1(party, user);
        Request createdRequest = requestService.create(testRequest);

        Request deletedRequest = requestService.delete(createdRequest.getId());

        // assert all values correctly stored
        // assertTrue("Request deleted successfully", testHelper.equalRequests(deletedRequest, testRequest));

        // assert that request is not existing anymore
        List<RequestVm> lists = requestService.getAllPartyRequests(party.getId());
        assertEquals("no Request found", 0, lists.size());
    }


    private Party persistParty(String testName) {
        Party testParty = make(an(ExampleParty).but(with(PartyMaker.name, testName)));
        return partyService.create(testParty);
    }

    private User persistUser(String testName) {
        User testUser = make(an(ExampleUser).but(with(name, testName)));
        return userRepository.save(testUser);
    }

}