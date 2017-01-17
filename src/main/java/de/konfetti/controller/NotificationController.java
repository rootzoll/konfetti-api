package de.konfetti.controller;

import de.konfetti.data.Client;
import de.konfetti.data.Notification;
import de.konfetti.service.ClientService;
import de.konfetti.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by relampago on 18.12.16.
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping(NotificationController.REST_API_MAPPING)
public class NotificationController {

    public static final String REST_API_MAPPING = "konfetti/api/notification";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ControllerSecurityHelper controllerSecurityHelper;

    @Autowired
    private ClientService clientService;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/notification/{notiId}", method = RequestMethod.DELETE)
    public Notification deleteNotification(@PathVariable long notiId, HttpServletRequest httpRequest) throws Exception {
        
        log.info("*** DELETE Notification ("+notiId+") ***");

        // get notification
        Notification noti = notificationService.findById(notiId);
        if (noti == null) throw new Exception("notification(" + notiId + ") not found");

        // check if user is allowed to work on notification
        if (httpRequest.getHeader("X-CLIENT-ID") != null) {
            // A) check if user is owner of notification
            Client client = controllerSecurityHelper.getClientFromRequestWhileCheckAuth(httpRequest, clientService);
            boolean userIsOwner = (noti.getUser().getId().equals(client.getUser().getId()));
            if (!userIsOwner)
                throw new Exception("cannot action notification(" + notiId + ") - user is not noti owner / client.userID(" + client.getUser().getId() + ") != notiUserId(" + noti.getUser().getId() + ")");
        } else {
            // B) check for trusted application with administrator privilege
            controllerSecurityHelper.checkAdminLevelSecurity(httpRequest);
        }

        if (notiId >= 0L) {
            notificationService.delete(notiId);
            log.info("Notification(" + notiId + ") DELETED");
        } else {
            log.warn("Client should not try to delete temporaray notifications with id<0");
        }
        return noti;
    }
}
