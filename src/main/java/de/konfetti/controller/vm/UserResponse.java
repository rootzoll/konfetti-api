package de.konfetti.controller.vm;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by relampago on 05.11.16.
 */
@Data
public class UserResponse {

    private Long id;

    private String name;

    private String eMail;

    private Long imageMediaID;

    private Set<String> spokenLangs = new HashSet<>();

    public Set<Long> activeOnParties = new HashSet<>();

    private Set<Long> adminOnParties = new HashSet<>();

    private Set<Long> reviewerOnParties = new HashSet<>();

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
