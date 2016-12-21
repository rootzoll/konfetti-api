package de.konfetti.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import de.konfetti.data.Request;
import de.konfetti.data.User;
import de.konfetti.service.UserService;
import lombok.extern.slf4j.Slf4j;

import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;

import java.util.List;

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

    private static final String LOCALE_ENGLISH 	= "en";
    private static final String LOCALE_GERMAN 	= "de";
    private static final String LOCALE_ARABIC 	= "ar";
    
    @Autowired
    private EMailManager eMailManager;

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
    			
    			String textShort = null;
    			String textLong = null;
    			String metaJSON = "{\"type\": \"REVIEW_WAITING\",\"partyID\": "+request.getParty().getId()+",\"taskID\": "+request.getId()+"}";
    			
    			// set text by locale of user
    			String locale = decideWichLanguageForUser(user);
    			
    			if (LOCALE_GERMAN.equals(locale)) {
        			textShort = "Aufgabe wartet auf Prüfung";
        			textLong = "Eine Aufgabe auf der Konfetti Party '"+request.getParty().getName()+"' wartet auf Prüfung.";
    			} else
    			if (LOCALE_ARABIC.equals(locale)) {
        			textShort = "مهمة فحص انتظار";
        			textLong = "مهمة فحص انتظار";
        		} else {
        			textShort = "Task for Review";
        			textLong = "A task for the Konfetti Party '"+request.getParty().getName()+"' is waiting for your review.";
        		}			
    			
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
    
    // TODO: implement and add to correct spots in code
    public void sendNotification_ReviewOK() {
    	log.warn("sendNotification_ReviewOK: TODO");
    }
    
    // TODO: implement and add to correct spots in code
    public void sendNotification_ReviewFAIL() {
    	log.warn("sendNotification_ReviewFAIL: TODO");
    }
    
    // TODO: implement and add to correct spots in code
    public void sendNotification_TaskCHAT() {
    	log.warn("sendNotification_TaskCHAT: TODO");
    	
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
        
    // TODO: implement and add to correct spots in code
    public void sendNotification_TaskREWARD() {
    	log.warn("sendNotification_TaskREWARD: TODO");
    }

    // TODO: implement and add to correct spots in code
    public void sendNotification_VotePAYBACK() {
    	log.warn("sendNotification_VotePAYBACK: TODO");
    }
    
    // TODO: implement and add to correct spots in code
    public void sendNotification_VoteDONE() {
    	log.warn("sendNotification_VoteDONE: TODO");
    }
    
    // TODO: implement and add to correct spots in code
    public boolean sendNotification_SendCOUPON() {
    	log.warn("sendNotification_SendCOUPON: TODO");
    	
    	/*
    	 
    	   // send coupon by eMail
           mailService.sendMail(address, "rest.user.coupons.received", "Open app and redeem coupon code: '" + code.getCode(), null, user.getSpokenLangs()))
    	 
    	 */
    	
    	return false;
    	
    }
    
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
     * user can speak multiple languages ... choose one
     * (quick and dirty)
     * @param user
     * @return
     */
    private String decideWichLanguageForUser(User user) {
    	
    	String result = null;
    	
    	String[] langs = user.getSpokenLangs();
    	for (String lang : langs) {
    		
    		// pick first language available
			if (result==null) result = lang;
			
			// pick german over english
			if ((LOCALE_GERMAN.equals(lang)) && (LOCALE_ENGLISH.equals(result))) result=lang;
			
			// pick english over any other if not german
			if ((LOCALE_ENGLISH.equals(lang)) && (!LOCALE_GERMAN.equals(result))) result=lang;
			
		}
    	
   		// english as backup
		if (result==null) result = LOCALE_ENGLISH;
    	return result;
    	
    }
   
    /**
     * sending push by email
     *
     * @param notification
     * @return
     */
    private boolean sendPushMail(User user, String subject, String body, String locale ) {
 
    	// TODO make 18n extra text
    	subject = "[Konfetti] "+subject;
    	body = body + "\n\nThis email was sent as a notification from the KonfettiApp. It can be that this notification is already outdated, please report your feedback with email notifications to bugs@konfettiapp.de";
        // TODO: add a direct jump link to browser version with meta data as URL parameter
    	
        // TODO multi lang --- see user setting
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
