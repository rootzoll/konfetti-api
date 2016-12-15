package de.konfetti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.konfetti.controller.mapper.PartyMapper;
import de.konfetti.controller.vm.PartyResponse;
import de.konfetti.data.*;
import de.konfetti.data.mediaitem.MultiLang;
import de.konfetti.service.*;
import de.konfetti.utils.AccountingTools;
import de.konfetti.utils.AutoTranslator;
import de.konfetti.utils.Helper;
import de.konfetti.websocket.CommandMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

import static de.konfetti.data.NotificationType.*;
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
        controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Creating PARTY(" + partyResponse.getId() + ")");
        Party createdParty = partyService.create(partyMapper.fromPartyResponseToParty(partyResponse));
        return partyMapper.fromPartyToPartyResponse(createdParty);
    }

    //---------------------------------------------------
    // PARTY Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.PUT)
    public PartyResponse updateParty(@RequestBody @Valid final PartyResponse partyResponse, HttpServletRequest request) throws Exception {
        controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Updating PARTY(" + partyResponse.getId() + ")");
        Party party = partyMapper.fromPartyResponseToParty(partyResponse);
        Party updatedParty = partyService.update(party);
        return partyMapper.fromPartyToPartyResponse(updatedParty);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}", method = RequestMethod.DELETE)
    public boolean deleteParty(@PathVariable long partyId, HttpServletRequest request) throws Exception {
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

        PartyResponse partyResponse = partyMapper.fromPartyToPartyResponse(partyService.findById(partyId));
        if (partyResponse == null) throw new Exception("was not able to load party with id(" + partyId + ") - NOT FOUND");


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
                    user.setLastActivityTS(System.currentTimeMillis());
                    userService.update(user);
                } else {
                    log.debug("user was active within last minute - no need to update last acivity TS");
                }

                List<Request> requests = requestService.getAllPartyRequests(partyId);
                List<Notification> notifications = notificationService.getAllNotificationsSince(client.getUser().getId(), partyId, lastTs);
                // TODO Causes Exception see https://github.com/rootzoll/konfetti-app/issues/32
                // notificationService.deleteAllNotificationsOlderThan(client.getUser().getId(), partyId, lastTs);
                if (requests == null) requests = new ArrayList<>();
                if (notifications == null) notifications = new ArrayList<>();

                log.debug("got requests(" + requests.size() + ") & notifications(" + notifications.size() + ")");

                // if not reviewer or admin then return just public and own requests
                if ((!userIsPartyAdmin) && (!userIsPartyReviewer)) {
                    List<Request> filteredRequests = new ArrayList<>();
                    for (Request r : requests) {
                        if ((r.getUserId().equals(user.getId())) || (r.getState().equals(STATE_DONE)) || (r.getState().equals(STATE_PROCESSING)) || (r.getState().equals(STATE_OPEN))) {
                            filteredRequests.add(r);
                        }
                    }
                    requests = filteredRequests;
                    log.debug("after non admin/reviewer filtering --> requests(" + requests.size() + ")");
                }

                partyResponse.setRequests(new HashSet<Request>(requests));
                partyResponse.setNotifications(new HashSet<Notification>(notifications));

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

                partyResponse.setKonfettiTotal(-1l); // TODO: implement statistic later
                partyResponse.setTopPosition(-1); // TODO: implement statistic later

                // see if there is any new chat message for user TODO: find a more performat way
                log.debug("see if there is any new chat message");
                List<Chat> allPartyChatsUserIsPartOf = chatService.getAllByUserAndParty(client.getUser().getId(), partyId);
                for (Chat chat : allPartyChatsUserIsPartOf) {
                    if (!chat.hasUserSeenLatestMessage(client.getUser().getId())) {
                        // create temporary notification (a notification that is not in DB)
                        Notification noti = new Notification();
                        noti.setId(-System.currentTimeMillis());
                        noti.setPartyId(partyId);
                        noti.setRef(chat.getRequestId());
                        noti.setType(NotificationType.CHAT_NEW);
                        noti.setUserId(client.getUser().getId());
                        noti.setTimeStamp(System.currentTimeMillis());
                        Set<Notification> notis = partyResponse.getNotifications();
                        notis.add(noti);
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
  
    	// check admin auth
        controllerSecurityHelper.checkAdminLevelSecurity(request);
        log.info("ADMIN: Get all PARTIES ...");
    	
        // get all parties and map to response
        List<PartyResponse> partyResponses = new ArrayList<>();
        List<Party> allParties = partyService.getAllParties();
        for (Party party : allParties) {
        	partyResponses.add(partyMapper.fromPartyToPartyResponse(party));
		}
        
        return partyResponses;
    };		
    		

    @CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.GET)
    public List<PartyResponse> getAllParties(
            @RequestParam(value = "lat", defaultValue = "0.0") String latStr,
            @RequestParam(value = "lon", defaultValue = "0.0") String lonStr,
            HttpServletRequest request) throws Exception {

        log.info("getAllParties lat(" + latStr + ") lon(" + lonStr + ")");

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
        try {

            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(request, clientService);

            if (client != null) {

                // force add parties the user is member of (if not already in list)
                User user = userService.findById(client.getUser().getId());
                if (user != null) {
                    if (user.getActiveParties().size() > 0) {
                        // TODO: implement
                        log.warn("PartyController getAllParties(): TODO: mustHaveParty to add to partylist");
                    }
                }

                // update activity on user
                if (!user.wasUserActiveInLastMinutes(1)) {
                    log.info("Updating ActivityTS of user(" + user.getId() + ")");
                    user.setLastActivityTS(System.currentTimeMillis());
                    userService.update(user);
                }


                // for all parties
                for (final Party party : resultParties) {
                    PartyResponse partyResponse = partyMapper.fromPartyToPartyResponse(party);
                    final String accountName = AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyResponse.getId());

                    // add accounting info
                    Long userBalance = accountingService.getBalanceOfAccount(accountName);

                    // if user is new on party (has no account yet)
                    if (userBalance == null) {
                        userBalance = 0L;
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
                            userBalance = accountingService.addBalanceToAccount(TransactionType.USER_WELCOME, accountName, partyResponse.getWelcomeBalance());
                        }
                        // show welcome notification
                        log.info("NOTIFICATION Welcome Paty (" + partyResponse.getId() + ")");
                        notificationService.create(NotificationType.PARTY_WELCOME, user.getId(), partyResponse.getId(), 0L);

                        log.debug("userBalance(" + userBalance + ")");
                    } else {
                        log.debug("user known on party");
                    }
                    partyResponse.setKonfettiCount(userBalance);

                    // disable statistics in this level
                    partyResponse.setKonfettiTotal(-1L);
                    partyResponse.setTopPosition(-1);
                    partyResponses.add(partyResponse);
                }
            }
        } catch (Exception e) {
            // exception can be ignored - because its just optional
            log.info("Was not able to get optional client info on request for party list: " + e.getMessage());
        }
        log.info("RESULT number of parties is " + resultParties.size());
        return partyResponses;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/notification/{notiId}", method = RequestMethod.GET)
    public Notification getNotification(@PathVariable long partyId, @PathVariable long notiId, @RequestParam(value = "action", defaultValue = "no") String action, HttpServletRequest httpRequest) throws Exception {
        log.info("PartyController getNotification(" + notiId + ") action(" + action + ") ...");

        // get notification
        Notification noti = notificationService.findById(notiId);
        if (noti == null) throw new Exception("notification(" + notiId + ") not found");

        // check if user is allowed to work on notification
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {
            // A) check if user is owner of notification
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            boolean userIsOwner = (noti.getUserId().equals(client.getUser().getId()));
            if (!userIsOwner)
                throw new Exception("cannot action notification(" + notiId + ") - user is not noti owner / client.userID(" + client.getUser().getId() + ") != notiUserId(" + noti.getUserId() + ")");
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        }

        // Action
        if (action.equals("delete")) {
            if (notiId >= 0L) {
                notificationService.delete(notiId);
                log.info("Notification(" + notiId + ") DELETED");
            } else {
                log.warn("Client should not try to delete temporaray notifications with id<0");
            }
        }
        return noti;
    }

    //---------------------------------------------------
    // NOTIFICATION Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/{langCode}/request", method = RequestMethod.POST)
    public Request createRequest(@PathVariable long partyId, @PathVariable String langCode, @RequestBody @Valid final Request request, HttpServletRequest httpRequest) throws Exception {

        // load party for background info
        Party party = partyService.findById(partyId);
        if (party == null) throw new Exception("party with id(" + partyId + ") not found");

        // get user info
        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        if (client == null) throw new Exception("client not found");
        User user = userService.findById(client.getUser().getId());
        if (user == null) throw new Exception("user(" + client.getUser().getId() + ") not found");

        // check if request has minimal konfetti
        if (request.getKonfettiCount() < 0) throw new Exception("invalid konfetti on request");
        if (request.getKonfettiCount() < party.getNewRequestMinKonfetti())
            throw new Exception("not enough konfetti on request - is(" + request.getKonfettiAdd() + ") needed(" + party.getNewRequestMinKonfetti() + ")");

        // check if user has minimal konfetti
        Long userBalance = accountingService.getBalanceOfAccount(AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), party.getId()));
        if (userBalance == null) userBalance = 0L;
        if (userBalance < request.getKonfettiCount())
            throw new Exception("not enough konfetti on userbalance - is(" + userBalance + ") needed(" + request.getKonfettiCount() + ")");

        // write data better set by server
        request.setTime(System.currentTimeMillis());
        request.setUserId(client.getUser().getId());
        request.setPartyId(partyId);

        // set state based on party settings
        if (party.getReviewLevel() == REVIEWLEVEL_NONE) {
            request.setState(STATE_OPEN);
        } else {
            request.setState(STATE_REVIEW);
            // TODO push info to review admin
            if (user.getPushActive()) log.warn("TODO: push info to review admin");
        }

        // update fields in user and persist
        user.setImageMediaID(request.getImageMediaID());
        user.setName(request.getUserName());
        user.setSpokenLangs(request.getSpokenLangs());
        userService.update(user);

        // title --> multi language
        MultiLang multiLang = autoTranslator.translate(langCode, request.getTitle());
        String json = new ObjectMapper().writeValueAsString(multiLang);
        log.info("request title --autotranslate--> " + json);
        MediaItem mediaItem = new MediaItem();
        mediaItem.setData(json);
        mediaItem.setReviewed(REVIEWED_PUBLIC);
        mediaItem.setType(TYPE_MULTILANG);
        mediaItem.setUserId(client.getUser().getId());
        mediaItem = mediaService.create(mediaItem);
        log.info("multilang stored with id(" + mediaItem.getId() + ")");
        request.setTitleMultiLangRef(mediaItem.getId());

        // check media items on new request
        if (request.getMediaItemIds().length > 0) {
            Long[] mediaItemIds = request.getMediaItemIds();
            for (int i = 0; i < mediaItemIds.length; i++) {
                Long mediaItemId = mediaItemIds[i];
                MediaItem item = mediaService.findById(mediaItemId);
                if (item == null) {
                    request.setMediaItemIds(null);
                    log.error("new request has non existing media items on it - security clearing all mediaitems on request");
                    break;
                }
                if (!item.getUserId().equals(client.getUser().getId())) {
                    request.setMediaItemIds(null);
                    log.error("new request has media items other users on it - security clearing all mediaitems on request");
                    break;
                }
            }
        }
        // create request
        Request persistent = requestService.create(request);

        // transfer balance to request account
        accountingService.createAccount(AccountingTools.getAccountNameFromRequest(persistent.getId()));
        if (request.getKonfettiCount() > 0) {
            accountingService.transferBetweenAccounts(TransactionType.TASK_CREATION, AccountingTools.getAccountNameFromUserAndParty(client.getUser().getId(), partyId), AccountingTools.getAccountNameFromRequest(persistent.getId()), request.getKonfettiCount());
        }

        // store notification
        notificationService.create(REVIEW_WAITING, null, party.getId(), request.getId());

        // publish info about update on public channel
        CommandMessage msg = new CommandMessage();
        msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
        msg.setData("{\"party\":" + persistent.getPartyId() + ", \"request\":" + persistent.getId() + " ,\"state\":\"" + persistent.getState() + "\"}");
        webSocket.convertAndSend("/out/updates", GSON.toJson(msg));

        return persistent;
    }

    //---------------------------------------------------
    // REQUEST Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request", method = RequestMethod.PUT)
    public Request updateRequest(@PathVariable long partyId, @RequestBody @Valid Request request, HttpServletRequest httpRequest) throws Exception {
        controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        return requestService.update(request);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request/{requestId}", method = RequestMethod.DELETE)
    public Request deleteRequest(@PathVariable long partyId, @PathVariable long requestId, HttpServletRequest httpRequest) throws Exception {

        // get request that is to be deleted
        Request request = requestService.findById(requestId);
        if (request == null) throw new Exception("no request with id(" + requestId + ") found");

        // check if user is allowed to delete
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {
            // A) client for user (party admin or reeuest author)
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            User user = userService.findById(client.getUser().getId());

            boolean userIsAuthor = (request.getUserId().equals(client.getUser().getId()));
            boolean userIsPartyAdmin = Helper.userIsAdminOnParty(user, request.getPartyId());
            log.info("delete request(" + requestId + ") ... client is author(" + userIsAuthor + ") partyAdmin(" + userIsPartyAdmin + ")");

            if ((!userIsAuthor) && (!userIsPartyAdmin))
                throw new Exception("cannot delete request(" + requestId + ") - user is not request author or party admin");
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        }
        // delete any waiting notification finding a reviewer
        if (STATE_REVIEW.equals(request.getState()))
            notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());

        // delete
        Request result = requestService.delete(request.getId());

        // payback of upvote konfetti when request is still open
        if (!STATE_DONE.equals(request.getState())) {
            List<KonfettiTransaction> allPayIns = konfettiTransactionService.getAllTransactionsToAccount(AccountingTools.getAccountNameFromRequest(requestId));
            for (KonfettiTransaction payIn : allPayIns) {
                if ((payIn.getType() == TransactionType.TASK_SUPPORT) && (!AccountingTools.getAccountNameFromUserAndParty(request.getUserId(), request.getPartyId()).equals(payIn.getFromAccount()))) {
                    // make payback
                    accountingService.transferBetweenAccounts(TransactionType.TASK_SUPPORT, AccountingTools.getAccountNameFromRequest(requestId), payIn.getFromAccount(), payIn.getAmount());
                    notificationService.create(PAYBACK, AccountingTools.getUserIdFromAccountName(payIn.getFromAccount()), AccountingTools.getPartyIdFromAccountName(payIn.getFromAccount()), payIn.getAmount());
                }
            }
        }
        return result;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request/{requestId}", method = RequestMethod.GET)
    public Request getRequest(@PathVariable long partyId, @PathVariable long requestId, @RequestParam(value = "upvoteAmount", defaultValue = "0") Long upvoteAmount, HttpServletRequest httpRequest) throws Exception {

        Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
        log.info("PartyController getRequest(" + requestId + ") upvoteAmount(" + upvoteAmount + ") ...");

        Request request = requestService.findById(requestId);
        if (request != null) {
            User user = userService.findById(client.getUser().getId());
            boolean userIsPartyAdmin = Helper.userIsAdminOnParty(user, request.getPartyId());

            // add chats to request (when user is host or member)
            List<Chat> chats = this.chatService.getAllByRequestId(request.getId());
            if (chats == null) chats = new ArrayList<Chat>();
            List<Chat> relevantChats = new ArrayList<Chat>();
            for (Chat chat : chats) {
                if (!chat.chatContainsMessages()) continue;
                if (chat.getHostId().equals(client.getUser().getId())) {
                    chat = ChatController.setChatPartnerInfoOn(userService, chat, chat.getMembers()[0], client.getUser().getId());
                    relevantChats.add(chat);
                } else if (Helper.contains(chat.getMembers(), client.getUser().getId())) {
                    chat = ChatController.setChatPartnerInfoOn(userService, chat, chat.getHostId(), client.getUser().getId());
                    relevantChats.add(chat);
                } else if (userIsPartyAdmin) {
                    chat = ChatController.setChatPartnerInfoOn(userService, chat, chat.getMembers()[0], client.getUser().getId());
                    relevantChats.add(chat);
                }
            }
            request.setChats(relevantChats);

            // add media items to request
            List<MediaItem> infos = null;
            if (infos == null) infos = new ArrayList<MediaItem>();
            Long[] mediaIDs = request.getMediaItemIds();
            if ((mediaIDs != null) && (mediaIDs.length > 0)) {
                for (Long mediaID : mediaIDs) {
                    if (mediaID != null) {
                        MediaItem item = mediaService.findById(mediaID);
                        infos.add(item);
                    }
                }
            }
            request.setInfo(infos);

            // get multi language media item
            if (request.getTitleMultiLangRef()!=null) {
                request.setTitleMultiLang(mediaRepository.findOne(request.getTitleMultiLangRef()));
            }


            // add info about support to the request from this user
            long konfettiAmountSupport = 0L;
            List<KonfettiTransaction> allTransactionsToRequest = konfettiTransactionService.getAllTransactionsToAccountSinceTS(AccountingTools.getAccountNameFromRequest(request.getId()), request.getTime());
            for (KonfettiTransaction konfettiTransaction : allTransactionsToRequest) {
                if (AccountingTools.getUserIdFromAccountName(konfettiTransaction.getFromAccount()).equals(user.getId())) {
                    konfettiAmountSupport += konfettiTransaction.getAmount();
                }
            }
            request.setKonfettiAmountSupport(konfettiAmountSupport);

            // add info about rewards from the request to user
            long konfettiAmountReward = 0L;
            if (request.getState().equals(STATE_DONE)) {
                String accountName = AccountingTools.getAccountNameFromRequest(request.getId());
                List<KonfettiTransaction> allTransactionsFromRequest = konfettiTransactionService.getAllTransactionsFromAccountSinceTS(accountName, request.getTime());
                for (KonfettiTransaction konfettiTransaction : allTransactionsFromRequest) {
                    if (konfettiTransaction.getType() != TransactionType.TASK_REWARD) continue;
                    if (konfettiTransaction.getFromAccount() == null) {
                        log.warn("NULL fromAdress on transaction(" + konfettiTransaction.getId() + ") on request(" + request.getId() + ") ... why?!?");
                        continue;
                    }
                    if (user.getId().equals(AccountingTools.getUserIdFromAccountName(konfettiTransaction.getToAccount()))) {
                        konfettiAmountReward += konfettiTransaction.getAmount();
                    }
                }
            }
            request.setKonfettiAmountReward(konfettiAmountReward);

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
            request.setKonfettiCount(accountingService.getBalanceOfAccount(AccountingTools.getAccountNameFromRequest(requestId)));

            // publish info about update on public channel
            CommandMessage msg = new CommandMessage();
            msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
            msg.setData("{\"party\":" + request.getPartyId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\", \"konfetti\":" + request.getKonfettiCount() + "}");
            webSocket.convertAndSend("/out/updates", GSON.toJson(msg));

        } else {
            log.warn("PartyController getRequest(" + requestId + ") --> NULL");
        }

        return request;
    }

    @SuppressWarnings("unchecked")
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/action/request/{requestId}", method = RequestMethod.GET)
    public Request actionRequest(@PathVariable long requestId, @RequestParam(value = "action", defaultValue = "no") String action, @RequestParam(value = "json", defaultValue = "") String json, HttpServletRequest httpRequest) throws Exception {

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

                userIsAuthor = (request.getUserId().equals(client.getUser().getId()));
                userIsPartyAdmin = Helper.userIsAdminOnParty(user, request.getPartyId());
                userIsPartyReviewer = Helper.userIsReviewerOnParty(user, request.getPartyId());
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
                    notificationService.create(NotificationType.REVIEW_OK, request.getUserId(), request.getPartyId(), request.getId());
                    // delete any waiting notification finding a reviewer
                    notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());
                }

                // publish info about update on public channel
                CommandMessage msg = new CommandMessage();
                msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
                msg.setData("{\"party\":" + request.getPartyId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
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
                        msg.setData("{\"party\":" + request.getPartyId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
                        webSocket.convertAndSend("/out/updates", GSON.toJson(msg));

                        // delete any waiting notification finding a reviewer
                        notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());

                        // send notification to author
                        notificationService.create(REVIEW_FAIL, request.getUserId(), request.getPartyId(), request.getId());
                    } else
                        // do reward
                        if (action.equals("reward")) {
                            // needed json data
                            if ((json == null) || (json.length() == 0)) throw new Exception("minning parameter json");
                            List<Long> ids = new ArrayList<Long>();
                            try {
                                List<Integer> idsInts = (new ObjectMapper()).readValue(json, ids.getClass());
                                int nInts = idsInts.size();
                                for (int i = 0; i < nInts; ++i) {
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
                                for (Long rewardId : ids) {
                                    log.info("making transfere reward to userId(" + rewardId + ") ...");
                                    if (rewardId == null) {
                                        log.warn("skipping a NULL rewardId");
                                        continue;
                                    }
                                    if (rewardId.equals(request.getUserId())) {
                                        log.warn("ignoring the author self-rewrad");
                                        continue;
                                    }
                                    final String rewardeeAccountName = AccountingTools.getAccountNameFromUserAndParty(rewardId, request.getPartyId());
                                    if (!accountingService.transferBetweenAccounts(TransactionType.TASK_REWARD, requestAccountName, rewardeeAccountName, rewardPerPerson)) {
                                        log.error("FAIL payout reward(" + rewardPerPerson + ") from(" + requestAccountName + ") to " + rewardeeAccountName);
                                    } else {
                                        log.info("OK payout reward(" + rewardPerPerson + ") from(" + requestAccountName + ") to " + rewardeeAccountName);
                                        // send notification to author
                                        notificationService.create(REWARD_GOT, rewardId, request.getPartyId(), request.getId());
                                    }
                                }
                                // notification to all supporters of request about finish
                                List<KonfettiTransaction> allPayIns = konfettiTransactionService.getAllTransactionsToAccount(AccountingTools.getAccountNameFromRequest(requestId));
                                for (KonfettiTransaction payIn : allPayIns) {
                                    if ((payIn.getType() == TransactionType.TASK_SUPPORT) && (!AccountingTools.getAccountNameFromUserAndParty(request.getUserId(), request.getPartyId()).equals(payIn.getFromAccount()))) {
                                        notificationService.create(SUPPORT_WIN, AccountingTools.getUserIdFromAccountName(payIn.getFromAccount()), AccountingTools.getPartyIdFromAccountName(payIn.getFromAccount()), request.getId());
                                    }
                                }
                            }

                            // set processing & persists
                            request.setState(STATE_DONE);
                            requestService.update(request);
                            log.info("request(" + requestId + ") set STATE to " + STATE_DONE);

                            // publish info about update on public channel
                            CommandMessage msg = new CommandMessage();
                            msg.setCommand(CommandMessage.COMMAND_PARTYUPADTE);
                            msg.setData("{\"party\":" + request.getPartyId() + ", \"request\":" + request.getId() + " ,\"state\":\"" + request.getState() + "\"}");
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
                                if (!chat.getRequestId().equals(request.getId()))
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
                                    requestService.update(request);
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
                                        requestService.update(request);
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

        return request;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/{partyId}/request", method = RequestMethod.GET)
    public List<Request> getAllPartyRequests(@PathVariable long partyId) throws Exception {
        return requestService.getAllPartyRequests(partyId);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/testData", method = RequestMethod.GET)
    public List<Party> testData() throws Exception {
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
