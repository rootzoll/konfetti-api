package de.konfetti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.konfetti.controller.mapper.ChatMapper;
import de.konfetti.controller.mapper.NotificationMapper;
import de.konfetti.controller.mapper.PartyMapper;
import de.konfetti.controller.mapper.RequestMapper;
import de.konfetti.controller.vm.ChatDto;
import de.konfetti.controller.vm.NotificationDto;
import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.*;
import de.konfetti.data.mediaitem.MultiLang;
import de.konfetti.service.*;
import de.konfetti.utils.AccountingTools;
import de.konfetti.utils.AutoTranslator;
import de.konfetti.utils.Helper;
import de.konfetti.utils.NotificationManager;
import de.konfetti.websocket.CommandMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static de.konfetti.data.enums.MediaItemReviewEnum.REVIEWED_PUBLIC;
import static de.konfetti.data.enums.MediaItemTypeEnum.TYPE_MULTILANG;
import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_DEACTIVATED;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;
import static de.konfetti.data.enums.RequestStateEnum.*;
import static de.konfetti.data.enums.SendKonfettiModeEnum.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(PartyController.REST_API_MAPPING)
public class PartyController {

    public static final String REST_API_MAPPING = "konfetti/api/party";

    private static final Gson GSON = new GsonBuilder().create();

    private final PartyService partyService;

    private final RequestService requestService;

    private final NotificationService notificationService;

    private final ClientService clientService;

    private final AccountingService accountingService;

    private final UserService userService;

    private final ChatService chatService;

    private final MediaService mediaService;

    private final KonfettiTransactionService konfettiTransactionService;

    @Autowired
    private ControllerSecurityHelper controllerSecurityHelper;

    @Autowired
    private AutoTranslator autoTranslator;

    @Autowired
    private SimpMessagingTemplate webSocket;

    @Autowired
    private MediaRepository mediaRepository;
    
    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private RequestMapper requestMapper;

    @Autowired
    private ChatMapper chatMapper;

    private PartyMapper partyMapper;

    @Autowired
    public PartyController(
            final PartyService partyService,
            final RequestService requestService,
            final NotificationService notificationService,
            final ClientService clientService,
            final AccountingService accountingService,
            final UserService userService,
            final ChatService chatService,
            final MediaService mediaService,
            final KonfettiTransactionService konfettiTransactionService
    ) {

        this.partyService = partyService;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.clientService = clientService;
        this.accountingService = accountingService;
        this.userService = userService;
        this.chatService = chatService;
        this.mediaService = mediaService;
        this.konfettiTransactionService = konfettiTransactionService;
        partyMapper = new PartyMapper();

    }

    //---------------------------------------------------
    // DASHBOARD Info
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public DashBoardInfo getDashBaordInfo(HttpServletRequest request) throws Exception {

    	log.info("*** GET Dashboard ***");
    	
        controllerSecurityHelper.checkAdminLevelSecurity(request);
        DashBoardInfo info = new DashBoardInfo();

        info.numberOfUsers = userService.getNumberOfActiveUsers();
        info.numberOfParties = partyService.getNumberOfParties();
        info.numberOfTasks = requestService.getNumberOfRequests();
        info.numberOfKonfetti = accountingService.getAllKonfettiBalance();

        return info;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.POST)
    public PartyResponse createParty(@RequestBody @Valid final PartyResponse partyResponse, HttpServletRequest request) throws Exception {
        
    	log.info("*** POST Party ***");
    	
    	controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Creating PARTY(" + partyResponse.getId() + ")");
        Party createdParty = partyService.create(partyMapper.fromPartyResponse(partyResponse));
        return partyMapper.toPartyResponse(createdParty);
    }

    //---------------------------------------------------
    // PARTY Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.PUT)
    public PartyResponse updateParty(@RequestBody @Valid final PartyResponse partyResponse, HttpServletRequest request) throws Exception {
        
    	log.info("*** PUT Update Party ***");
    	
    	controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Updating PARTY(" + partyResponse.getId() + ")");
        Party party = partyMapper.fromPartyResponse(partyResponse);
        Party updatedParty = partyService.update(party);
        return partyMapper.toPartyResponse(updatedParty);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}", method = RequestMethod.DELETE)
    public boolean deleteParty(@PathVariable long partyId, HttpServletRequest request) throws Exception {
    	
    	log.info("*** DELETE Party ***");
    	
        controllerSecurityHelper.checkAdminLevelSecurity(request);
     
    	/* real delete needs to delete also all connected data
        partyService.delete(partyId);
        */

    	/*
         * just deactiavte for now
    	 */

        Party party = partyService.findById(partyId);
        party.setVisibility(VISIBILITY_DEACTIVATED);
        partyService.update(party);

        return true;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}", method = RequestMethod.GET)
    public PartyResponse getParty(@PathVariable long partyId, @RequestParam(value = "lastTS", defaultValue = "0") long lastTs, HttpServletRequest request) throws Exception {

    	log.info("*** GET Party ("+partyId+") ***");
    	
        PartyResponse partyResponse = partyMapper.toPartyResponse(partyService.findById(partyId));
        if (partyResponse == null)
            throw new Exception("was not able to load party with id(" + partyId + ") - NOT FOUND");


        // if user/client is set by header -> add requests and notifications important to user
        try {
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(request, clientService);

            if (client != null) {
                log.debug("getParty - adding info for client(" + client.getId() + ")");

                User user = userService.findById(client.getUser().getId());
                if (user == null)
                    throw new Exception("was not able to load user with id(" + client.getUser().getId() + ") - NOT FOUND");
                boolean userIsPartyAdmin = Helper.userIsAdminOnParty(user, partyId);
                boolean userIsPartyReviewer = Helper.userIsReviewerOnParty(user, partyResponse.getId());

                log.debug("is User(" + user.getId() + ") isPartyAdmin(" + userIsPartyAdmin + ") isPartyReviewer(" + userIsPartyReviewer + ")");

                // update activity on user
                if (!user.wasUserActiveInLastMinutes(1)) {
                    log.debug("Updating ActivityTS of user(" + user.getId() + ")");
                    userService.updateActivity(user);
                } else {
                    log.debug("user was active within last minute - no need to update last acivity TS");
                }

                List<RequestVm> requests = requestService.getAllPartyRequests(partyId);
                List<Notification> notifications = notificationService.getAllNotificationsSince(client.getUser().getId(), partyId, lastTs);
                // TODO Causes Exception see https://github.com/rootzoll/konfetti-app/issues/32
                // notificationService.deleteAllNotificationsOlderThan(client.getUser().getId(), partyId, lastTs);
                if (requests == null) requests = new ArrayList<>();
                if (notifications == null) notifications = new ArrayList<>();

                log.debug("got requests(" + requests.size() + ") & notifications(" + notifications.size() + ")");

                // if not reviewer or admin then return just public and own requests
                if ((!userIsPartyAdmin) && (!userIsPartyReviewer)) {
                    List<RequestVm> filteredRequests = new ArrayList<>();
                    for (RequestVm requestIter : requests) {
                        if ((requestIter.getUserId().equals(user.getId())) || (requestIter.getState().equals(STATE_DONE)) || (requestIter.getState().equals(STATE_PROCESSING)) || (requestIter.getState().equals(STATE_OPEN))) {
                            filteredRequests.add(requestIter);
                        }
                    }
                    requests = filteredRequests;
                    log.debug("after non admin/reviewer filtering --> requests(" + requests.size() + ")");
                }

                partyResponse.setRequests(new HashSet<RequestVm>(requests));
                NotificationMapper notificationMapper = new NotificationMapper();
                HashSet<Notification> foundNotification = new HashSet<>(notifications);
                Set<NotificationDto> notificationDtos = foundNotification.stream().map(notification -> notificationMapper.toNotificationDto(notification)).collect(Collectors.toSet());
                partyResponse.setNotifications(notificationDtos);

                // add accounting info
                log.debug("add accounting info");
                final String userAccountName = AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyId);
                Long userBalance = accountingService.getBalanceOfAccount(userAccountName);
                if (userBalance == null) userBalance = 0L;
                partyResponse.setKonfettiCount(userBalance);

                // set how many konfetti can be send id feature is enabled
                if (partyResponse.getSendKonfettiMode() == SENDKONFETTIMODE_DISABLED) {
                    // is disabled - set to zero
                    partyResponse.setSendKonfettiMaxAmount(0);
                } else if (partyResponse.getSendKonfettiMode() == SENDKONFETTIMODE_ALL) {
                    // all konfetti can be spend
                    partyResponse.setSendKonfettiMaxAmount(partyResponse.getKonfettiCount());
                } else if (partyResponse.getSendKonfettiMode() == SENDKONFETTIMODE_JUSTEARNED) {
                    // just earned konfetti can be spend
                    partyResponse.setSendKonfettiMaxAmount(this.accountingService.getBalanceEarnedOfAccount(userAccountName));
                } else {
                    log.warn("Not implemented KonfettiSendMode of " + partyResponse.getSendKonfettiMode());
                }

                partyResponse.setKonfettiTotal(-1L); // TODO: implement statistic later
                partyResponse.setTopPosition(-1); // TODO: implement statistic later

                // see if there is any new chat message for user TODO: find a more performant way
                log.debug("see if there is any new chat message");
                List<Chat> allPartyChatsUserIsPartOf = chatService.getAllByUserAndParty(client.getUser().getId(), partyId);
                Party party = partyService.findById(partyId);
                for (Chat chat : allPartyChatsUserIsPartOf) {
                    if (!chat.hasUserSeenLatestMessage(client.getUser().getId())) {
                        // create temporary notification (a notification that is not in DB)
                        Notification noti = new Notification();
                        noti.setId(-System.currentTimeMillis());
                        noti.setParty(party);
                        noti.setRef(chat.getRequest().getId());
                        noti.setType(NotificationType.CHAT_NEW);
                        noti.setUser(client.getUser());
                        noti.setTimeStamp(System.currentTimeMillis());

                        // add mapped NotificationDto to partyResponse
                        Set<NotificationDto> notis = partyResponse.getNotifications();
                        notis.add(notificationMapper.toNotificationDto(noti));
                        partyResponse.setNotifications(notis);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Was not able to get optional client info on request for party(" + partyId + "): " + e.getMessage(), e);
        }

        return partyResponse;
    }

    /*
     * just for admin to request all parties - without any geo filtering
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public List<PartyResponse> getAllPartiesAdmin(HttpServletRequest request) throws Exception {
    	
    	log.info("*** GET All Parties (Admin) ***");
    	
        // check admin auth
        controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Get all PARTIES ...");

        // get all parties and map to response
        List<PartyResponse> partyResponses = new ArrayList<>();
        List<Party> allParties = partyService.getAllParties();
        for (Party party : allParties) {
            partyResponses.add(partyMapper.toPartyResponse(party));
        }

        return partyResponses;
    }


    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.GET)
    public List<PartyResponse> getAllParties(
            @RequestParam(value = "lat", defaultValue = "0.0") String latStr,
            @RequestParam(value = "lon", defaultValue = "0.0") String lonStr,
            HttpServletRequest request) {

    	log.info("*** GET All Parties lat(" + latStr + ") lon(" + lonStr + ") ***");
    
        // TODO: improve later by filter on GPS per search index

        List<Party> foundParties = partyService.findByVisibility(VISIBILITY_PUBLIC);
        List<Party> resultParties = new ArrayList<Party>();

        // TODO: fix this if it works again, at the moment no filtering by geo coordinates, does not work on server


//		if ((latStr.equals("0.0")) && (lonStr.equals("0.0"))) {

        // return all parties when lat & lon not given
        log.info("return all parties");

        resultParties = foundParties;

//		} else {
//
//			// filter parties when in reach of GPS
//
//			double lat = Double.parseDouble(latStr);
//			double lon = Double.parseDouble(lonStr);
//
//			log.info("filter parties on lat(" + lat + ") lon(" + lon + ")");
//
//			for (Party party : allParties) {
//
//				// calc distance in meters (and set on object)
//				double distanceMetersDouble = Helper.distInMeters(lat, lon, party.getLat(), party.getLon());
//				long distanceMetersLong = Math.round(distanceMetersDouble);
//				if (distanceMetersLong > Integer.MAX_VALUE) distanceMetersLong = Integer.MAX_VALUE;
//				int distanceMeters = (int) distanceMetersLong;
//
//				log.info("party(" + party.getId() + ") with meterrange(" + party.getMeters() + ") has distance to user of meters(" + distanceMeters + ")");
//
//				// check if user GPS is within party area or party is global
//				log.warn("TODO: Fix this geo filter later ... now just show every party");
//				if ((distanceMeters <= party.getMeters()) || (party.getMeters() == 0)) {
//
//					log.info("--> IN");
//
//					// use meters field to set distance for user perspective
//					party.setDistanceToUser(distanceMeters);
//
//					// add to result list
//					resultParties.add(party);
//				} else {
//
//					log.info("--> OUT");
//
//				}
//
//			}
//
//		}

        List<PartyResponse> partyResponses = new ArrayList<>();
        // try to personalize when client/user info is in header

        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(request, clientService);

        if (client != null) {
            // force add parties the user is member of (if not already in list)
            User user = userService.findById(client.getUser().getId());
            if (user != null) {
                if (!user.getActiveParties().isEmpty()) {
                    // TODO: implement
                    log.warn("PartyController getAllParties(): TODO: mustHaveParty to add to partylist");
                }
            }

            // update activity on user
            if (!user.wasUserActiveInLastMinutes(1)) {
                userService.updateActivity(user);
            }


            // for all parties
            for (final Party party : resultParties) {
                PartyResponse partyResponse = partyMapper.toPartyResponse(party);
                final String accountName = AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyResponse.getId());

                // add accounting info
                Account userAccountForParty = accountingService.findAccountByName(accountName);

                // if user is new on party (has no account yet)
                if (userAccountForParty == null) {
                    log.info("New User(" + client.getUser().getId() + ") active on Party(" + partyResponse.getId() + ")");

                    // create account
                    if (!accountingService.createAccount(accountName)) {
                        log.warn("Was not able to create balance account(" + accountName + ")");
                    }

                    // make user member of party
                    if (!user.getActiveParties().contains(party)) {
                        user.getActiveParties().add(party);
                        userService.update(user);
                    }

                    // welcome user
                    if (partyResponse.getWelcomeBalance() > 0) {
                        // transfer welcome konfetti
                        log.info("Transfer Welcome-Konfetti(" + partyResponse.getWelcomeBalance() + ") on Party(" + partyResponse.getId() + ") to User(" + client.getUser().getId() + ") with accountName(" + accountName + ")");
                        Long userBalance = accountingService.addBalanceToAccount(TransactionType.USER_WELCOME, accountName, partyResponse.getWelcomeBalance());
                        log.debug("userBalance(" + userBalance + ")");
                    }
                    // show welcome notification
                    log.info("NOTIFICATION Welcome Paty (" + partyResponse.getId() + ")");
                    notificationManager.sendNotification_PartyWelcome(user, party);
                } else {
                    log.debug("user known on party");
                    partyResponse.setKonfettiCount(userAccountForParty.getBalance());
                }

                // disable statistics in this level
                partyResponse.setKonfettiTotal(-1L);
                partyResponse.setTopPosition(-1);
                partyResponses.add(partyResponse);
            }
        }

        log.info("RESULT number of parties is " + resultParties.size());
        return partyResponses;
    }


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/{langCode}/request", method = RequestMethod.POST)
    public RequestVm createRequest(@PathVariable long partyId, @PathVariable String langCode, @RequestBody @Valid final RequestVm requestVm, HttpServletRequest httpRequest) throws Exception {

    	log.info("*** POST Create Request ***");
    	
        // load party for background info
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party with id(" + partyId + ") not found");

        // get user info
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        if (client == null) throw new Exception("client not found");
        User user = userService.findById(client.getUser().getId());
        if (user == null) throw new Exception("user(" + client.getUser().getId() + ") not found");

        // check if request has minimal konfetti
        if (requestVm.getKonfettiCount() < 0) throw new Exception("invalid konfetti on request");
        if (requestVm.getKonfettiCount() < party.getNewRequestMinKonfetti())
            throw new Exception("not enough konfetti on request - is(" + requestVm.getKonfettiAdd() + ") needed(" + party.getNewRequestMinKonfetti() + ")");

        // check if user has minimal konfetti
        Long userBalance = accountingService.getBalanceOfAccount(AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), party.getId()));
        if (userBalance == null) userBalance = 0L;
        if (userBalance < requestVm.getKonfettiCount())
            throw new Exception("not enough konfetti on userbalance - is(" + userBalance + ") needed(" + requestVm.getKonfettiCount() + ")");

        // write data better set by server
        requestVm.setTime(System.currentTimeMillis());
        requestVm.setUserId(client.getUser().getId());
        requestVm.setPartyId(partyId);

        // set state based on party settings
        if (party.getReviewLevel() == REVIEWLEVEL_NONE) {
            requestVm.setState(STATE_OPEN);
        } else {
            requestVm.setState(STATE_REVIEW);
        }

        // update fields in user and persist
        user.setImageMediaID(requestVm.getImageMediaID());
        user.setName(requestVm.getUserName());
        user.setSpokenLangs(requestVm.getSpokenLangs());
        userService.update(user);

        // title --> multi language
        MultiLang multiLang = autoTranslator.translate(langCode, requestVm.getTitle());
        String json = new ObjectMapper().writeValueAsString(multiLang);
        log.info("request title --autotranslate--> " + json);
        MediaItem mediaItem = new MediaItem();
        mediaItem.setData(json);
        mediaItem.setReviewed(REVIEWED_PUBLIC);
        mediaItem.setType(TYPE_MULTILANG);
        mediaItem.setUserId(client.getUser().getId());
        mediaItem = mediaService.create(mediaItem);
        log.info("multilang stored with id(" + mediaItem.getId() + ")");
        requestVm.setTitleMultiLangRef(mediaItem.getId());

        // check media items on new request
        if (requestVm.getMediaItemIds().length > 0) {
            Long[] mediaItemIds = requestVm.getMediaItemIds();
            for (int i = 0; i < mediaItemIds.length; i++) {
                Long mediaItemId = mediaItemIds[i];
                MediaItem item = mediaService.findById(mediaItemId);
                if (item == null) {
                    requestVm.setMediaItemIds(null);
                    log.error("new request has non existing media items on it - security clearing all mediaitems on request");
                    break;
                }
                if (!item.getUserId().equals(client.getUser().getId())) {
                    requestVm.setMediaItemIds(null);
                    log.error("new request has media items other users on it - security clearing all mediaitems on request");
                    break;
                }
            }
        }

        // create request
        Request persistent = requestService.create(requestMapper.fromRequestVm(requestVm));

        // transfer balance to request account
        accountingService.createAccount(AccountingTools.getAccountNameFromRequest(persistent.getId()));
        if (requestVm.getKonfettiCount() > 0) {
            accountingService.transferBetweenAccounts(TransactionType.TASK_CREATION,
                    AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyId),
                    AccountingTools.getAccountNameFromRequest(persistent.getId()),
                    requestVm.getKonfettiCount());
        }

        this.notificationManager.sendNotification_ReviewWAITING(persistent);

        // publish info about update on public channel
        CommandMessage msg = new CommandMessage();
        msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
        msg.setData("{\"party\":" + persistent.getParty().getId() + ", \"request\":" + persistent.getId() + " ,\"state\":\"" + persistent.getState() + "\"}");
        webSocket.convertAndSend("/out/updates", GSON.toJson(msg));

        return requestMapper.toRequestVm(persistent);
    }

    //---------------------------------------------------
    // REQUEST Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request", method = RequestMethod.PUT)
    public RequestVm updateRequest(@PathVariable long partyId, @RequestBody @Valid RequestVm request, HttpServletRequest httpRequest) throws Exception {
        
    	log.info("*** PUT Update Request ***");
    	
    	log.debug("updateRequest");
        controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        Request updateRequestEntity = requestService.update(requestMapper.fromRequestVm(request));
        RequestVm requestVm = requestMapper.toRequestVm(updateRequestEntity);
        requestVm.setKonfettiAmountReward(request.getKonfettiAmountReward());
        requestVm.setKonfettiAmountSupport(request.getKonfettiAmountSupport());
        requestVm.setKonfettiCount(request.getKonfettiCount());
        requestVm.setKonfettiAdd(request.getKonfettiAdd());
        return requestVm;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request/{requestId}", method = RequestMethod.DELETE)
    public RequestVm deleteRequest(@PathVariable long partyId, @PathVariable long requestId, HttpServletRequest httpRequest) throws Exception {

    	log.info("*** DELETE Request ***");
    	
        // get request that is to be deleted
        Request request = requestService.findById(requestId);
        if (request == null) throw new Exception("no request with id(" + requestId + ") found");

        // check if user is allowed to delete
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {
            // A) client for user (party admin or reeuest author)
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            User user = userService.findById(client.getUser().getId());

            boolean userIsAuthor = (request.getUser().getId().equals(client.getUser().getId()));
            boolean userIsPartyAdmin = Helper.userIsAdminOnParty(user, request.getParty().getId());
            log.info("delete request(" + requestId + ") ... client is author(" + userIsAuthor + ") partyAdmin(" + userIsPartyAdmin + ")");

            if ((!userIsAuthor) && (!userIsPartyAdmin))
                throw new Exception("cannot delete request(" + requestId + ") - user is not request author or party admin");
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        }
        
        // delete
        Request result = requestService.delete(request.getId());
        RequestVm requestVm = requestMapper.toRequestVm(request);

        // payback of upvote konfetti when request is still open
        if (!STATE_DONE.equals(request.getState())) {
            List<KonfettiTransaction> allPayIns = konfettiTransactionService.getAllTransactionsToAccount(AccountingTools.getAccountNameFromRequest(requestId));
            for (KonfettiTransaction payIn : allPayIns) {
                if ((payIn.getType() == TransactionType.TASK_SUPPORT) && (!AccountingTools.getAccountNameFromUserAndParty(request.getUser().getId(), request.getParty().getId()).equals(payIn.getFromAccount()))) {
                    // make payback
                    accountingService.transferBetweenAccounts(TransactionType.TASK_SUPPORT, AccountingTools.getAccountNameFromRequest(requestId), payIn.getFromAccount(), payIn.getAmount());
                    Long userIdFromAccountName = AccountingTools.getUserIdFromAccountName(payIn.getFromAccount());
                    User user = userService.findById(userIdFromAccountName);
                    Long partyIdFromAccountName = AccountingTools.getPartyIdFromAccountName(payIn.getFromAccount());
                    Party party = partyService.findById(partyIdFromAccountName);
                    this.notificationManager.sendNotification_VotePAYBACK(user, result, payIn.getAmount());
                }
            }
        }
        return requestVm;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request/{requestId}", method = RequestMethod.GET)
    public ResponseEntity<RequestVm> getRequest(@PathVariable long partyId, @PathVariable long requestId, @RequestParam(value = "upvoteAmount", defaultValue = "0") Long upvoteAmount, HttpServletRequest httpRequest) throws Exception {

    	log.info("*** GET Request ***");
    	
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        log.info("PartyController getRequest(" + requestId + ") upvoteAmount(" + upvoteAmount + ") ...");

        Request requestEntity = requestService.findById(requestId);

        if (requestEntity == null) {
            log.warn("PartyController getRequest(" + requestId + ") --> NULL");
            throw new Exception("was not able to load request with id(" + requestId + ") - NOT FOUND");
        }

        RequestVm requestReponse = requestMapper.toRequestVm(requestEntity);

        User user = userService.findById(client.getUser().getId());
        boolean userIsPartyAdmin = Helper.userIsAdminOnParty(user, requestReponse.getPartyId());

        // add chats to request (when user is host or member)
        List<Chat> chats = this.chatService.getAllByRequestId(requestReponse.getId());
        if (chats == null) chats = new ArrayList<Chat>();
        List<ChatDto> relevantChats = new ArrayList<ChatDto>();
        ChatDto chatDto = new ChatDto();
        for (Chat chat : chats) {
            if (!chat.chatContainsMessages()) continue;
            chatDto = chatMapper.toChatDto(chat);
            if (chatDto.getHostId().equals(client.getUser().getId())) {
                chatDto = ChatController.setChatPartnerInfoOn(userService, chatDto, chat.getMembers()[0], client.getUser().getId());
                relevantChats.add(chatDto);
            } else if (Helper.contains(chat.getMembers(), client.getUser().getId())) {
                chatDto = ChatController.setChatPartnerInfoOn(userService, chatDto, chat.getHostId(), client.getUser().getId());
                relevantChats.add(chatDto);
            } else if (userIsPartyAdmin) {
                chatDto = ChatController.setChatPartnerInfoOn(userService, chatDto, chat.getMembers()[0], client.getUser().getId());
                relevantChats.add(chatDto);
            }
        }

        requestReponse.setChats(relevantChats);

        // add media items to request
        List<MediaItem> infos = null;
        if (infos == null) infos = new ArrayList<MediaItem>();
        Long[] mediaIDs = requestReponse.getMediaItemIds();
        if ((mediaIDs != null) && (mediaIDs.length > 0)) {
            for (Long mediaID : mediaIDs) {
                if (mediaID != null) {
                    MediaItem item = mediaService.findById(mediaID);
                    infos.add(item);
                }
            }
        }
        requestReponse.setInfo(infos);

        // get multi language media item
        if (requestReponse.getTitleMultiLangRef() != null) {
            requestReponse.setTitleMultiLang(mediaRepository.findOne(requestReponse.getTitleMultiLangRef()));
        }


        // add info about support to the request from this user
        long konfettiAmountSupport = 0L;
        List<KonfettiTransaction> allTransactionsToRequest = konfettiTransactionService.getAllTransactionsToAccountSinceTS(AccountingTools.getAccountNameFromRequest(requestReponse.getId()), requestReponse.getTime());
        for (KonfettiTransaction konfettiTransaction : allTransactionsToRequest) {
            if (AccountingTools.getUserIdFromAccountName(konfettiTransaction.getFromAccount()).equals(user.getId())) {
                konfettiAmountSupport += konfettiTransaction.getAmount();
            }
        }
        requestReponse.setKonfettiAmountSupport(konfettiAmountSupport);

        // add info about rewards from the request to user
        long konfettiAmountReward = 0L;
        if (requestReponse.getState().equals(STATE_DONE)) {
            String accountName = AccountingTools.getAccountNameFromRequest(requestReponse.getId());
            List<KonfettiTransaction> allTransactionsFromRequest = konfettiTransactionService.getAllTransactionsFromAccountSinceTS(accountName, requestReponse.getTime());
            for (KonfettiTransaction konfettiTransaction : allTransactionsFromRequest) {
                if (konfettiTransaction.getType() != TransactionType.TASK_REWARD) continue;
                if (konfettiTransaction.getFromAccount() == null) {
                    log.warn("NULL fromAdress on transaction(" + konfettiTransaction.getId() + ") on request(" + requestReponse.getId() + ") ... why?!?");
                    continue;
                }
                if (user.getId().equals(AccountingTools.getUserIdFromAccountName(konfettiTransaction.getToAccount()))) {
                    konfettiAmountReward += konfettiTransaction.getAmount();
                }
            }
        }
        requestReponse.setKonfettiAmountReward(konfettiAmountReward);

        // UPVOTE (optional when request parameter set)
        if (upvoteAmount > 0L) {
            log.info("Upvoting request(" + requestId + ") with amount(" + upvoteAmount + ") ...");
            // check if user has enough balance
            String userAccountname = AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyId);
            Long userBalance = accountingService.getBalanceOfAccount(userAccountname);
            if (userBalance == null)
                throw new Exception("not able to get account balance of account(" + userAccountname + ")");
            if (userBalance < upvoteAmount)
                throw new Exception("user(" + client.getId() + ") has not enough balance to upvote on party(" + partyId + ") - is(" + userBalance + ") needed(" + upvoteAmount + ")");

            // transfer amount
            if (!accountingService.transferBetweenAccounts(TransactionType.TASK_SUPPORT, userAccountname, AccountingTools.getAccountNameFromRequest(requestId), upvoteAmount)) {
                throw new Exception("was not able to transfer upvote amount(" + upvoteAmount + ") from(" + userAccountname + ") to(" + AccountingTools.getAccountNameFromRequest(requestId) + ")");
            }
            log.info("... OK: transfer of upvote amount(" + upvoteAmount + ") from(" + userAccountname + ") to(" + AccountingTools.getAccountNameFromRequest(requestId) + ") done.");
        } else {
            log.info("no Upvoting - amount(" + upvoteAmount + ")");
        }

        // add account balance to request object
        requestReponse.setKonfettiCount(accountingService.getBalanceOfAccount(AccountingTools.getAccountNameFromRequest(requestId)));

        // publish info about update on public channel
        CommandMessage msg = new CommandMessage();
        msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
        msg.setData("{\"party\":" + requestReponse.getPartyId() + ", \"request\":" + requestReponse.getId() + " ,\"state\":\"" + requestReponse.getState() + "\", \"konfetti\":" + requestReponse.getKonfettiCount() + "}");
        webSocket.convertAndSend("/out/updates", GSON.toJson(msg));


        return new ResponseEntity<>(requestReponse, HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/request/{requestId}", method = RequestMethod.GET)
    public RequestVm actionRequest(@PathVariable long requestId, @RequestParam(value = "action", defaultValue = "no") String action, @RequestParam(value = "json", defaultValue = "") String json, HttpServletRequest httpRequest) throws Exception {

    	log.info("*** GET Action on Request("+requestId+") action("+action+") ***");
    	
        Request request = requestService.findById(requestId);
        if (request != null) {
            if (action.equals("no")) throw new Exception("missing parameter action");
            
            // check if user is allowed to work on request
            boolean userIsAuthor = false;
            boolean userIsPartyAdmin = false;
            boolean userIsPartyReviewer = false;
            if (httpRequest.getHeader("X-CLIENT-ID") != null) {
                // A) client for user (party admin, reviewer or request author)

                Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
                User user = userService.findById(client.getUser().getId());

                userIsAuthor = (request.getUser().getId().equals(client.getUser().getId()));
                userIsPartyAdmin = Helper.userIsAdminOnParty(user, request.getParty().getId());
                userIsPartyReviewer = Helper.userIsReviewerOnParty(user, request.getParty().getId());
                log.info("action request(" + requestId + ") ... client is author(" + userIsAuthor + ") partyAdmin(" + userIsPartyAdmin + ") partyReview(" + userIsPartyReviewer + ")");

                if ((!userIsAuthor) && (!userIsPartyAdmin) && (!userIsPartyReviewer))
                    throw new Exception("cannot action request(" + requestId + ") - user is not request author or party admin or reviewer");
            } else {
                // B) check for trusted application with administrator privilege
                controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
                userIsPartyAdmin = true;
            }

            // Actions
            // open from review (by admin and reviewer)
            if (action.equals(STATE_OPEN.toString())) {
                // check if pre-state is valid
                boolean fromReview = request.getState().equals(STATE_REVIEW);
                boolean fromProcessing = request.getState().equals(STATE_PROCESSING);
                if ((!fromReview) && (!fromProcessing))
                    throw new Exception("request(" + requestId + ") with state(" + request.getState() + ") CANNOT set to '" + STATE_OPEN + "'");

                // check if admin or reviewer
                if ((!userIsPartyAdmin) && (!userIsPartyReviewer)) {
                    // if is author unpausing an request
                    if ((!userIsAuthor) || (!request.getState().equals(STATE_PROCESSING)))
                        throw new Exception("request(" + requestId + ") author cannot set to open");
                }

                // set open & persists
                request.setState(STATE_OPEN);
                requestService.update(request);
                log.info("request(" + requestId + ") set STATE to " + STATE_OPEN);

                if (fromReview) {
                    // send notification to author
                	log.info("set to open - from review (before)");
                	notificationManager.sendNotification_ReviewOK(request);
                	log.info("set to open - from review (after)");
                } else {
                	log.info("set to open - not from review");
                }

                // publish info about update on public channel
            	log.info("publish info about update on public channel");
                CommandMessage msg = new CommandMessage();
                msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
                msg.setData("{\"party\":" + request.getParty().getId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
                webSocket.convertAndSend("/out/updates", GSON.toJson(msg));
            } else
                // set processing (by all)
                if (action.equals(STATE_PROCESSING.toString())) {
                    // check if pre-state is valid
                    if (!request.getState().equals(STATE_OPEN))
                        throw new Exception("request(" + requestId + ") with state(" + request.getState() + ") CANNOT set to '" + STATE_PROCESSING + "'");

                    // set processing & persists
                    request.setState(STATE_PROCESSING);
                    requestService.update(request);
                    log.info("request(" + requestId + ") set STATE to " + STATE_PROCESSING);
                } else
                    // set rejected (by admin and reviewer)
                    if (action.equals(STATE_REJECTED.toString())) {
                        // check if admin or reviewer
                        if ((!userIsPartyAdmin) && (!userIsPartyReviewer))
                            throw new Exception("request(" + requestId + ") author cannot set to rejected");

                        // set processing & persists
                        request.setState(STATE_REJECTED);
                        requestService.update(request);
                        log.info("request(" + requestId + ") set STATE to " + STATE_REJECTED);

                        // publish info about update on public channel
                        CommandMessage msg = new CommandMessage();
                        msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
                        msg.setData("{\"party\":" + request.getParty().getId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
                        webSocket.convertAndSend("/out/updates", GSON.toJson(msg));
                        notificationManager.sendNotification_ReviewFAIL(request);
                        
                    } else
                        // do reward
                        if (action.equals("reward")) {
                            // needed json data
                            if ((json == null) || (json.length() == 0)) throw new Exception("minning parameter json");
                            List<Long> ids = new ArrayList<Long>();
                            try {
                                List<Integer> idsInts = (new ObjectMapper()).readValue(json, ids.getClass());
                                for (int i = 0; i < idsInts.size(); ++i) {
                                    ids.add(idsInts.get(i).longValue());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new Exception("json paramter not valid");
                            }
                            if (ids.isEmpty()) throw new Exception("json(" + json + ") is empty list if ids");
                            if (ids.get(0) == null)
                                throw new Exception("json(" + json + ") contains just NULL and no list if ids");

                            // check if admin or reviewer
                            if ((!userIsPartyAdmin) && (!userIsAuthor))
                                throw new Exception("request(" + requestId + ") author cannot set to rejected");

                            // TODO: check if ids have chats on request

                            // get reward balance
                            final String requestAccountName = AccountingTools.getAccountNameFromRequest(request.getId());
                            Long requestBalance = accountingService.getBalanceOfAccount(requestAccountName);
                            if ((requestBalance < ids.size()) && (requestBalance > 0))
                                throw new Exception("there are more rewardees than reward - not possible");

                            // split reward
                            Long rewardPerPerson = 0L;
                            if (requestBalance > 0) {

                                rewardPerPerson = (long) Math.floor((requestBalance * 1d) / (ids.size() * 1d));
                                if (((rewardPerPerson * ids.size()) > requestBalance) || (rewardPerPerson <= 0))
                                    throw new Exception("reward(" + requestBalance + ") is not splitting up correctly to " + ids.size() + " --> " + rewardPerPerson);

                                // transfer reward to users
                                for (Long userRewardId : ids) {
                                    log.info("making transfere reward to userId(" + userRewardId + ") ...");
                                    if (userRewardId == null) {
                                        log.warn("skipping a NULL rewardId");
                                        continue;
                                    }
                                    if (userRewardId.equals(request.getUser().getId())) {
                                        log.warn("ignoring the author self-rewrad");
                                        continue;
                                    }
                                    final String rewardeeAccountName = AccountingTools.getAccountNameFromUserAndParty(userRewardId, request.getParty().getId());
                                    if (!accountingService.transferBetweenAccounts(TransactionType.TASK_REWARD, requestAccountName, rewardeeAccountName, rewardPerPerson)) {
                                        log.error("FAIL payout reward(" + rewardPerPerson + ") from(" + requestAccountName + ") to " + rewardeeAccountName);
                                    } else {
                                        log.info("OK payout reward(" + rewardPerPerson + ") from(" + requestAccountName + ") to " + rewardeeAccountName);
                                        // send notification to author
                                        User userReward = userService.findById(userRewardId);
                                        notificationManager.sendNotification_TaskREWARD(userReward, request, rewardPerPerson);
                                    }
                                }
                                // notification to all supporters of request about finish
                                List<KonfettiTransaction> allPayIns = konfettiTransactionService.getAllTransactionsToAccount(AccountingTools.getAccountNameFromRequest(requestId));
                                for (KonfettiTransaction payIn : allPayIns) {
                                    if ((payIn.getType() == TransactionType.TASK_SUPPORT) && (!AccountingTools.getAccountNameFromUserAndParty(request.getUser().getId(), request.getParty().getId()).equals(payIn.getFromAccount()))) {
                                        Long userIdFromAccountName = AccountingTools.getUserIdFromAccountName(payIn.getFromAccount());
                                        User user = userService.findById(userIdFromAccountName);
                                        Long partyIdFromAccountName = AccountingTools.getPartyIdFromAccountName(payIn.getFromAccount());
                                        Party party = partyService.findById(partyIdFromAccountName);
                                        notificationManager.sendNotification_VoteDONE(user, request);
                                    }
                                }
                            }

                            // set processing & persists
                            request.setState(STATE_DONE);
                            request = requestService.update(request);
                            log.info("request(" + requestId + ") set STATE to " + STATE_DONE);

                            // publish info about update on public channel
                            CommandMessage msg = new CommandMessage();
                            msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
                            msg.setData("{\"party\":" + request.getParty().getId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
                            webSocket.convertAndSend("/out/updates", GSON.toJson(msg));
                        } else
                            // mute chat on request
                            if (action.equals("muteChat")) {
                                // needed json data
                                if ((json == null) || (json.length() == 0))
                                    throw new Exception("minning parameter json");
                                Long chatId = 0L;
                                try {
                                    chatId = (new ObjectMapper()).readValue(json, chatId.getClass());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new Exception("json paramter not valid");
                                }

                                // try load chat
                                Chat chat = chatService.findById(chatId);
                                if (chat == null) throw new Exception("chat(" + chatId + ") not found");
                                if (!chat.getRequest().getId().equals(request.getId()))
                                    throw new Exception("chat(" + chatId + ") not on request(" + requestId + ")");

                                // check if admin or author
                                if ((!userIsPartyAdmin) && (!userIsAuthor))
                                    throw new Exception("request(" + requestId + ") not allowed to mute chat(" + chatId + ")");

                                // mut chat & persists
                                chat.setMuted(true);
                                chatService.update(chat);
                                log.info("chat(" + chatId + ") on request(" + requestId + ") muted ");

                                // TODO Implement send notification to muted chat user or add info as chat message
                                log.warn("TODO: Implement send notification to muted chat user or add info as chat message");
                            } else
                                // delete media item from request
                                if (action.equals("deleteMedia")) {
                                    // needed json data --> the id of the media item to add
                                    if ((json == null) || (json.length() == 0))
                                        throw new Exception("missing parameter json");
                                    Long mediaId = 0L;
                                    try {
                                        mediaId = (new ObjectMapper()).readValue(json, mediaId.getClass());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        throw new Exception("json paramter not valid");
                                    }
                                    // check if media item exists
                                    MediaItem item = mediaService.findById(mediaId);
                                    if (item == null) throw new Exception("media(" + mediaId + ") not found");

                                    // check if admin or author
                                    if ((!userIsPartyAdmin) && (!userIsAuthor) && (!userIsPartyReviewer))
                                        throw new Exception("request(" + requestId + ") not allowed to remove media(" + mediaId + ")");

                                    // remove media
                                    request.setMediaItemIds(Helper.remove(request.getMediaItemIds(), mediaId));
                                    request = requestService.update(request);
                                    log.info("mediaItem(" + mediaId + ") removed from request(" + requestId + ")");
                                } else
                                    // add media item to request
                                    if (action.equals("addMedia")) {
                                        // needed json data --> the id of the media item to add
                                        if ((json == null) || (json.length() == 0))
                                            throw new Exception("missing parameter json");
                                        Long mediaId = 0L;
                                        try {
                                            mediaId = (new ObjectMapper()).readValue(json, mediaId.getClass());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            throw new Exception("json paramter not valid");
                                        }

                                        // check if media item exists
                                        MediaItem item = mediaService.findById(mediaId);
                                        if (item == null) throw new Exception("media(" + mediaId + ") not found");

                                        // check if admin or author
                                        if ((!userIsPartyAdmin) && (!userIsAuthor))
                                            throw new Exception("request(" + requestId + ") not allowed to ad media(" + mediaId + ")");

                                        // add media to request
                                        Long[] itemIds = request.getMediaItemIds();
                                        itemIds = Helper.append(itemIds, mediaId);
                                        request.setMediaItemIds(itemIds);
                                        request = requestService.update(request);
                                        log.info("mediaItem(" + mediaId + ") add to request(" + requestId + ")");

                                        // TODO Implement send notification to reviewer if media item still needs review
                                        log.warn("TODO: Implement send notification to reviewer if media item still needs review");
                                    } else
                                        // make a media item public (set as reviewed)
                                        if (action.equals("publicMedia")) {
                                            // needed json data --> the id of the media item to add
                                            if ((json == null) || (json.length() == 0))
                                                throw new Exception("missing parameter json");
                                            Long mediaId = 0L;
                                            try {
                                                mediaId = (new ObjectMapper()).readValue(json, mediaId.getClass());
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                throw new Exception("json paramter not valid");
                                            }

                                            // check if media item exists
                                            MediaItem item = mediaService.findById(mediaId);
                                            if (item == null) throw new Exception("media(" + mediaId + ") not found");

                                            // check if request contains this media item
                                            if (!Helper.contains(request.getMediaItemIds(), mediaId))
                                                throw new Exception("mediaItem(" + mediaId + ") is not part of request(" + requestId + ")");

                                            // check if admin or reviewer
                                            if ((!userIsPartyAdmin) && (!userIsPartyReviewer))
                                                throw new Exception("request(" + requestId + ") not allowed to remove media(" + mediaId + ")");

                                            // set media public
                                            item.setReviewed(REVIEWED_PUBLIC);
                                            mediaService.update(item);
                                            log.info("mediaItem(" + mediaId + ") is now public");
                                        } else {
                                            // unkown action
                                            throw new Exception("unkown action(" + action + ") on request(" + requestId + ")");
                                        }
        } else {
            log.warn("PartyController getRequest(" + requestId + ") --> NULL");
        }

        return requestMapper.toRequestVm(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request", method = RequestMethod.GET)
    public List<RequestVm> getAllPartyRequests(@PathVariable long partyId) throws Exception {
    	
    	log.info("*** GET All Requests on Party ***");
    	
        return requestService.getAllPartyRequests(partyId);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/testData", method = RequestMethod.GET)
    public List<Party> testData() throws Exception {
    	
    	log.info("*** GET Test Data ***");
    	
        List<Party> foundParties = partyService.findByName("Helferverein Nord e.V.");
        if (CollectionUtils.isEmpty(foundParties)) {
            log.debug("Creating Test Parties : Creating test parties..");

            Party partyOne = new Party();
            partyOne.setName("Helferverein Nord e.V.");
            partyOne.setContact("http://pankowhilft.blogsport.de");
            partyOne.setDetailText("Berliner Str. 99, 13189 Berlin, GERMANY");
            partyOne.setLat(Float.parseFloat("52.52"));
            partyOne.setLon(Float.parseFloat("13.13"));
            partyOne.setMeters(0);
            partyOne.setVisibility(VISIBILITY_PUBLIC);
            partyOne.setReviewLevel(REVIEWLEVEL_NONE);
            partyOne.setNewRequestMinKonfetti(1);
            partyOne.setWelcomeBalance(Long.parseLong("100"));
            partyOne.setSendKonfettiMode(SENDKONFETTIMODE_ALL);
            Party partyOnePersisted = partyService.create(partyOne);

            Party partyTwo = new Party();
            partyTwo.setName("Helferverein Süd e.V.");
            partyTwo.setContact("http://muenchen.blogsport.de");
            partyTwo.setDetailText("Antonplatz 3, 89282 München, GERMANY");
            partyTwo.setLat(Float.parseFloat("52.52"));
            partyTwo.setLon(Float.parseFloat("13.13"));
            partyTwo.setMeters(0);
            partyTwo.setVisibility(VISIBILITY_PUBLIC);
            partyTwo.setReviewLevel(REVIEWLEVEL_NONE);
            partyTwo.setNewRequestMinKonfetti(10);
            partyTwo.setWelcomeBalance(Long.parseLong("10"));
            partyTwo.setSendKonfettiMode(SENDKONFETTIMODE_ALL);
            Party partyTwoPersisted = partyService.create(partyTwo);

            return Arrays.asList(partyOnePersisted, partyTwoPersisted);
        }
        log.debug("Creating Test Parties : Parties exist already, doing nothing..");
        return null;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ex.getMessage();
    }

    public class DashBoardInfo {
        public Long numberOfKonfetti = -1L;
        public Long numberOfUsers = -1L;
        public Long numberOfTasks = -1L;
        public Long numberOfParties = -1L;
    }

}
