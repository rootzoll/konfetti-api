package de.konfetti.service;

import de.konfetti.controller.mapper.RequestMapper;
import de.konfetti.controller.vm.RequestVm;
import de.konfetti.data.*;
import de.konfetti.service.exception.ServiceException;
import de.konfetti.utils.AccountingTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Validated
public class RequestServiceImpl extends BaseService implements RequestService {

    private RequestMapper requestMapper = new RequestMapper();

    public RequestServiceImpl() {
    }

    @Autowired
    public RequestServiceImpl(PartyRepository partyRepository, RequestRepository requestRepository, AccountRepository accountRepository, MediaRepository mediaRepository) {
        this.partyRepository = partyRepository;
        this.requestRepository = requestRepository;
        this.accountRepository = accountRepository;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Request create(@NotNull Request request) {

        getPartyOrThrowError(request.getParty().getId());

        Long requestId = request.getId();
        if (requestId != null && requestId > 0) {
            throw new ServiceException(
                    String.format("The id cannot be set for create request"));
        }

        request.setId(null);

        return requestRepository.saveAndFlush(request);
    }

    @Override
    public Request update(@NotNull Request request) {
        Objects.nonNull(request);

        Party dbParty = getPartyOrThrowError(request.getParty().getId());

        Request dbRequest = getRequestOrThrowError(dbParty.getId(), request.getId());

        // update the fields TODO: could be done with entityManager merge??
        dbRequest.setTitle(request.getTitle());
        dbRequest.setImageMediaID(request.getImageMediaID());
        dbRequest.setTime(request.getTime());

        return requestRepository.saveAndFlush(dbRequest);
    }

    @Override
    public Request delete(@NotNull long requestId) {
    	
        // make sure the request exists
        Request dbRequest = requestRepository.findOne(requestId);
        if (dbRequest == null) {
            // we suppose the request was deleted before, all okay
            return null;
        }

        requestRepository.delete(dbRequest.getId());
        requestRepository.flush();
        return dbRequest;
    }

    @Override
    public List<RequestVm> getAllPartyRequests(@NotNull long partyId) {
    	List<Request> partyRequests = requestRepository.findByPartyId(partyId);
    	List<RequestVm> result = new ArrayList<>();

        for (Request requestEntity : partyRequests) {
    	    RequestVm requestVm = requestMapper.toRequestVm(requestEntity);


            // get account balance of request
            Long requestBalance = 0L;
            final String requestAccountName = AccountingTools.getAccountNameFromRequest(requestVm.getId());

            Account account = accountRepository.findByName(requestAccountName);
            requestBalance = account.getBalance();

            requestVm.setKonfettiCount(requestBalance);

            // get multi language media item
            if (requestVm.getTitleMultiLangRef()!=null) {
                requestVm.setTitleMultiLang(mediaRepository.findOne(requestVm.getTitleMultiLangRef()));
            }

            // add to result set
            result.add(requestVm);
		}
    	return result;
    }
    
    @Override
    public Request findById(long requestId) {
        return requestRepository.findOne(requestId);
    }
    

	@Override
	public Long getNumberOfRequests() {
		return requestRepository.count();
	}
}
