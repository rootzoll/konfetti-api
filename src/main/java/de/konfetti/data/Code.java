package de.konfetti.data;

import lombok.Data;

import javax.persistence.*;

import static de.konfetti.data.CodeActionTypeEnum.ACTION_TYPE_KONFETTI;

@Data
@Entity
public class Code {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	// the time of creation
    private Long timestamp;
    
    // the party the code belongs to
    private Long partyID;
    
    // creator of coupon
    private Long userID;
    
    // the code of the coupon - normally just a number - but could be also string
    @Column(unique=true)
    private String code;
    
    // what kind of action is behind this 
    private CodeActionTypeEnum actionType = ACTION_TYPE_KONFETTI;
    
    // amount of konfetti (when actionType => 0)
    private Long amount;

}
