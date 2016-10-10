package de.konfetti.data;

import de.konfetti.data.enums.PartyReviewLevelEnum;
import de.konfetti.data.enums.PartyVisibilityEnum;
import lombok.Data;

import javax.persistence.*;
import java.util.Set;

import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;

@Entity
@Data
public class Party {

	/*
	 * KONFETTI SEND MODE
	 */
	
	// sending of konfetti to other users is disabled 
	public static final int SENDKONFETTIMODE_DISABLED = 0;
	// all konfetti can be send to other users/e-mails
	public static final int SENDKONFETTIMODE_ALL = 1;
	// just earned konfetti can be send to other users/e-mails	
	public static final int SENDKONFETTIMODE_JUSTEARNED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // name of party to display
    private String name;
    
    // detail text (can contain basic HTML)
    // for e.g. address to show for editorial info
    private String detailText;

    // website http-address or email for further info
    // optional but should be seperate field so client can offer options
    private String contact;
    
    // determines the visibilty of the party to new users
    // see final values VISIBILITY_* above
    private PartyVisibilityEnum visibility = VISIBILITY_PUBLIC;
    
    // determines if orga admins need to review public posting
    // see final values REVIEWLEVEL_* above
    private PartyReviewLevelEnum reviewLevel = REVIEWLEVEL_NONE;
    
    // minimal konfetti to spend on new request posting
    private int newRequestMinKonfetti = 0;
    
    // konfetti amount a new user gets 
    private long welcomeBalance = 0;

    /*
     * GEO DATA
     * is a GPS coordinate (lat/lon) together with a radius in meter
     * just if user within this radius party will be shown
     */

    private Float lon;
    private Float lat;
    private int meters;

    @Transient
    private int distanceToUser;

    /*
     * SEND KONFETTI
     * feature to send konfetti to other users
     * for mode use SENDKONFETTIMODE_* (see above)
     * if any value is set on white list its activated
     * white list is a list of e-mail addresses only allowed to send to
     */
    
    private Integer sendKonfettiMode;
    private String[] sendKonfettiWhiteList = {};

    /*
     * TRANSIENT DATA
     * just be delivered to client on REST end point
     */
    
    @Transient // how many konfetti has calling user personally on this party
    private long konfettiCount;
    @Transient // how many konfetti can be send if feature is enabled
    private long sendKonfettiMaxAmount;
    @Transient // how many konfetti calling user earned total on this party
    private long konfettiTotal;
    @Transient // which ranking place the calling user has on this party   
    private int topPosition;
    
    @Transient // requests (tasks) relevant for this party
    private Set<Request> requests;
    
	@Transient // notification relevant for this party and user
    private Set<Notification> notifications;

	public String[] getSendKonfettiWhiteList() {
		if (sendKonfettiWhiteList==null) return new String[0];
		return sendKonfettiWhiteList;
	}

	public Integer getSendKonfettiMode() {
		if (sendKonfettiMode==null) return SENDKONFETTIMODE_DISABLED;
		return sendKonfettiMode;
	}

}
