package de.konfetti.controller.vm;

import lombok.Data;

/**
 * Created by relampago on 05.11.16.
 */
@Data
public class UserResponse {

    private Long id;

    private String name;

    private String eMail;

    private Long imageMediaID;

    private String[] spokenLangs = {};

    public Long[] activeOnParties = {};

    private Long[] adminOnParties = {};

    private Long[] reviewerOnParties = {};

    private Long lastActivityTS = 0l;

    private Boolean pushActive = false;

    private String pushSystem;

    private String pushID;

    private Long clientId;

    private String clientSecret;

    public UserResponse(Long id) {
        this.id = id;
    }
}
