package de.konfetti.controller.vm;

import de.konfetti.data.Notification;
import de.konfetti.data.Request;
import de.konfetti.data.enums.PartyReviewLevelEnum;
import de.konfetti.data.enums.PartyVisibilityEnum;
import de.konfetti.data.enums.SendKonfettiModeEnum;
import lombok.Data;

import javax.persistence.Enumerated;
import java.util.HashSet;
import java.util.Set;

import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;
import static de.konfetti.data.enums.SendKonfettiModeEnum.SENDKONFETTIMODE_DISABLED;

/**
 * Created by relampago on 06.11.16.
 */
@Data
public class PartyResponse {

    private Long id;

    private String name;

    private String detailText;

    private String contact;

    @Enumerated
    private PartyVisibilityEnum visibility = VISIBILITY_PUBLIC;

    @Enumerated
    private PartyReviewLevelEnum reviewLevel = REVIEWLEVEL_NONE;

    private int newRequestMinKonfetti = 0;

    private long welcomeBalance = 0;

    // GeoData
    private Float lon;
    private Float lat;
    private int meters;

    private int distanceToUser;

    private SendKonfettiModeEnum sendKonfettiMode;
    private String[] sendKonfettiWhiteList = {};

    private long konfettiCount;

    private long sendKonfettiMaxAmount;

    private long konfettiTotal;

    private int topPosition;

    private Set<Request> requests = new HashSet<>();

    private Set<Notification> notifications = new HashSet<>();

    public PartyResponse(Long id) {
        this.id = id;
    }

    public String[] getSendKonfettiWhiteList() {
        if (sendKonfettiWhiteList==null) return new String[0];
        return sendKonfettiWhiteList;
    }

    public SendKonfettiModeEnum getSendKonfettiMode() {
        if (sendKonfettiMode==null) return SENDKONFETTIMODE_DISABLED;
        return sendKonfettiMode;
    }

    public PartyResponse() {
    }
}
