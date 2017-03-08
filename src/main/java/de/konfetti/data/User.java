package de.konfetti.data;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class User {

    private static final String LOCALE_ENGLISH 	= "en";
    private static final String LOCALE_GERMAN 	= "de";
	
	public static final String PUSHSYSTEM_IOS = "ios";
	public static final String PUSHSYSTEM_ANDROID = "android";
	public static final String PUSHSYSTEM_CHROME = "chrome";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    private String name;

    private String eMail;

    private String password;

	private Long imageMediaID;

	// list of languages the user speaks (e.g. 'de', 'en', 'ar')
    private String[] spokenLangs = {};

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private List<Client> clients = new ArrayList<>();

    // IDs of parties the user has an konfetti balance on
    @ManyToMany
    @JoinTable(
            name = "user_party_active",
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
    private Long lastActivityTS = 0L;

    private Boolean pushActive = false;
    
    // placeholder to support multiple push systems later on 
    private String pushSystem;
    
    private String pushID;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    @Column(name = "reset_date")
    private ZonedDateTime resetDate = null;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<Request> requests;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<Notification> notifications;
    
	public boolean wasUserActiveInLastMinutes(int minutes) {
		long minutesSinceLastActivity = Math.round((System.currentTimeMillis() - this.lastActivityTS) / (60d*1000d));
		return  ((minutesSinceLastActivity==0) || (minutes>=minutesSinceLastActivity));
	}
	
    /**
     * user can speak multiple languages ... choose one
     * (quick and dirty)
     * @param user
     * @return
     */
    public String decideWichLanguageForUser() {
    	
    	String result = null;
    	
    	String[] langs = this.getSpokenLangs();
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

}

