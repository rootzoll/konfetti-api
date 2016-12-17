package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.Request;

/**
 * Created by relampago on 17.12.16.
 */
public class RequestMapper {

    public RequestVm toRequestVm(Request request) {
        RequestVm requestVm = new RequestVm();
        requestVm.setId(request.getId());
        requestVm.setUserId(request.getUserId());
        requestVm.setUserName(request.getUserName());
        requestVm.setPartyId(request.getPartyId());
        requestVm.setState(request.getState());
        requestVm.setTitle(request.getTitle());
        requestVm.setTitleMultiLangRef(request.getTitleMultiLangRef());
        requestVm.setTime(request.getTime());
        requestVm.setMediaItemIds(request.getMediaItemIds());
        return requestVm;
    }

    public Request fromRequestVm(RequestVm requestVm) {
        Request request = new Request();
        request.setId(requestVm.getId());
        request.setUserId(requestVm.getUserId());
        request.setUserName(requestVm.getUserName());
        request.setPartyId(requestVm.getPartyId());
        request.setState(requestVm.getState());
        request.setTitle(requestVm.getTitle());
        request.setTitleMultiLangRef(requestVm.getTitleMultiLangRef());
        request.setTime(requestVm.getTime());
        request.setMediaItemIds(requestVm.getMediaItemIds());
        return request;
    }
}
