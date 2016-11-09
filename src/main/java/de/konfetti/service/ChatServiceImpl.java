package de.konfetti.service;

import de.konfetti.data.Chat;
import de.konfetti.data.ChatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl extends BaseService implements ChatService {

	public ChatServiceImpl() {
	}

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }
    
    @Override
    public Chat create(Chat chat) {
        Chat persited = chatRepository.saveAndFlush(chat);
		log.info("Chat(" + persited.getId() + ") CREATED");
		return persited;
    }

    @Override
	public Chat findById(Long id) {
		log.info("Chat(" + id + ") READ");
		return chatRepository.findOne(id);
    }

	@Override
	public List<Chat> getAllByRequestId(Long id) {
		return chatRepository.findByRequestId(id);
	}
	
	@Override
	public List<Chat> getAllByUserAndParty(Long userId, Long partyId) {
		List<Chat> partyChats = chatRepository.findByPartyId(partyId);
		List<Chat> res = partyChats.stream()
				.filter(chat -> (chat.getHostId().equals(userId))
						|| userIsMemberOfChat(userId, chat.getMembers()))
				.collect(Collectors.toList());
		return res;
	}

	private boolean userIsMemberOfChat(Long userId, Long[] members) {
		boolean isMember = Arrays.asList(members).stream()
				.anyMatch(aLong -> aLong.equals(userId));
		return isMember;
	}

	@Override
	public Chat update(Chat chat) {
        Chat persited = chatRepository.saveAndFlush(chat);
		log.info("Chat(" + persited.getId() + ") UPDATED");
		return persited;
	}
    
}
