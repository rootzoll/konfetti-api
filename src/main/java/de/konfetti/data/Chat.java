package de.konfetti.data;

import lombok.Data;

import javax.persistence.*;
import java.util.HashMap;

@Data
@Entity
public class Chat {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;
    
    // the party this chat belongs to - request --> party (if possible keep optional)
    private Long partyId;
    
    // the admin / initiator of the chat ... the userId
    private Long hostId;
    
    // other userIds part of the chat
    private Long[] members = {};
    
    // true if the author dont want chat to be displayed anymore
    private Boolean muted = false;
    
    // remember which TS was the last message received per member
    private HashMap<Long, Long> lastTSperMember = new HashMap<Long, Long>();

    public Long getLastTSforMember(Long userId) {
        Long lastTS = this.lastTSperMember.get(userId);
        if (lastTS==null) lastTS = 0l;
        return lastTS;
    }

    public void setLastTSforMember(Long userId, Long ts) {
        this.lastTSperMember.put(userId, ts);
    }

    public boolean hasUserSeenLatestMessage(Long userId) {
        if ((userId==null) || (userId==0)) return true;
        Long userTS = this.getLastTSforMember(userId);
        for (Long memberID : this.lastTSperMember.keySet()) {
            if (this.lastTSperMember.get(memberID).longValue()>userTS.longValue()) return false;
        }
        return true;
    }

    public boolean chatContainsMessages() {
        for (Long member : this.lastTSperMember.keySet()) {
            if (this.getLastTSforMember(member).longValue()>0l) return true;
        }
        return false;
    }

}
