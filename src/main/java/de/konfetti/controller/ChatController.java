package de.konfetti.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.konfetti.controller.mapper.ChatMapper;
import de.konfetti.controller.vm.ChatDto;
import de.konfetti.data.*;
import de.konfetti.service.*;
import de.konfetti.utils.NotificationManager;
import de.konfetti.websocket.CommandMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

import static de.konfetti.data.enums.RequestStateEnum.STATE_DONE;
import static de.konfetti.data.enums.RequestStateEnum.STATE_PROCESSING;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(ChatController.REST_API_MAPPING)
public class ChatController {

	public static final String REST_API_MAPPING = "konfetti/api/chat";

	private static final Gson GSON = new GsonBuilder().create();

    private final UserService userService;
    private final ClientService clientService;
    private final ChatService chatService;
    private final MessageService messageService;
    private final RequestService requestService;
    private final MediaService mediaService;

	@Autowired
	private ControllerSecurityHelper controllerSecurityHelper;

    @Autowired
    private SimpMessagingTemplate webSocket;
    
    @Autowired 
    private NotificationManager notificationManager;

    @Autowired
    public ChatController(final UserService userService, final ClientService clientService, final ChatService chatService, final MessageService messageService, final RequestService requestService, final MediaService mediaService) {
        this.userService = userService;
        this.clientService = clientService;
        this.chatService = chatService;
        this.messageService = messageService;
        this.requestService = requestService;
        this.mediaService = mediaService;
    }

    //---------------------------------------------------
    // CHAT Controller
    //---------------------------------------------------

	public static ChatDto setChatPartnerInfoOn(UserService userService, ChatDto chatDto, Long chatPartnerUserId, Long selfId) {
		User user = userService.findById(chatPartnerUserId);
		if (user == null) {
			log.warn("Cannot set ChatPartnerInfo for user(" + chatPartnerUserId + ") - NOT FOUND");
			return chatDto;
		}
		chatDto.setChatPartnerId(user.getId());
		chatDto.setChatPartnerName(user.getName());
		if ((user.getImageMediaID() != null) && (user.getImageMediaID() > 0))
			chatDto.setChatPartnerImageMediaID(user.getImageMediaID());
		if ((user.getSpokenLangs() != null) && (user.getSpokenLangs().length > 0))
			chatDto.setChatPartnerSpokenLangs(user.getSpokenLangs());
		chatDto.setUnreadMessage(!chatDto.hasUserSeenLatestMessage(selfId));
		return chatDto;
	}

	@CrossOrigin(origins = "*")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ChatDto> createChat(@RequestBody @Valid final ChatDto template, HttpServletRequest httpRequest) throws Exception {

        log.info("*** POST Create Chat ***");
		
    	// check if user is allowed to create
    	if (httpRequest.getHeader("X-CLIENT-ID")!=null) {

    		// A) check that chat is just hosted by user
    		Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
    		boolean userIsHost = (template.getHostId().equals(client.getUser().getId()));
    		if (!userIsHost) throw new Exception("user cannot create chat for other users");

        	// B) check if request is set and and set correct party id from request
        	if (template.getRequestId()==null) throw new Exception("request reference is not set");
        	Request request = requestService.findById(template.getRequestId());
        	if (request==null) throw new Exception("request("+template.getRequestId()+") not found");
        	template.setPartyId(request.getParty().getId());

        	// C) check if request is open for chats (not done or processing)
        	if (STATE_DONE.equals(request.getState())) throw new Exception("no chat possible on DONE request");
        	if (STATE_PROCESSING.equals(request.getState())) throw new Exception("no chat possible on PROCESSING request");

    	} else {

    		// B) check for trusted application with administrator privilege
        	controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
    	}
    	// security override on template
    	template.setId(null);
    	template.setMessages(new ArrayList<Message>());
    	template.setMuted(false);

    	// check if all members exist
    	for (Long memberId : template.getMembers()) {
			User memberUser = userService.findById(memberId);
			if (memberUser==null) throw new Exception("member("+memberId+") on new chat does NOT EXIST");
		}
    	// create new chat
		ChatMapper chatMapper = new ChatMapper();
    	Chat chat = chatService.create(chatMapper.fromChatDto(template));

    	ChatDto chatDto = chatMapper.toChatDto(chat);
		// add transient chat partner info
		if (httpRequest.getHeader("X-CLIENT-ID")!=null) {
    		if (chat.getMembers().length==1) {
				setChatPartnerInfoOn(userService, chatDto, chat.getMembers()[0], 0L);
			} else {
				log.warn("Cannot set ChatPartnerInfo on chats with more than one member.");
			}
    	}
        return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value="/{chatId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ChatDto> getChat(@PathVariable Long chatId, @RequestParam(value="lastTS",defaultValue="0") Long lastTS, HttpServletRequest httpRequest) throws Exception {

        log.info("*** GET Chat ("+chatId+") ***");
    	
    	// try to load message and chat
    	Chat chat = chatService.findById(chatId);
    	if (chat==null) throw new Exception("chat("+chatId+") not found");

    	// load messages of chat
    	List<Message> messages = messageService.getAllMessagesOfChatSince(chat.getId(),lastTS);

    	ChatDto chatDto = null;

    	// check if user is allowed to get data
    	if (httpRequest.getHeader("X-CLIENT-ID")!=null) {
			// A) check that user is host or member of chat
    		Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
    		boolean userIsHost = (chat.getHostId().equals(client.getUser().getId()));
    		boolean userIsMember = false;
    		for (Long memeberId : chat.getMembers()) {
				if (client.getUser().getId().equals(memeberId)) {
					userIsMember = true;
					break;
				}
			}
    		if ((!userIsHost) && (!userIsMember)) throw new Exception("not host or member on chat("+chatId+")");

			// B) find biggest message TS of delivered messages and remember
        	long biggestTS = 0L;
        	for (Message message : messages) {
    			if (message.getTime()>biggestTS) biggestTS = message.getTime();
    		}
        	if (biggestTS>chat.getLastTSforMember(client.getUser().getId())) {
				chat.setLastTSforMember(client.getUser().getId(), biggestTS);
        		chatService.update(chat);
        	}

			// C) add transient chat partner info
			ChatMapper chatMapper = new ChatMapper();
			chatDto =  chatMapper.toChatDto(chat);
			chatDto.setMessages(messages);

    		if (userIsHost) {
    			// show member as chat partner
    			if (chat.getMembers().length==1) {
    				setChatPartnerInfoOn(userService, chatDto, chat.getMembers()[0], client.getUser().getId());
    			} else {
					log.warn("Cannot set ChatPartnerInfo on chats with more than one member.");
				}
    		} else {
    			// show host as chat partner
    			setChatPartnerInfoOn(userService, chatDto, chat.getHostId(), client.getUser().getId());
    		}
		} else {
			// B) check for trusted application with administrator privilege
        	controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
    	}
		return new ResponseEntity<>(chatDto, HttpStatus.OK);
    }
    
    //---------------------------------------------------
    // MESSAGE Controller
    //---------------------------------------------------

    @CrossOrigin(origins = "*")
    @RequestMapping(value="/{chatId}/message", method = RequestMethod.POST, produces = "application/json")
    public Message addMessage(@PathVariable Long chatId, @RequestBody @Valid final Message template, HttpServletRequest httpRequest) throws Exception {
    	
        log.info("*** POST Message on Chat ("+chatId+") ***");
    	
    	Set<Long> receivers = null;
    	long messageTS = System.currentTimeMillis();
    	
    	Chat chat = chatService.findById(chatId);
    	if (chat==null) throw new Exception("chat("+chatId+") not found");
    	
    	Client client = null;
    	
    	// check if user is allowed to create
    	if (httpRequest.getHeader("X-CLIENT-ID")!=null) {
    		
    		// A) check that user is host or member of chat
    		client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
    		boolean userIsHost = (chat.getHostId().equals(client.getUser().getId()));
    		boolean userIsMember = false;
    		for (Long memeberId : chat.getMembers()) {
				if (client.getUser().getId().equals(memeberId)) {
					userIsMember = true;
					break;
				}
			}
    		if ((!userIsHost) && (!userIsMember)) throw new Exception("not host or member on chat("+chatId+")");
    	
    		// make sure userId is correct
        	template.setUserId(client.getUser().getId());
        	
        	// B) set last TS for posting user to this message TS
        	long lastTSofUser = chat.getLastTSforMember(client.getUser().getId());
        	if (lastTSofUser<messageTS) {
        		chat.setLastTSforMember(client.getUser().getId(), messageTS);
        		chatService.update(chat);
        	} else {
				log.warn("strange: messageTS <= lastTSofUser");
			}
        	
        	// C) prepare list of receivers of this message
    		receivers = new HashSet<Long>();
    		receivers.addAll(Arrays.asList(chat.getMembers()));
    		receivers.add(chat.getHostId());
    		receivers.remove(client.getUser().getId());
    		
    	} else {
    		
    		// A) check for trusted application with administrator privilege
        	controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
    	}
    	
    	// security override on template
    	template.setId(null);
    	template.setTime(messageTS);
    	template.setChatId(chat.getId());
    	    	
    	// TODOD check that itemId exists
    	
    	// create new user
    	Message message = messageService.create(template);
		log.info("Message(" + message.getId() + ") CREATED on chat(" + chatId + ")");


		// publish info about new chat message public channel
    	CommandMessage msg = new CommandMessage();
    	msg.setCommand(CommandMessage.COMMAND_CHATUPADTE);
    	String jsonArray = "[";
    	for (Long memberID : chat.getMembers()) {
    		jsonArray += (memberID + ",");
		}
		jsonArray += (chat.getHostId() + "]");
    	msg.setData("{\"party\":"+chat.getPartyId()+", \"users\":"+jsonArray+"}");
    	webSocket.convertAndSend("/out/updates", GSON.toJson(msg));  
    	
    	// load all users that will receive chat message
    	List<User> receivingUsers = new ArrayList<User>();
    	for (Long userId : receivers) {
			User u = userService.findById(userId);
			if (u!=null) receivingUsers.add(u);
		}
    	    	
    	// send notification
    	this.notificationManager.sendNotification_TaskCHAT(chat, message, this.mediaService.findById(template.getItemId()), receivingUsers, client, requestService.findById(chat.getRequestId()));
    	
        return message;
    }
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value="/{chatId}/message/{messageId}", method = RequestMethod.GET, produces = "application/json")
    public Message actionMessage(@PathVariable Long chatId, @PathVariable Long messageId, HttpServletRequest httpRequest) throws Exception {
        
        log.info("*** GET Message ("+messageId+") on Chat ("+chatId+") ***");
    	
    	// try to load message and chat
    	Chat chat = chatService.findById(chatId);
    	if (chat==null) throw new Exception("chat("+chatId+") not found");
    	Message message = messageService.findById(messageId);
    	if (message==null) throw new Exception("message("+messageId+") not found");
    
    	// check if user is allowed to create
    	if (httpRequest.getHeader("X-CLIENT-ID")!=null) {
    		
    		// A) check that user is host or member of chat
    		Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
    		boolean userIsHost = (chat.getHostId().equals(client.getUser().getId()));
    		boolean userIsMember = false;
    		for (Long memeberId : chat.getMembers()) {
				if (client.getUser().getId().equals(memeberId)) {
					userIsMember = true;
					break;
				}
			}
    		if ((!userIsHost) && (!userIsMember)) throw new Exception("not host or member on chat("+chatId+")");
    
    	} else {
    		
    		// B) check for trusted application with administrator privilege
        	controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
    	}
    	
    	return message;
    }
    
	
}
