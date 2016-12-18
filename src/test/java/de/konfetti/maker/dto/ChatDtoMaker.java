package de.konfetti.maker.dto;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import de.konfetti.controller.vm.ChatDto;
import de.konfetti.data.Message;

import java.util.HashMap;
import java.util.List;

import static com.natpryce.makeiteasy.Property.newProperty;

/**
 * Created by relampago on 18.12.16.
 */
public class ChatDtoMaker {
    
    public static final Property<ChatDto, Long> id = newProperty();
    public static final Property<ChatDto, Long> requestId = newProperty();
    public static final Property<ChatDto, Long> partyId = newProperty();
    public static final Property<ChatDto, Long> hostId = newProperty();
    public static final Property<ChatDto, Long[]> members = newProperty();
    public static final Property<ChatDto, Boolean> muted = newProperty();
    public static final Property<ChatDto, HashMap<Long, Long>> lastTSperMember = newProperty();
    public static final Property<ChatDto, List<Message>> messages = newProperty();
    public static final Property<ChatDto, Long> chatPartnerId = newProperty();
    public static final Property<ChatDto, String> chatPartnerName = newProperty();
    public static final Property<ChatDto, Long> chatPartnerImageMediaID = newProperty();


    public static final Instantiator<ChatDto> ExampleChatDto = propertyLookup -> {
        Long nullLongValue = null;
        Long[] emptyArrayValue = new Long[]{};
        HashMap<Long,Long> emptyHashMapValue = new HashMap<>();
        List<Message> nullMessageList = null;

        ChatDto chatDto = new ChatDto();
        chatDto.setId(propertyLookup.valueOf(id, nullLongValue));
        chatDto.setRequestId(propertyLookup.valueOf(requestId, nullLongValue));
        chatDto.setPartyId(propertyLookup.valueOf(partyId, nullLongValue));
        chatDto.setHostId(propertyLookup.valueOf(hostId, nullLongValue));
        chatDto.setMembers(propertyLookup.valueOf(members, emptyArrayValue));
        chatDto.setMuted(propertyLookup.valueOf(muted, Boolean.FALSE));
        chatDto.setLastTSperMember(propertyLookup.valueOf(lastTSperMember, emptyHashMapValue));
        chatDto.setMessages(propertyLookup.valueOf(messages, nullMessageList));
        chatDto.setChatPartnerId(propertyLookup.valueOf(chatPartnerId, nullLongValue));
        chatDto.setChatPartnerName(propertyLookup.valueOf(chatPartnerName, "testChatPartnerName"));
        chatDto.setChatPartnerImageMediaID(propertyLookup.valueOf(chatPartnerImageMediaID, nullLongValue));

        return chatDto;
    };
}
