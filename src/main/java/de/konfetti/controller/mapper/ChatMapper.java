package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.ChatDto;
import de.konfetti.data.Chat;
import de.konfetti.data.Request;
import de.konfetti.data.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by relampago on 18.12.16.
 */
@Service
public class ChatMapper {

    @Autowired
    private RequestRepository requestRepository;

    public ChatDto toChatDto(Chat chat) {
        ChatDto chatDto = new ChatDto();
        chatDto.setId(chat.getId());
        chatDto.setHostId(chat.getHostId());
        chatDto.setPartyId(chat.getPartyId());
        chatDto.setRequestId(chat.getRequest().getId());
        chatDto.setMembers(chat.getMembers());
        chatDto.setMuted(chat.getMuted());
        chatDto.setLastTSperMember(chat.getLastTSperMember());
        return chatDto;
    }

    public Chat fromChatDto(ChatDto chatDto) {
        Request request = requestRepository.findOne(chatDto.getRequestId());

        Chat chat = new Chat();
        chat.setId(chatDto.getId());
        chat.setHostId(chatDto.getHostId());
        chat.setPartyId(chatDto.getPartyId());
        chat.setRequest(request);
        chat.setMembers(chatDto.getMembers());
        chat.setMuted(chatDto.getMuted());
        chat.setLastTSperMember(chatDto.getLastTSperMember());
        return chat;
    }

}
