package de.konfetti.data;

import de.konfetti.data.enums.RequestStateEnum;
import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    private RequestStateEnum state;
    
    private String title;
    
    private Long titleMultiLangRef;
    
	private Long time;
	
	private Long[] mediaItemIds = {}; 
    
    /*
     * Hard Copy
     * Some data fields like user name and image are a hard copy from user and does not update when changed on user object
     * thats because a request object should not be changed on public visible info after a review was done
     */
    // hard copy from user
    private Long imageMediaID;
    
    // hard copy from user
    private String[] spokenLangs = {};

}
