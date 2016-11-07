package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.UserResponse;
import de.konfetti.data.User;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by relampago on 05.11.16.
 */
public class UserMapper {

    public UserResponse fromUserToUserResponse(User user) {
        UserResponse userResponse = new UserResponse(user.getId());
        userResponse.setName(user.getName());
        userResponse.setEMail(user.getEMail());
        userResponse.setImageMediaID(user.getImageMediaID());
        userResponse.setSpokenLangs(new HashSet<>(Arrays.asList(user.getSpokenLangs())));
        userResponse.setActiveOnParties(new HashSet<>(Arrays.asList(user.getActiveOnParties())));
        Set<Long> adminOnParties = user.getAdminParties().stream().map(party -> party.getId()).collect(Collectors.toSet());
        userResponse.setAdminOnParties(adminOnParties);
        userResponse.setReviewerOnParties(new HashSet<>(Arrays.asList(user.getReviewerOnParties())));
        userResponse.setLastActivityTS(user.getLastActivityTS());
        userResponse.setPushActive(user.getPushActive());
        userResponse.setPushSystem(user.getPushSystem());
        userResponse.setPushID(user.getPushID());
        return userResponse;
    }

}
