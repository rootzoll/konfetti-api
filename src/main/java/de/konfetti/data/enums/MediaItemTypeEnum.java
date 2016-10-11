package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum MediaItemTypeEnum {

    TYPE_UNKOWN("n/a"),
    TYPE_TEXT("java.lang.String"),
    TYPE_MULTILANG("MediaItemMultiLang"),
    TYPE_LOCATION("Location"),
    TYPE_IMAGE("Image"),
    TYPE_DATE("Date");

    private String text;

    MediaItemTypeEnum(String text) {
        this.text = text;
    }

    public static MediaItemTypeEnum byText(String text) {
        for (MediaItemTypeEnum c : MediaItemTypeEnum.values()) {
            if (c.text.equals(text)) {
                return c;
            }
        }
        return null;
    }


}
