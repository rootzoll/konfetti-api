package de.konfetti.utils;

import de.konfetti.data.Code;

/**
 * Created by relampago on 10.10.16.
 */
public class CheatCodes {

    public Code getCodeFromCouponCode(String couponCode) {
        Code coupon = new Code();
        if (couponCode.equals("1")) {
            // add 100 Konfetto #1
            coupon = new Code();
            coupon.setAmount(100l);
            coupon.setPartyID(1l);
            coupon.setUserID(0l);
            coupon.setCode("1");
            coupon.setActionType(Code.ACTION_TYPE_KONFETTI);
        } else if (couponCode.equals("111")) {
            // upgrade user to admin of party #1
            coupon = new Code();
            coupon.setPartyID(1l);
            coupon.setCode("111");
            coupon.setActionType(Code.ACTION_TYPE_ADMIN);
        } else if (couponCode.equals("11")) {
            // upgrade user to reviewer of party #1
            coupon = new Code();
            coupon.setPartyID(1l);
            coupon.setCode("11");
            coupon.setActionType(Code.ACTION_TYPE_REVIEWER);
        } else if (couponCode.equals("2")) {
            // add 100 Konfetto #2
            coupon = new Code();
            coupon.setAmount(100l);
            coupon.setPartyID(2l);
            coupon.setUserID(0l);
            coupon.setCode("2");
            coupon.setActionType(Code.ACTION_TYPE_KONFETTI);
        } else if (couponCode.equals("222")) {
            // upgrade user to admin of party #2
            coupon = new Code();
            coupon.setPartyID(2l);
            coupon.setCode("222");
            coupon.setActionType(Code.ACTION_TYPE_ADMIN);
        } else if (couponCode.equals("22")) {
            // upgrade user to reviewer of party #2
            coupon = new Code();
            coupon.setPartyID(2l);
            coupon.setCode("22");
            coupon.setActionType(Code.ACTION_TYPE_REVIEWER);
        }
        return coupon;
    }

}
