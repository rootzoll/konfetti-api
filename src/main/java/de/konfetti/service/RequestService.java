package de.konfetti.service;

import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.Request;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface RequestService {

    Request create(@NotNull Request request);

    Request update(@NotNull Request request);

    Request delete(@NotNull long requestId);
    
    Request findById(long requestId);

    List<RequestVm> getAllPartyRequests(@NotNull long partyId);

	Long getNumberOfRequests();
}