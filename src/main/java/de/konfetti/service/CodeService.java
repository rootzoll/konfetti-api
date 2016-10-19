package de.konfetti.service;

import de.konfetti.controller.vm.RedeemResponse;
import de.konfetti.data.Code;
import de.konfetti.data.User;

public interface CodeService {

    Code createKonfettiCoupon(Long partyID, Long userID, Long konfettiAmount);
    
    Code createAdminCode(Long partyID);
    
    Code createReviewCode(Long partyID);

    // throws exception if code is not valid
    Code redeemByCode(String code) throws Exception;

    Code findByCode(String code);

    RedeemResponse processCodeCoupon(User user, Code code) throws Exception;
}