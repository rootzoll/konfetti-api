package de.konfetti.data;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Entity
@Data
public class User {

	public static final String PUSHSYSTEM_IOS = "ios";
	public static final String PUSHSYSTEM_ANDROID = "android";
	public static final String PUSHSYSTEM_CHROME = "chrome";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // self written name (nick or real)
    private String name;

    // email
    private String eMail;

    // password
    private String password;

	// image of user
	private Long imageMediaID;

	// list of languages the user speaks (e.g. 'de', 'en', 'ar')
    private String[] spokenLangs = {};

    @OneToMany(mappedBy = "user")
    private List<Client> clients;

    // IDs of parties the user has an konfetti balance on
    @ManyToMany
    @JoinTable(
            name = "user_party_review",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "party_id", referencedColumnName = "id"))
    private List<Party> activeParties = new ArrayList<>();

	// IDs of parties the user has admin privileges on
    @ManyToMany
    @JoinTable(
            name = "user_party_admin",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "party_id", referencedColumnName = "id"))
    private List<Party> adminParties = new ArrayList<>();

    // IDs of parties the user has reviewer privileges on
    @ManyToMany
    @JoinTable(
            name = "user_party_review",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "party_id", referencedColumnName = "id"))
    private List<Party> reviewerParties = new ArrayList<>();

    // time stamp when the user last was online (not more precise 1 minute)
    private Long lastActivityTS = 0l;

    private Boolean pushActive = false;
    
    private String pushSystem;
    
    private String pushID;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    @Column(name = "reset_date", nullable = true)
    private ZonedDateTime resetDate = null;
    
	public boolean wasUserActiveInLastMinutes(int minutes) {
		long minutesSinceLastActivity = Math.round((System.currentTimeMillis() - this.lastActivityTS) / (60d*1000d));
		boolean wasUserActiveInLastMinutes = ((minutesSinceLastActivity==0) || (minutes>=minutesSinceLastActivity));
		//System.out.println("wasUserActiveInLastMinutes("+minutes+") : User("+this.id+") lastActivity old ("+minutesSinceLastActivity+")min <= "+minutes+" --> "+wasUserActiveInLastMinutes);
		return wasUserActiveInLastMinutes;
	}
    
}

