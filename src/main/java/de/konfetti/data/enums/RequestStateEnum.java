package de.konfetti.data.enums;

/**
 * Created by relampago on 10.10.16.
 */
public enum RequestStateEnum {

    STATE_REVIEW("review"),
    STATE_REJECTED("rejected"),
    STATE_OPEN("open"),
    STATE_PROCESSING("processing"),
    STATE_DONE("done");

    private String text;

    RequestStateEnum(String text) {
        this.text = text;
    }

    public static RequestStateEnum byText(String text) {
        for (RequestStateEnum c : RequestStateEnum.values()) {
            if (c.text.equals(text)) {
                return c;
            }
        }
        return null;
    }


}
