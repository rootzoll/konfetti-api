package de.konfetti.controller.mapper;

import de.konfetti.controller.vm.UserResponse;
import de.konfetti.data.User;

/**
 * Created by relampago on 05.11.16.
 */
public class UserMapper {

    public UserResponse fromUserToUserResponse(User user) {
        UserResponse userResponse = new UserResponse(user.getId());
        userResponse.setName(user.getName());
        userResponse.setEMail(user.getEMail());
        userResponse.setImageMediaID(user.getImageMediaID());
        userResponse.setSpokenLangs(user.getSpokenLangs());
        userResponse.setActiveOnParties(user.getActiveOnParties());
        userResponse.setAdminOnParties(user.getAdminOnParties());
        userResponse.setReviewerOnParties(user.getReviewerOnParties());
        userResponse.setLastActivityTS(user.getLastActivityTS());
        userResponse.setPushActive(user.getPushActive());
        userResponse.setPushSystem(user.getPushSystem());
        userResponse.setPushID(user.getPushID());
        return userResponse;
    }

}
