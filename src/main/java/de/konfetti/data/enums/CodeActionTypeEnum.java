package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum CodeActionTypeEnum {

    ACTION_TYPE_KONFETTI(0), ACTION_TYPE_ADMIN(1), ACTION_TYPE_REVIEWER(2), ACTION_TYPE_USER(3);

    private int ordinal;

    CodeActionTypeEnum(int ordinal) {
        this.ordinal = ordinal;
    }

    public static CodeActionTypeEnum byOrdinal(int ord) {
        for (CodeActionTypeEnum c : CodeActionTypeEnum.values()) {
            if (c.ordinal == ord) {
                return c;
            }
        }
        return null;
    }


}
