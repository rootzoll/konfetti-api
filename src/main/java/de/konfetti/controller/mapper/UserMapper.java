package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.UserResponse;
import de.konfetti.data.User;

import java.util.Arrays;
import java.util.HashSet;

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
        userResponse.setAdminOnParties(new HashSet<>(Arrays.asList(user.getAdminOnParties())));
        userResponse.setReviewerOnParties(new HashSet<>(Arrays.asList(user.getReviewerOnParties())));
        userResponse.setLastActivityTS(user.getLastActivityTS());
        userResponse.setPushActive(user.getPushActive());
        userResponse.setPushSystem(user.getPushSystem());
        userResponse.setPushID(user.getPushID());
        return userResponse;
    }

}
