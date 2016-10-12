package de.konfetti.data;

import de.konfetti.data.enums.RequestStateEnum;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long partyId;

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
    private String userName;
    
    // hard copy from user
    private Long imageMediaID;
    
    // hard copy from user
    private String[] spokenLangs = {};

    /*
     * Transient Data
     */
    @Transient // --> get from accounting
    private long konfettiCount;

    @Transient // --> just for transport
    private long konfettiAdd;
    
    @Transient // --> for delivery
    private List<Chat> chats = new ArrayList<Chat>();
    
	@Transient // --> for delivery
    private List<MediaItem> info = new ArrayList<MediaItem>();

	@Transient // --> just for delivery
	private MediaItem titleMultiLang;
	
	@Transient // --> the amount a single user supported the request
	private Long konfettiAmountSupport = 0L;
	
	@Transient // --> the amount a single user got rewarded by the request
	private Long konfettiAmountReward = 0L;
}
