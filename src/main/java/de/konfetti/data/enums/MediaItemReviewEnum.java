package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum MediaItemReviewEnum {

    REVIEWED_PUBLIC(0), REVIEWED_PRIVATE(1);

    private int ordinal;

    MediaItemReviewEnum(int ordinal) {
        this.ordinal = ordinal;
    }

    public static MediaItemReviewEnum byOrdinal(int ord) {
        for (MediaItemReviewEnum c : MediaItemReviewEnum.values()) {
            if (c.ordinal == ord) {
                return c;
            }
        }
        return null;
    }


}
