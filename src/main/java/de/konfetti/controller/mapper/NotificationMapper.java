package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.NotificationDto;
import de.konfetti.data.Notification;

/**
 * Created by relampago on 18.12.16.
 */
public class NotificationMapper {

    public NotificationDto toNotificationDto(Notification notification) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setId(notification.getId());
        notificationDto.setUserId(notification.getUser().getId());
        notificationDto.setPartyId(notification.getParty().getId());
        notificationDto.setType(notification.getType());
        notificationDto.setRef(notification.getRef());
        notificationDto.setTimeStamp(notification.getTimeStamp());
        notificationDto.setHigherPushDone(notification.getHigherPushDone());
        return notificationDto;
    }

}
