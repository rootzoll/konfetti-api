package de.konfetti.data;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;
    
    // type (see CONST above)
	private NotificationType type;

	// reference - depending on type (database nightmare)
    private Long ref;
    
    // time stamp of creation
	private Long timeStamp;

	private Boolean higherPushDone = Boolean.FALSE;

}

