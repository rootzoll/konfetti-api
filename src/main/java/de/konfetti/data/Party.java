package de.konfetti.data;

import de.konfetti.data.enums.PartyReviewLevelEnum;
import de.konfetti.data.enums.PartyVisibilityEnum;
import de.konfetti.data.enums.SendKonfettiModeEnum;
import lombok.Data;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static de.konfetti.data.enums.PartyReviewLevelEnum.REVIEWLEVEL_NONE;
import static de.konfetti.data.enums.PartyVisibilityEnum.VISIBILITY_PUBLIC;
import static de.konfetti.data.enums.SendKonfettiModeEnum.SENDKONFETTIMODE_DISABLED;

@Entity
@Data
public class Party {

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
    @Enumerated
    private PartyVisibilityEnum visibility = VISIBILITY_PUBLIC;
    
    // determines if orga admins need to review public posting
    // see final values REVIEWLEVEL_* above
    @Enumerated
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

    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "activeParties")
    private List<User> activeUsers = new ArrayList<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "adminParties")
    private List<User> adminUsers = new ArrayList<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany(mappedBy = "reviewerParties")
    private List<User> reviewerUser = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "party")
    private List<Request> requests;

    /*
     * SEND KONFETTI
     * feature to send konfetti to other users
     * for mode use SENDKONFETTIMODE_* (see above)
     * if any value is set on white list its activated
     * white list is a list of e-mail addresses only allowed to send to
     */
    private SendKonfettiModeEnum sendKonfettiMode;

    private String[] sendKonfettiWhiteList = {};

	public String[] getSendKonfettiWhiteList() {
		if (sendKonfettiWhiteList==null) return new String[0];
		return sendKonfettiWhiteList;
	}

	public SendKonfettiModeEnum getSendKonfettiMode() {
		if (sendKonfettiMode==null) return SENDKONFETTIMODE_DISABLED;
		return sendKonfettiMode;
	}

}
