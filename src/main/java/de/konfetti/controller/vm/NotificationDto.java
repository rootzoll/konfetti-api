package de.konfetti.controller.vm;

import de.konfetti.data.NotificationType;
import lombok.Data;

/**
 * Created by relampago on 18.12.16.
 */
@Data
public class NotificationDto {

    private Long id;

    private Long userId;

    private Long partyId;

    private NotificationType type;

    private Long ref;

    private Long timeStamp;

    private Boolean higherPushDone = Boolean.FALSE;

    public boolean needsManualDeletion;

}
