package de.konfetti.service;

import de.konfetti.data.Notification;
import de.konfetti.data.NotificationType;
import de.konfetti.data.Party;
import de.konfetti.data.User;

import java.util.List;

public interface NotificationService {

	Notification create(NotificationType type, User user, Party party, Long ref);

    void delete(long notiId);
    
    Notification findById(long notiId);
       
    List<Notification> getAllNotifications();
    
    List<Notification> getAllNotifications(Long userId, Long partyId);

	List<Notification> getAllNotificationsSince(Long userId, Long partyId, Long sinceTimestamp);

	void deleteAllNotificationsOlderThan(Long userId, Long partyId, Long sinceTimestamp);

    List<Notification> getAllPossiblePushNotifications();
    
    void setNotificationAsPushProcessed(Long id);

	void deleteByTypeAndReference(NotificationType type, Long referenceValue);

}