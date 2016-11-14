package de.konfetti.utils;

import de.konfetti.data.Code;

import static de.konfetti.data.enums.CodeActionTypeEnum.*;

/**
 * Created by relampago on 10.10.16.
 */
public class CheatCodes {

    public Code getCodeFromCouponCode(String couponCode) {
        Code coupon = new Code();
        if (couponCode.equals("1")) {
            // add 100 Konfetti #1
            coupon = new Code();
            coupon.setAmount(100L);
            coupon.setPartyID(1L);
            coupon.setUserID(0L);
            coupon.setCode("1");
            coupon.setActionType(ACTION_TYPE_KONFETTI);
        } else if (couponCode.equals("2")) {
            // make user to normal user of party #1
            coupon = new Code();
            coupon.setPartyID(1L);
            coupon.setCode("2");
            coupon.setActionType(ACTION_TYPE_USER);
        } else if (couponCode.equals("3")){
            // upgrade user to reviewer of party #1
            coupon = new Code();
            coupon.setPartyID(1L);
            coupon.setCode("3");
            coupon.setActionType(ACTION_TYPE_REVIEWER);
        } else if (couponCode.equals("4")) {
            // upgrade user to admin of party #1
            coupon = new Code();
            coupon.setPartyID(1L);
            coupon.setCode("4");
            coupon.setActionType(ACTION_TYPE_ADMIN);
        } else if (couponCode.equals("11")) {
            // add 100 Konfetti #2
            coupon = new Code();
            coupon.setAmount(100L);
            coupon.setPartyID(2L);
            coupon.setUserID(0L);
            coupon.setCode("11");
            coupon.setActionType(ACTION_TYPE_KONFETTI);
        } else if (couponCode.equals("22")) {
            // make user to normal user of party #2
            coupon = new Code();
            coupon.setPartyID(2L);
            coupon.setCode("22");
            coupon.setActionType(ACTION_TYPE_USER);
        } else if (couponCode.equals("33")) {
            // upgrade user to admin of party #2
            coupon = new Code();
            coupon.setPartyID(2L);
            coupon.setCode("33");
            coupon.setActionType(ACTION_TYPE_REVIEWER);
        } else if (couponCode.equals("44")) {
            // upgrade user to reviewer of party #2
            coupon = new Code();
            coupon.setPartyID(2L);
            coupon.setCode("44");
            coupon.setActionType(ACTION_TYPE_ADMIN);
        } else {
            return null;
        }
        return coupon;
    }

}
