package de.konfetti.notifier;

import com.google.common.cache.CacheBuilder;
import de.konfetti.data.Notification;
import de.konfetti.data.NotificationType;
import de.konfetti.data.Party;
import de.konfetti.data.User;
import de.konfetti.service.NotificationService;
import de.konfetti.service.UserService;
import de.konfetti.utils.EMailManager;
import de.konfetti.utils.PushManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/*
 * A task that is scheduled to check in short periods 
 * if there is any notification to be delivered by email or push. 
 * 
 * Is designed to run as a scheduled singleton process with access to database.
 * 
 * TODO: add push notification support - just email for now
 *
 */
@Slf4j
@Component
@Transactional
public class NotifierBackgroundTask {

    private static final String PUSHTYPE_IGNORE = "ignore";
    private static final String PUSHTYPE_NOTPOSSIBLE = "not-possible";
    private static final String PUSHTYPE_FAIL = "fail";
    private static final String PUSHTYPE_EMAIL = "email";
    private static final String PUSHTYPE_PUSH = "push";

    @Autowired
    private EMailManager eMailManager;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    //CACHE 1 "Pushed Users" - make sure just one/push/user all 15min max
    private Cache spamBlockerPerUserCache;
    
    // CACHE 2 "Processed Notifications" - notification persistence can have a storage sync latency up to 15 minutes on processed flag
    private Cache processedNotificationsCache;

    public NotifierBackgroundTask() {
        log.info("CONTRUCTOR BACKGROUNDTASK");
        this.processedNotificationsCache = this.cacheManager().getCache("processedNotifications");
        this.spamBlockerPerUserCache = this.cacheManager().getCache("spamBlockerPerUser");
    }


    /**
     * Constructor
     * @return
     */
    @Bean
    public CacheManager cacheManager() {
        GuavaCacheManager guavaCacheManager = new GuavaCacheManager();
        guavaCacheManager.setCacheBuilder(CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES));
        return guavaCacheManager;
    }

    /*
     *  This is the timer thread started by Spring to ensure that
     *  the background task gets scheduled often.
     *
     *  Spring boost takes care not to start the task when still running.
     */
    @Scheduled(fixedRate = 5000)
    public void periodicStartUpTimer() {
        // start notifier background task loop and catch all problems
        try {
            // do the actual work
            // runNotifierBackgroundTaskThread();
        	
        	log.info("NotifierBackgroundTask DEPRECATED: TODO: get rid of it completly and also remove Notification persistence");
        	
        } catch (Exception e) {
            log.error("EXCEPTION on NotifierBackgroundTask loop: ", e);
            e.printStackTrace();
        }
    }

    /**
     * What the background task actually does when running (one loop)
     *
     * @throws InterruptedException
     */
    private void runNotifierBackgroundTaskThread() throws InterruptedException {
        // Going thru all pending notifications and see which to level up to email or push
        // TODO: get just pending notifications form database (at the moment all that are not deleted)
        List<Notification> pendingNotifications = notificationService.getAllPossiblePushNotifications();
        //log.info("--> PENDING NOTIFICATIONS: " + pendingNotifications.size());

        for (Notification notification : pendingNotifications) {
            // check if already handled since last restart
            if (!wasNotificationAlreadyGivenHigherAttention(notification)) {
                log.info("|");
                log.info(" -> PROCESSING NOTIFICATION(" + notification.getId() + ")");
                // decide if notification needs more attention from user
                if (shouldNotificationGetHigherAttention(notification)) {
                    log.info("--> needs higher attention");
                    // to prevent spaming the user
                    if (userNotFeelingSpammedYet(notification)) {
                        log.info(" -> SEND PUSH TO USER");
                        // decide if eMail or push notification
                        String typeOfPush = getTypeOfPushForUser(notification);

                        // if not possible ok - see as done
                        if (PUSHTYPE_NOTPOSSIBLE.equals(typeOfPush)) {
                            log.info(" -> OK - NOT POSSIBLE TO SEND ANY HIGHER PUSH TO USER");
                            markNotificationAsPushed(notification, typeOfPush);
                            return;
                        }
                        // email push
                        if (PUSHTYPE_EMAIL.equals(typeOfPush)) {
                            log.info(" -> SEND EMAIL");
                            if (sendPushMail(notification)) {
                                log.info(" -> OK - PUSH SEND BY EMAIL");
                                markNotificationAsPushed(notification, typeOfPush);
                                // remember last notification to user for short period of time
                                this.spamBlockerPerUserCache.put(notification.getUser().getId(), notification);
                            } else {
                                log.warn(" -> SEND NOT SUPPORTED PUSH: " + typeOfPush);
                                markNotificationAsPushed(notification, PUSHTYPE_FAIL);
                            }
                            // do push notification
                        } else if (PUSHTYPE_PUSH.equals(typeOfPush)) {
                            log.info(" -> SEND PUSH");
                            sendPushPush(notification);

                            log.info(" -> OK - PUSH SEND BY EMAIL");
                            markNotificationAsPushed(notification, typeOfPush);
                            // remember last notification to user for short period of time
                            this.spamBlockerPerUserCache.put(notification.getUser().getId(), notification);
                            // not supported push
                        } else {
                            log.warn(" -> SEND TO PUSH: " + typeOfPush);
                        }
                    } else {
                        log.info(" -> USER UNDER SPAM PROTECTION - DELAYING PUSH");
                    }
                } else {
                    log.info("--> IGNORE");
                }
            } else {
                log.debug(" -> ALREADY PROCESSED");
            }
        }
    }

    /**
     * Takes a notification and decides if it needs to be pushed
     *
     * @param notification
     * @return boolean
     */
    private boolean shouldNotificationGetHigherAttention(Notification notification) {
        long oldInSeconds = (System.currentTimeMillis() - notification.getTimeStamp()) / 1000L;
        Party party = notification.getParty();
        if (party == null){
            log.warn("party is null for notification with id : " + notification.getId() + " , mark corrupt notifcation as pushed");
            markNotificationAsPushed(notification, PUSHTYPE_IGNORE);
            return false;
        }
        log.info("Notification seconds(" + oldInSeconds + ") id(" + notification.getId() + ") party(" + party.getId() + ") type(" + notification.getType() + ")");
		
		/*
		 * SIMPLE HIGHER ATTENTION CASES
		 */
        if (NotificationType.REVIEW_OK == notification.getType()) return true;
        if (NotificationType.REVIEW_FAIL == notification.getType()) return true;
        if (NotificationType.CHAT_NEW == notification.getType()) return true;
        if (NotificationType.REWARD_GOT == notification.getType()) return true;
        if (NotificationType.SUPPORT_WIN == notification.getType()) return true;

        // REVIEW WAITING ==> select one reviewer/admin by random
        if (NotificationType.REVIEW_WAITING == notification.getType()) {
        	
            // get all reviewer and admins for party
            Stream<User> reviewer = userService.getAllUsersReviewerOnParty(party.getId());

            // filter all that dont have email or push active
            Stream<User> userWithPushOrEmailStream = reviewer
                    .filter(userX -> userX.getPushActive()  || (
                                    (!StringUtils.isEmpty(userX.getEMail())) && (userX.getEMail().trim().length() > 2))
                    );

            Optional<User> reviewableUser = userWithPushOrEmailStream.findAny();

            // no reviewers --> close notification
            if (!reviewableUser.isPresent()) {
                log.warn("Party(" + party.getId() + ") has no admin or reviewer to deliver notification to.");
                markNotificationAsPushed(notification, PUSHTYPE_IGNORE);
                return false;
            }

            Long reviewerId = reviewableUser.get().getId();
            log.debug("REVIEWER is user(" + reviewerId + ")");
            notification.setUser(userService.findById(reviewerId));
            return true;
        }

        // DEFAULT ==> ALL OTHER MESSAGES ==> MARK AS DONE
        markNotificationAsPushed(notification, PUSHTYPE_IGNORE);
        return false;
    }
			

	/*
	 * TODO maybe cache recently processed IDs if persistence gets more decoupled 
	 */
    private boolean wasNotificationAlreadyGivenHigherAttention(Notification notification) {
        ValueWrapper cacheState = processedNotificationsCache.get(notification.getId());
        if (cacheState == null) {
            // no information on local cache - trust value from persistence
            return notification.getHigherPushDone();
        } else {
            long oldInSeconds = (System.currentTimeMillis() - notification.getTimeStamp()) / 1000L;
            log.warn("Cache has different state than Notification(" + notification.getId() + ") type(" + notification.getType() + ") old(" + oldInSeconds + ")secs from persistence: " + cacheState.get());
            return true;
        }
    }

    private void markNotificationAsPushed(Notification notification, String type) {
        // remember notification as processed
        processedNotificationsCache.put(notification.getId(), type);

        // decide to keep or delete the notification
        boolean keepNotification = true;
        if (notification.getType() == NotificationType.REVIEW_WAITING) keepNotification = false;

        if (keepNotification) {
            // keep but remember that pushed
            notificationService.setNotificationAsPushProcessed(notification.getId());
        } else {
            // delete
            log.info("Deleting notification(" + notification.getId() + ")");
            notificationService.delete(notification.getId());
        }
    }


    /**
     * returns true when it seems OK to send another push to user
     * @param notification
     * @return
     */
    private boolean userNotFeelingSpammedYet(Notification notification) {
        // Check if user was active recently
        User user = userService.findById(notification.getUser().getId());
        if (user.wasUserActiveInLastMinutes(3)) {
            log.info("User(" + user.getId() + ") was/is active on App ... wait with push.");
            return false;
        }

        // Check Pushes send ..
        ValueWrapper inCache = spamBlockerPerUserCache.get(notification.getUser().getId());

        // if no notification recently --> go ahead
        if (inCache == null) return true;

        Notification lastNotificationSendToUser = (Notification) inCache.get();

        // if there was a notification recently - ignore this one if same type
        if (lastNotificationSendToUser.getType() == notification.getType()) {
            log.info("Notification is same type as send recently - IGNORE");
            markNotificationAsPushed(notification, PUSHTYPE_IGNORE);
            return false;
        }

        log.info("User got push noti recently --- so skipping this time");
        return false;
    }


    /**
     * decide how to send push on notification
     *
     * @param notification
     * @return
     */
    private String getTypeOfPushForUser(Notification notification) {
        User user = userService.findById(notification.getUser().getId());

        // check for push notification
        if ((user.getPushActive()) && (PushManager.getInstance().isAvaliable())) {
            // just push the following notifications
            if (NotificationType.REVIEW_WAITING.equals(notification.getType())) return PUSHTYPE_PUSH;
            if (NotificationType.REVIEW_OK.equals(notification.getType())) return PUSHTYPE_PUSH;
            if (NotificationType.CHAT_NEW.equals(notification.getType())) return PUSHTYPE_PUSH;
            if (NotificationType.REWARD_GOT.equals(notification.getType())) return PUSHTYPE_PUSH;
            if (NotificationType.SUPPORT_WIN.equals(notification.getType())) return PUSHTYPE_PUSH;
        }

        // check for eMail
        if ((user.getEMail() == null) || (user.getEMail().trim().length() < 4)) {
            return PUSHTYPE_NOTPOSSIBLE;
        }
        return PUSHTYPE_EMAIL;
    }

    /**
     * sending push by email
     *
     * @param notification
     * @return
     */
    private boolean sendPushMail(Notification notification) {
        User user = userService.findById(notification.getUser().getId());
        if (eMailManager.sendMail(user.getEMail(), "[Konfetti] Party Event", "Open Konfetti App so see more :D", null)) {
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
    private boolean sendPushPush(Notification notification) {
        User user = userService.findById(notification.getUser().getId());
        // TODO multi lang --- see user setting
        PushManager.getInstance().sendNotification(
                PushManager.PLATFORM_ANDROID,
                user.getPushID(),
                "new events in your neighborhood",
                "en", // locale
                "{}");
        log.info("OK - PUSH SEND BY PUSH (" + user.getPushID() + ")");
        return true;
    }

}