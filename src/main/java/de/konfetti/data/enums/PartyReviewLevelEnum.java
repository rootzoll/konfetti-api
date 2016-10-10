package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum PartyReviewLevelEnum {

    // 0 = no review
    // 1 = full review of all public posts
    // 2 = just review the initial task, follow up public info on request no review

    REVIEWLEVEL_NONE(0), REVIEWLEVEL_EVERYTHING(1), REVIEWLEVEL_TASKS(2);

    private int ordinal;

    PartyReviewLevelEnum(int ordinal) {
        this.ordinal = ordinal;
    }

    public static PartyReviewLevelEnum byOrdinal(int ord) {
        for (PartyReviewLevelEnum c : PartyReviewLevelEnum.values()) {
            if (c.ordinal == ord) {
                return c;
            }
        }
        return null;
    }


}
