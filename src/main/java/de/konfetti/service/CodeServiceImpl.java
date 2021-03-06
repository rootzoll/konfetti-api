package de.konfetti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.konfetti.controller.mapper.UserMapper;
import de.konfetti.controller.vm.RedeemResponse;
import de.konfetti.data.*;
import de.konfetti.utils.AccountingTools;
import de.konfetti.utils.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static de.konfetti.data.enums.CodeActionTypeEnum.*;

@Slf4j
@Service
public class CodeServiceImpl extends BaseService implements CodeService {

    @Autowired
    PartyService partyService;

    @Autowired
    AccountingService accountingService;

    @Autowired
    UserService userService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    public CodeServiceImpl(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    @Override
    public Code createKonfettiCoupon(Long partyID, Long userID, Long konfettiAmount) {
        Code code = new Code();
        code.setPartyID(partyID);
        code.setActionType(ACTION_TYPE_KONFETTI);
        code.setAmount(konfettiAmount);
        code.setUserID(userID);
        return saveWithUniqueCode(code);
    }

    @Override
    public Code createAdminCode(Long partyID) {
        Code code = new Code();
        code.setPartyID(partyID);
        code.setActionType(ACTION_TYPE_ADMIN);
        return saveWithUniqueCode(code);
    }

    @Override
    public Code createReviewCode(Long partyID) {
        Code code = new Code();
        code.setPartyID(partyID);
        code.setActionType(ACTION_TYPE_REVIEWER);
        return saveWithUniqueCode(code);
    }

    @Override
    public Code redeemByCode(String code) {
        Code codeObject = findByCode(code);
        if (codeObject == null) return null;
        this.codeRepository.delete(codeObject.getId());
        return codeObject;
    }

    @Override
    public Code findByCode(String code) {
        return codeRepository.findByCode(code);
    }

    @Override
    public RedeemResponse processCodeCoupon(User user, Code coupon, String locale) {
        log.info("processCodeCoupon for user '" + user.getName() + "' with couponCode '" + coupon.getCode() + "'");
    	
        RedeemResponse result = new RedeemResponse();
        Party foundParty = partyService.findById(coupon.getPartyID());
        String redeemMessageCode = null;
        Long[] redeemMessageVars = null;

        // redeem konfetti
        if (ACTION_TYPE_KONFETTI == coupon.getActionType()) {
            // add konfetti to party
            result.setActions(addKonfettiOnParty(user, coupon.getPartyID(), coupon.getAmount(), result.getActions()));

            // get GPS from party
            ClientAction gpsInfo = new ClientAction();
            gpsInfo.command = "gpsInfo";
            gpsInfo.json = "{\"lat\":" + foundParty.getLat() + ", \"lon\":" + foundParty.getLon() + "}";
            result.getActions().add(gpsInfo);

            redeemMessageCode = "redeem.code.konfetti";
            redeemMessageVars = new Long[]{coupon.getAmount()};
        } else if (ACTION_TYPE_REVIEWER == coupon.getActionType()) {
            // promote user to reviewer
            result.setActions(makeUserReviewerOnParty(user, foundParty, result.getActions()));
            redeemMessageCode = "redeem.code.reviewer";
        } else if (ACTION_TYPE_ADMIN == coupon.getActionType()) {
            // promote user to admin
            List<ClientAction> actions = makeUserAdminOnParty(user, foundParty, result.getActions());
            result.setActions(actions);
            redeemMessageCode = "redeem.code.admin";
        }  else if (ACTION_TYPE_USER == coupon.getActionType()) {
            // promote user to admin
            List<ClientAction> actions = makeUserNormaloOnParty(user, foundParty, result.getActions());
            result.setActions(actions);
            redeemMessageCode = "redeem.code.user";
        }
        result.setFeedbackHtml(messageSource.getMessage(redeemMessageCode, redeemMessageVars, Locale.forLanguageTag(locale)));
        return result;
    }

    private List<ClientAction> addKonfettiOnParty(User user, Long partyId, Long konfettiAmount, List<ClientAction> actions)  {
       
    	Party party = partyService.findById(partyId);
        Objects.nonNull(party);
        final String userAccountName = AccountingTools.getAccountNameFromUserAndParty(user.getId(), partyId);

        // add user to party if not already part of
        if (!user.getActiveParties().contains(party)) {
            this.accountingService.createAccount(userAccountName);
            user.getActiveParties().add(party);
            this.userService.update(user);
            
    		// check if user should get granted welcome konfetti additional to coupon
        	if (party.getWelcomeBalance()>0) {
        		this.accountingService.addBalanceToAccount(TransactionType.USER_WELCOME, userAccountName, party.getWelcomeBalance());
        	}
        }

        // add konfetti to account
        Long konfettiBefore = this.accountingService.getBalanceOfAccount(userAccountName);
        Long konfettiAfter = this.accountingService.addBalanceToAccount(TransactionType.COUPON, userAccountName, konfettiAmount);

        // security check - just for the paranoid
        if (konfettiBefore.equals(konfettiAfter)) {
            log.warn("addKonfettiOnParty, same amount as before! konfettiAmount: " + konfettiAmount + " , konfettiBefore : " + konfettiBefore + " , konfettiAfter : " + konfettiAfter);
        }
        
        log.info("user(" + user.getId() + ") on party(" + partyId + ") +" + konfettiAmount + " konfetti");
        actions = addFocusPartyAction(actions, partyId);
        return actions;
    }

    private List<ClientAction> makeUserAdminOnParty(User user, Party party, List<ClientAction> actions) {
        Objects.nonNull(party);
        log.info("make user '" + user.getName() + "' admin on party  '" + party.getName() + "'");

        user.setReviewerParties(new ArrayList<>());
        List<Party> adminParties = user.getAdminParties();
        if (adminParties.contains(party)){
            log.warn("user(" + user.getId() + ") is ALREADY ADMIN on party(" + party.getId() + ")");
        } else {
            log.info("user(" + user.getId() + ") is now ADMIN on party(" + party.getId() + ")");
            adminParties.add(party);
            userService.update(user);
        }
        actions = addUpdateUserAction(actions, user);
        actions = addFocusPartyAction(actions, party.getId());
        return actions;
    }

    private List<ClientAction> makeUserReviewerOnParty(User user, Party party, List<ClientAction> actions) {
        Objects.nonNull(party);
        log.info("make user '" + user.getName() + "' reviewer on party  '" + party.getName() + "'");


        user.setAdminParties(new ArrayList<>());
        if (user.getReviewerParties().contains(party)){
            log.debug("user : " + user.getName() + " is already reviewer on partyId : " + party.getId());
            return actions;
        }

        user.getReviewerParties().add(party);
        userService.update(user);

        log.info("user(" + user.getId() + ") is now REVIEWER on party(" + party.getId() + ")");

        actions = addUpdateUserAction(actions, user);
        actions = addFocusPartyAction(actions, party.getId());
        return actions;
    }


    private List<ClientAction> makeUserNormaloOnParty(User user, Party party, List<ClientAction> actions) {
        Objects.nonNull(party);
        log.info("make user '" + user.getName() + "' normalo on party  '" + party.getName() + "'");


        // remove admin und reviewer from user
        user.setAdminParties(new ArrayList<>());
        user.setReviewerParties(new ArrayList<>());

        log.info("user(" + user.getId() + ") is now USER on party(" + party.getId() + ")");

        userService.update(user);

        actions = addUpdateUserAction(actions, user);
        actions = addFocusPartyAction(actions, party.getId());
        return actions;
    }

    private List<ClientAction> addFocusPartyAction(List<ClientAction> actions, Long partyId) {
        ClientAction a = new ClientAction();
        a.command = "focusParty";
        a.json = "" + partyId;
        actions.add(a);
        return actions;
    }

    private List<ClientAction> addUpdateUserAction(List<ClientAction> actions, User actualUser) {
        String userJson = null;
        try {
            userJson = new ObjectMapper().writeValueAsString(new UserMapper().fromUserToUserResponse(actualUser));
        } catch (Exception e) {
            e.printStackTrace();
        }
        ClientAction a = new ClientAction();
        a.command = "updateUser";
        a.json = userJson;
        actions.add(a);
        return actions;
    }


    private Code saveWithUniqueCode(Code code) {
        Code result = null;
        int count = 0;
        do {
            count++;
            try {
                code.setCode("" + RandomUtil.generadeCodeNumber());
                result = saveWhenCodeUnique(code);
            } catch (Exception e) {
                log.warn("Was not able to use code ... will try again");
            }
        } while ((result == null) && (count < 100));
        if (count >= 100) log.error("Even afer 100 tries ... not unique code found.");
        return result;
    }

    private synchronized Code saveWhenCodeUnique(Code code) throws Exception {
        if (findByCode(code.getCode()) != null) throw new Exception("code(" + code.getCode() + ") already in use");
        return this.codeRepository.saveAndFlush(code);
    }


}
