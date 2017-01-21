package de.konfetti.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import de.konfetti.data.Chat;
import de.konfetti.data.Client;
import de.konfetti.data.Code;
import de.konfetti.data.MediaItem;
import de.konfetti.data.Message;
import de.konfetti.data.Notification;
import de.konfetti.data.NotificationType;
import de.konfetti.data.Party;
import de.konfetti.data.Request;
import de.konfetti.data.User;
import de.konfetti.data.enums.MediaItemTypeEnum;
import de.konfetti.service.NotificationService;
import de.konfetti.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import static de.konfetti.data.NotificationType.PAYBACK;
import static de.konfetti.data.NotificationType.REVIEW_FAIL;
import static de.konfetti.data.NotificationType.REVIEW_WAITING;
import static de.konfetti.data.NotificationType.REWARD_GOT;
import static de.konfetti.data.NotificationType.SUPPORT_WIN;
import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;

import java.util.List;
import java.util.Locale;

import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;

/*
 * Use to manage nofification sending to users. 
 * The manager will decide on which way the notification get delivered.
 * --> see application.properties file for configuration
 */
@Slf4j
@Service
@Configuration
public class NotificationManager {
    
    @Autowired
    private EMailManager eMailManager;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private NotificationService notificationService;

    @SuppressWarnings("unused")
	@Autowired
    private UserService userService;

    @Autowired
    private PushManager pushManager;

    public NotificationManager() {
        log.info("NotificationManager Constructor");
    }
    
    /*
     * USE THE FOLLOWING PUBLIC METHODS FOR DOING NOTIFICATIONS
     */
        
    public void sendNotification_ReviewWAITING(Request request) {
    	
    	try {
    	
        	// check if this party needs a review process
    		if (request.getParty().getReviewLevel() == REVIEWLEVEL_NONE) {
    			log.info("sendNotification_ReviewWAITING: no notification because party has no review process");
    			return;
    		}
        	
        	// find all reviewer for this 
    		List<User> reviewer = request.getParty().getReviewerUser();
    		
    		// if a party has no dedicated reviewers - use admin
    		if (reviewer.size()==0) reviewer = request.getParty().getAdminUsers();
    		
    		// logic check
    		if (reviewer.size()==0) log.warn("sendNotification_ReviewWAITING: party("+request.getParty().getId()+") has no admin user");
    		
    		// if more than 3 reviewers - TODO: find the last three active reviews
    		if (reviewer.size()>3) {
    			log.warn("TODO: reviewer list is bigger than 3 - sort by last active and just use the lastest 3 active");
    			reviewer = reviewer.subList(0, 2);
    		}

    		// send notification to all reviewers
    		for (User user : reviewer) {
    			
    			// TODO: decide to persist a party notification and link id in metaJSON
    			
    			// set text by locale of user
    			String locale = user.decideWichLanguageForUser();
    			
    			String textShort = messageSource.getMessage("push.reviewer.short", new String[]{}, Locale.forLanguageTag(locale));
    			String textLong = messageSource.getMessage("push.reviewer.long", new String[]{request.getParty().getName()}, Locale.forLanguageTag(locale));
    			String metaJSON = "{\"type\": \"REVIEW_WAITING\",\"partyID\": "+request.getParty().getId()+",\"taskID\": "+request.getId()+"}";

				// push notification
				sendPushAuto(user, textShort, textLong, metaJSON, locale);

			}
    		
    		
    	} catch (Exception e) {
    		log.error("sendNotification_ReviewWAITING: FAILED TO PROCESS", e);
    	}
    	    	
    }
    
    public void sendNotification_ReviewOK(Request request) {

    	log.info("sendNotification_ReviewOK to user");

        // delete any waiting notification finding a reviewer
        notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());

    	// store notification to be displayed on party
        Notification notification = notificationService.create(NotificationType.REVIEW_OK, request.getUser(), request.getParty(), request.getId());

		// set text by locale of user
		String locale = request.getUser().decideWichLanguageForUser();

		String textShort = messageSource.getMessage("push.review.ok.short", new String[]{}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("push.review.ok.long", new String[]{request.getTitle()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"REVIEW_OK\",\"partyID\": "+request.getParty().getId()+",\"requestID\": "+request.getId()+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(request.getUser(), textShort, textLong, metaJSON, locale);

    }
    
    // TODO: add comment why rejected that was given in app by reviewer
    public void sendNotification_ReviewFAIL(Request request) {

    	// delete any waiting notification finding a reviewer
        notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());

    	// store notification to be displayed on party
        Notification notification = notificationService.create(REVIEW_FAIL, request.getUser(), request.getParty(), request.getId());

		// set text by locale of user
		String locale = request.getUser().decideWichLanguageForUser();

		String textShort = messageSource.getMessage("push.review.fail.short", new String[]{}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("push.review.fail.long", new String[]{request.getTitle()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"REVIEW_FAIL\",\"partyID\": "+request.getParty().getId()+",\"requestID\": "+request.getId()+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(request.getUser(), textShort, textLong, metaJSON, locale);

    }
    
    public void sendNotification_TaskCHAT(Chat chat, Message message, MediaItem item, List<User> receivingUsers, Client fromClient, Request request) {
    	
    	// get a chat message text preview
    	String messageTextPreview = "(media)";
    	if ((item!=null) && (item.getType()==MediaItemTypeEnum.TYPE_TEXT)) messageTextPreview=item.getData();

    	// get name from user sending
    	String fromUserName = "ADMIN";
    	if (fromClient!=null) fromUserName = fromClient.getUser().getName();

    	// NOTE: notification persistence because at the moment the PartyController will check all chats for new messages
    	if (receivingUsers.size()==0) log.warn("sendNotification_TaskCHAT: no receivers of chat message");

    	for (User user : receivingUsers) {

    		String localeStr = user.decideWichLanguageForUser();
        	Locale locale = Locale.forLanguageTag(localeStr);
    		String subject = messageSource.getMessage("chat.message.new.head", new String[]{request.getTitle(), message.getTime()+""}, locale); // TODO: dateformat with locale
    		String body = messageSource.getMessage("chat.message.new.body", new String[]{fromUserName, messageTextPreview}, locale);
    		String metaJSON = "{\"type\": \"CHAT_NEW\",\"partyID\": "+chat.getPartyId()+",\"chatID\": "+chat.getId()+",\"messageID\": "+message.getId()+"}";

    		// PUSH NOTIFICATION
    		sendPushAuto(user, subject, body, metaJSON, localeStr);

		}

    }

    public void sendNotification_TaskREWARD(User user, Request request, Long amount) {

    	// store notification to be displayed on party
    	Notification notification = notificationService.create(REWARD_GOT, user, request.getParty(), request.getId());
    	
		// set text by locale of user
		String locale = request.getUser().decideWichLanguageForUser();

		String textShort = messageSource.getMessage("push.task.reward.short", new String[]{}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("push.task.reward.long", new String[]{amount+"", request.getTitle()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"REWARD_GOT\",\"partyID\": "+request.getParty().getId()+",\"requestID\": "+request.getId()+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(request.getUser(), textShort, textLong, metaJSON, locale);

    }

    public void sendNotification_VotePAYBACK(User user, Request request, Long amount) {

    	// store notification to be displayed on party
    	Notification notification = notificationService.create(PAYBACK, user, request.getParty(), amount);

		// set text by locale of user
		String locale = request.getUser().decideWichLanguageForUser();

		String textShort = messageSource.getMessage("push.vote.payback.short", new String[]{}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("push.vote.payback.long", new String[]{request.getTitle()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"PAYBACK\",\"partyID\": "+request.getParty().getId()+",\"requestID\": "+request.getId()+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(request.getUser(), textShort, textLong, metaJSON, locale);

    }
    
    // TODO: push/email
    public void sendNotification_VoteDONE(User user, Request request) {

    	// store notification to be displayed on party
    	Notification notification = notificationService.create(SUPPORT_WIN, user, request.getParty(), request.getId());

		// set text by locale of user
		String locale = request.getUser().decideWichLanguageForUser();

		String textShort = messageSource.getMessage("push.vote.done.short", new String[]{}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("push.vote.done.long", new String[]{request.getTitle()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"SUPPORT_WIN\",\"partyID\": "+request.getParty().getId()+",\"requestID\": "+request.getId()+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(request.getUser(), textShort, textLong, metaJSON, locale);

    }
    
    public boolean sendNotification_SendCOUPON(String receiverMail, User user, Code code) {

    	Locale locale = Locale.forLanguageTag(user.decideWichLanguageForUser());
		String subject = messageSource.getMessage("email.coupon.send.head", new String[]{code.getAmount()+""}, locale);
		String body = messageSource.getMessage("email.coupon.send.body", new String[]{code.getCode()}, locale);
    	// TODO: add in email how to install app & enter code (or browser)

    	// send coupon by eMail
    	return eMailManager.sendMail(receiverMail, subject, body, null);

    }

	public void sendNotification_SendTRANSFER(User user, Party party, Integer amount) {

		// set text by locale of user
		String locale = user.decideWichLanguageForUser();

		// create notification object for user
        Notification notification = notificationService.create(NotificationType.TRANSFER_RECEIVED, user, party, new Long(amount));
		
		String textShort = messageSource.getMessage("email.konfetti.transfer.head", new String[]{amount+""}, Locale.forLanguageTag(locale));
		String textLong = messageSource.getMessage("email.konfetti.transfer.body", new String[]{amount+"", party.getName()}, Locale.forLanguageTag(locale));
		String metaJSON = "{\"type\": \"TRANSFER_RECEIVED\",\"partyID\": "+party.getId()+",\"amount\": "+amount+",\"notificationID\": "+notification.getId()+"}";

		// PUSH NOTIFICATION
		sendPushAuto(user, textShort, textLong, metaJSON, locale);

	}
	
    public void sendNotification_PartyWelcome(User user, Party party) {

    	// no push - just store notification to be displayed on party
        notificationService.create(NotificationType.PARTY_WELCOME, user, party, 0L);

    }

    
    /*
     * PRIVATE METHODS
     */
            

    /**
     * tries to send push or email - what ever works best if available
     * @param user
     */
	private boolean sendPushAuto(User user, String textShort, String textLong, String metaJSON, String locale) {

		boolean wasSend = false;

		if (PushManager.getInstance().isAvaliable()) log.warn("Pushnotification Provider is NOT available. Check Server Config.");

        // check for push notification works for user
        if ((user.getPushActive()) && (PushManager.getInstance().isAvaliable())) {
        	wasSend = sendPushPush(user,textShort,textLong,metaJSON,locale);
        }

        // check for eMail email works for user
        if ((!wasSend) && (user.getEMail() != null) && (user.getEMail().trim().length() >= 4)) {
        	wasSend = sendPushMail(user,textShort,textLong,locale);
        }

        if (!wasSend) log.warn("sendPushAuto: Notification was NOT send to user ("+user.getId()+") textShort("+textShort+")");

		return wasSend;

	}


    /**
     * sending push by email
     *
     * @param notification
     * @return
     */
    private boolean sendPushMail(User user, String subject, String body, String locale ) {
 
    	body = body + "\n\n"+messageSource.getMessage("email.general.footer", new String[]{}, Locale.forLanguageTag(locale));
    	// TODO: add a direct jump link to browser version with meta data as URL parameter

        if (eMailManager.sendMail(user.getEMail(), subject, body, null)) {
            log.info("OK - PUSH SEND BY EMAIL (" + user.getEMail() + ")");
            return true;
        } else {
            log.warn("FAIL - PUSH SEND BY EMAIL (" + user.getEMail() + ")");
            return false;
        }
    }


    /**
     * sending push by push
     *
     * @param notification
     * @return
     */
    private boolean sendPushPush(User user, String textShort, String textLong, String meta, String locale) {
        
       if (pushManager.sendNotification(
                PushManager.PLATFORM_ANDROID,
                user.getPushID(),
                textShort,
                locale,
                meta)) {

           log.info("OK - PUSH SEND BY PUSH (" + user.getPushID() + ")");
           return true;

        } else {

           log.warn("FAIL - PUSH SEND BY PUSH (" + user.getPushID() + ")");
           return false;

        }

    }

    
}
