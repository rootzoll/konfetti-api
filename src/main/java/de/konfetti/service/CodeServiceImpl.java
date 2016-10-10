package de.konfetti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.konfetti.controller.vm.RedeemResponse;
import de.konfetti.data.*;
import de.konfetti.utils.AccountingTools;
import de.konfetti.utils.Helper;
import de.konfetti.utils.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.konfetti.data.enums.CodeActionTypeEnum.ACTION_TYPE_ADMIN;
import static de.konfetti.data.enums.CodeActionTypeEnum.ACTION_TYPE_KONFETTI;
import static de.konfetti.data.enums.CodeActionTypeEnum.ACTION_TYPE_REVIEWER;

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
    public RedeemResponse processCodeCoupon(User user, Code coupon) throws Exception {
        RedeemResponse result = new RedeemResponse();
        // redeem konfetti
        if (ACTION_TYPE_KONFETTI == coupon.getActionType()) {
            // add konfetti to party
            result.setActions(addKonfettiOnParty(user, coupon.getPartyID(), coupon.getAmount(), result.getActions()));

            // get GPS from party
            Party party = this.partyService.findById(coupon.getPartyID());
            ClientAction gpsInfo = new ClientAction();
            gpsInfo.command = "gpsInfo";
            gpsInfo.json = "{\"lat\":" + party.getLat() + ", \"lon\":" + party.getLon() + "}";
            result.getActions().add(gpsInfo);

            // TODO --> multi lang by lang set in user
            result.setFeedbackHtml("You got now " + coupon.getAmount() + " konfetti to create a task with or upvote other ideas.");
        } else if (ACTION_TYPE_REVIEWER == coupon.getActionType()) {
            // promote user to reviewer
            result.setActions(makeUserReviewerOnParty(user, coupon.getPartyID(), result.getActions()));
            // TODO --> multi lang by lang set in user
            result.setFeedbackHtml("You are now REVIEWER on the following party.");
        } else if (ACTION_TYPE_ADMIN == coupon.getActionType()) {
            // promote user to admin
            result.setActions(makeUserAdminOnParty(user, coupon.getPartyID(), result.getActions()));
            // TODO --> multi lang by lang set in user
            result.setFeedbackHtml("You are now ADMIN on the following party.");
        }
        return result;
    }

    private List<ClientAction> addKonfettiOnParty(User user, Long partyId, Long konfettiAmount, List<ClientAction> actions) throws Exception {

        final String userAccountName = AccountingTools.getAccountNameFromUserAndParty(user.getId(), partyId);

        // add user to party if not already part of
        if (!Helper.contains(user.activeOnParties, partyId)) {
            user.activeOnParties = Helper.append(user.activeOnParties, partyId);
            this.accountingService.createAccount(userAccountName);
            this.userService.update(user);
        }

        Long konfettiBefore = this.accountingService.getBalanceOfAccount(userAccountName);
        Long konfettiAfter = this.accountingService.addBalanceToAccount(TransactionType.COUPON, userAccountName, konfettiAmount);

        if (konfettiBefore.equals(konfettiAfter)) throw new Exception("adding amount failed");

        log.info("user(" + user.getId() + ") on party(" + partyId + ") +" + konfettiAmount + " konfetti");

        actions = addFocusPartyAction(actions, partyId);

        return actions;
    }

    private List<ClientAction> makeUserAdminOnParty(User user, Long partyId, List<ClientAction> actions) {
        Long[] arr = user.getAdminOnParties();
        if (!Helper.contains(arr, partyId)) arr = Helper.append(arr, partyId);
        user.setAdminOnParties(arr);
        userService.update(user);

        log.info("user(" + user.getId() + ") is now ADMIN on party(" + partyId + ")");

        actions = addUpdateUserAction(actions, user);
        actions = addFocusPartyAction(actions, partyId);
        return actions;
    }

    private List<ClientAction> makeUserReviewerOnParty(User user, Long partyId, List<ClientAction> actions) {

        Long[] arr = user.getReviewerOnParties();
        if (!Helper.contains(arr, partyId)) arr = Helper.append(arr, partyId);
        user.setReviewerOnParties(arr);
        userService.update(user);

        log.info("user(" + user.getId() + ") is now REVIEWER on party(" + partyId + ")");

        actions = addUpdateUserAction(actions, user);
        actions = addFocusPartyAction(actions, partyId);

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
            userJson = new ObjectMapper().writeValueAsString(actualUser);
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
