package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum SendKonfettiModeEnum {

    // 0: sending of konfetti to other users is disabled
    // 1: all konfetti can be send to other users/e-mails
    // 2: just earned konfetti can be send to other users/e-mails

    SENDKONFETTIMODE_DISABLED(0), SENDKONFETTIMODE_ALL(1), SENDKONFETTIMODE_JUSTEARNED(2);

    private int ordinal;

    SendKonfettiModeEnum(int ordinal) {
        this.ordinal = ordinal;
    }

    public static SendKonfettiModeEnum byOrdinal(int ord) {
        for (SendKonfettiModeEnum c : SendKonfettiModeEnum.values()) {
            if (c.ordinal == ord) {
                return c;
            }
        }
        return null;
    }


}
