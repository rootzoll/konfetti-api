package de.konfetti.controller.vm;

import de.konfetti.data.Message;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by relampago on 18.12.16.
 */
@Data
public class ChatDto {

    private Long id;

    private Long requestId;

    private Long partyId;

    private Long hostId;

    private Long[] members = {};

    private Boolean muted = false;

    private HashMap<Long, Long> lastTSperMember = new HashMap<Long, Long>();

    private List<Message> messages = new ArrayList<>();

    private Long chatPartnerId;

    private String chatPartnerName;

    private Long chatPartnerImageMediaID;

    private String[] chatPartnerSpokenLangs = new String[]{};

    private boolean unreadMessage;

    public Long getLastTSforMember(Long userId) {
        Long lastTS = this.lastTSperMember.get(userId);
        if (lastTS==null) lastTS = 0L;
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
