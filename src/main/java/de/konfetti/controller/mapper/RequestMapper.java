package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.Party;
import de.konfetti.data.Request;
import de.konfetti.data.User;
import de.konfetti.service.PartyService;
import de.konfetti.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by relampago on 17.12.16.
 */
@Service
public class RequestMapper {

    @Autowired
    private UserService userService;

    @Autowired
    private PartyService partyService;


    public RequestVm toRequestVm(Request request) {
        RequestVm requestVm = new RequestVm();
        requestVm.setId(request.getId());
        requestVm.setUserId(request.getUser().getId());
        requestVm.setUserName(request.getUser().getName());
        requestVm.setPartyId(request.getParty().getId());
        requestVm.setState(request.getState());
        requestVm.setTitle(request.getTitle());
        requestVm.setTitleMultiLangRef(request.getTitleMultiLangRef());
        requestVm.setTime(request.getTime());
        requestVm.setMediaItemIds(request.getMediaItemIds());
        return requestVm;
    }

    public Request fromRequestVm(RequestVm requestVm) {
        User user = userService.findById(requestVm.getUserId());
        Party party = partyService.findById(requestVm.getPartyId());

        Request request = new Request();
        request.setId(requestVm.getId());
        request.setUser(user);
        request.setParty(party);
        request.setState(requestVm.getState());
        request.setTitle(requestVm.getTitle());
        request.setTitleMultiLangRef(requestVm.getTitleMultiLangRef());
        request.setTime(requestVm.getTime());
        request.setMediaItemIds(requestVm.getMediaItemIds());
        return request;
    }

}
