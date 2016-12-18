package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.ChatDto;
import de.konfetti.data.Chat;

/**
 * Created by relampago on 18.12.16.
 */
public class ChatMapper {

    public ChatDto toChatDto(Chat chat) {
        ChatDto chatDto = new ChatDto();
        chatDto.setId(chat.getId());
        chatDto.setHostId(chat.getHostId());
        chatDto.setPartyId(chat.getPartyId());
        chatDto.setRequestId(chat.getRequestId());
        chatDto.setMembers(chat.getMembers());
        chatDto.setMuted(chat.getMuted());
        chatDto.setLastTSperMember(chat.getLastTSperMember());
        return chatDto;
    }

    public Chat fromChatDto(ChatDto chatDto) {
        Chat chat = new Chat();
        chat.setId(chatDto.getId());
        chat.setHostId(chatDto.getHostId());
        chat.setPartyId(chatDto.getPartyId());
        chat.setRequestId(chatDto.getRequestId());
        chat.setMembers(chatDto.getMembers());
        chat.setMuted(chatDto.getMuted());
        chat.setLastTSperMember(chatDto.getLastTSperMember());
        return chat;
    }

}
