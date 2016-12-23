package de.konfetti.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import de.konfetti.data.Code;
import de.konfetti.data.NotificationType;
import de.konfetti.data.Party;
import de.konfetti.data.Request;
import de.konfetti.data.User;
import de.konfetti.service.NotificationService;
import de.konfetti.service.UserService;
import lombok.extern.slf4j.Slf4j;

import static de.konfetti.data.NotificationType.PAYBACK;
import static de.konfetti.data.NotificationType.REVIEW_FAIL;
import static de.konfetti.data.NotificationType.REVIEW_WAITING;
import static de.konfetti.data.NotificationType.REWARD_GOT;
import static de.konfetti.data.NotificationType.SUPPORT_WIN;
import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;

import java.util.List;
import java.util.Locale;

/*
 * Use to manage nofification sending to users. 
 * The manager will decide on which way the notification get delivered.
 * --> see application.properties file for configuration
 */
@Slf4j
@Service
@Configuration
public class NotificationManager {

    private static final String PUSHTYPE_NOTPOSSIBLE = "not-possible";
    private static final String PUSHTYPE_EMAIL = "email";
    private static final String PUSHTYPE_PUSH = "push";
    
    @Autowired
    private EMailManager eMailManager;

    @Autowired
    private MessageSource messageSource;
   
    @Autowired
    private NotificationService notificationService;
    
    @SuppressWarnings("unused")
	@Autowired
    private UserService userService;
   
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
    		
    		// send notification to all reviewers
    		for (User user : reviewer) {
    			
    			// set text by locale of user
    			String locale = user.decideWichLanguageForUser();
    			
    			String textShort = messageSource.getMessage("push.reviewer.short", new String[]{}, Locale.forLanguageTag(locale));
    			String textLong = messageSource.getMessage("push.reviewer.long", new String[]{request.getParty().getName()}, Locale.forLanguageTag(locale));
    			String metaJSON = "{\"type\": \"REVIEW_WAITING\",\"partyID\": "+request.getParty().getId()+",\"taskID\": "+request.getId()+"}";
    			    			
    			final String pushType = getTypeOfPushForUser(user);
    			if (PUSHTYPE_EMAIL.equals(pushType)) {
    				
    				// EMAIL
    				sendPushMail(user, textShort, textLong, locale);
    				
    			} else {
    				
    				// PUSH NOTIFICATION
    				sendPushPush(user, textShort, textLong, metaJSON, locale);
    			}
    			
			}
    		
    		
    	} catch (Exception e) {
    		log.error("sendNotification_ReviewWAITING: FAILED TO PROCESS", e);
    	}
    	    	
    }
    
    // TODO: push/email
    public void sendNotification_ReviewOK(Request request) {
    	
    	log.warn("sendNotification_ReviewOK: TODO push/email");
    	
        // delete any waiting notification finding a reviewer
        notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());
    	
    	// store notification to be displayed on party
        notificationService.create(NotificationType.REVIEW_OK, request.getUser(), request.getParty(), request.getId());

    }
    
    // TODO: push/email
    public void sendNotification_ReviewFAIL(Request request) {
    	
    	log.warn("sendNotification_ReviewFAIL: TODO push/email");
    
    	// delete any waiting notification finding a reviewer
        notificationService.deleteByTypeAndReference(REVIEW_WAITING, request.getId());

    	// store notification to be displayed on party
        notificationService.create(REVIEW_FAIL, request.getUser(), request.getParty(), request.getId());
 
    }
    
    // TODO: implement and add to correct spots in code
    public void sendNotification_TaskCHAT() {
    	
    	log.warn("sendNotification_TaskCHAT: TODO");
    	
    	// why no notification persistence? deleted on last commit? or will it get calculated on party list deliver? 
    	
    	/*
    	 // send push notification if possible
    	if (PushManager.getInstance().isAvaliable()) {
			log.info("PushMessage Alert");
			if (receivers!=null) {
    			for (Long userID : receivers) {
					log.info("PUSHTO(" + userID + ")");
					User receiver = userService.findById(userID);
    				if (receiver!=null) {
    					if (receiver.getPushActive()) {
							log.info(" - WIN - DOING PUSH ...");

							// TODO multi lang - see user
    	    				PushManager.getInstance().sendNotification(
    	    						PushManager.PLATFORM_ANDROID, 
    	    						receiver.getPushID(), 
    	    						"new chat message for you", 
    	    						null, //locale, 
    	    						null, //messageLocale,);
							log.info(" - PUSH DONE :D");

						} else {
							log.info(" - FAIL - NO PUSH");
						}
    				} else {
						log.warn("PUSH RECEIVER id(" + userID + ") NOT FOUND");
					}
				}
    		} else {
				log.info("No Receivers on chat ?!? - no push");
			}
     	} else {
			log.info("PushMessage not configured");
		}
    	 
    	 */
    	
    }
        
    // TODO: push/email
    public void sendNotification_TaskREWARD(User user, Request request) {
    	
    	log.warn("sendNotification_TaskREWARD: TODO push/email");
            
    	// store notification to be displayed on party
    	notificationService.create(REWARD_GOT, user, request.getParty(), request.getId());
  
    }

    // TODO: push/email
    public void sendNotification_VotePAYBACK(User user, Party party, Long amount) {
    	
    	log.warn("sendNotification_VotePAYBACK: TODO push/email");
    	
    	// store notification to be displayed on party
    	notificationService.create(PAYBACK, user, party, amount);
    	
    }
    
    // TODO: push/email
    public void sendNotification_VoteDONE(User user, Request request) {
    	
    	log.warn("sendNotification_VoteDONE: TODO push/email");

    	// store notification to be displayed on party
    	notificationService.create(SUPPORT_WIN, user, request.getParty(), request.getId());

    }
    
    // TODO: implement and add to correct spots in code
    public boolean sendNotification_SendCOUPON(String receiverMail, User user, Code code) {
    	
    	log.warn("sendNotification_SendCOUPON: TODO");
 
    	Locale locale = Locale.forLanguageTag(user.decideWichLanguageForUser());
		String subject = messageSource.getMessage("email.coupon.send.head", new String[]{code.getAmount()+""}, locale);
		String body = messageSource.getMessage("email.coupon.send.body", new String[]{code.getCode()}, locale);
    	// TODO: add in email how to install app & enter code (or browser)
		
    	// send coupon by eMail
    	eMailManager.sendMail(receiverMail, subject, body, null);
    	
    	return false;
    	
    }
    
    public void sendNotification_PartyWelcome(User user, Party party) {
    	// store notification to be displayed on party
        notificationService.create(NotificationType.PARTY_WELCOME, user, party, 0L);
    }
    
    // TODO
	public void sendNotification_SendTRANSFER() {
		
	   	log.warn("sendNotification_SendTRANSFER: TODO");

		/*
	            // send notification receiver (email as fallback)
            boolean sendNotification = false;
            if ((toUser.getPushID() != null) && (PushManager.getInstance().isAvaliable())) {
                // push notification
                if (PushManager.getInstance().sendNotification(
                        PushManager.mapUserPlatform(toUser.getPushSystem()),
                        toUser.getPushID(),
                        "You received " + amount + " Konfetti on Party '" + party.getName() + "'",
                        null,
                        null,
                        0l
                )) {
                    log.info("- push notification send to");
                    sendNotification = true;
                } else {
                    log.warn("was not able to send push notification to uuserId(" + user.getId() + ")");
                }
            }

            if (!sendNotification) {
                // eMail
                if ((mailEnabled) && (mailService.sendMail(address, "rest.user.coupons.received.party", "Open app and check party '" + party.getName() + "' :)", null, user.getSpokenLangs()))) {
                    log.info("- eMail with Info notification send to: " + address);
                } else {
                    log.error("Was not able to send eMail with Notification about received konfetti to " + user.getEMail() + " - check address and server email config");
                }
            }
	
		 */
		
	}
		
    
    /*
     * PRIVATE METHODS
     */
            
    /**
     * decide how to send push notification to user
     *
     * @param notification
     * @return
     */
    private String getTypeOfPushForUser(User user) {
    	
        // check for push notification
        if ((user.getPushActive()) && (PushManager.getInstance().isAvaliable())) {
        	return PUSHTYPE_PUSH;
        }

        // check for eMail
        if ((user.getEMail() != null) && (user.getEMail().trim().length() >= 4)) {
        	return PUSHTYPE_EMAIL;
            
        }
        
        return PUSHTYPE_NOTPOSSIBLE;
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
        
       if (PushManager.getInstance().sendNotification(
                PushManager.PLATFORM_ANDROID,
                user.getPushID(),
                textShort,
                locale,
                meta)) {
           log.info("OK - PUSH SEND BY PUSH (" + user.getPushID() + ")");
       } else {
           log.warn("FAIL - PUSH SEND BY PUSH (" + user.getPushID() + ")");
       }
        return true;
    }

    
}
