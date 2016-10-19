package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum PartyVisibilityEnum {

    // 0 = default - for everybody to see
    // 1 = can be found but is asking for invitation code
    // 2 = cannot be found, just enter with invitation code
    // -1 = deactivated

    VISIBILITY_PUBLIC(0), VISIBILITY_PRIVATE(1), VISIBILITY_HIDDEN(2), VISIBILITY_DEACTIVATED(-1);

    private int ordinal;

    PartyVisibilityEnum(int ordinal) {
        this.ordinal = ordinal;
    }

    public static PartyVisibilityEnum byOrdinal(int ord) {
        for (PartyVisibilityEnum c : PartyVisibilityEnum.values()) {
            if (c.ordinal == ord) {
                return c;
            }
        }
        return null;
    }


}
