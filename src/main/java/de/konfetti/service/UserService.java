package de.konfetti.service;

import de.konfetti.data.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User create();

	User update(User user);
    
    User findById(long userId);

	User findByName(String name);

	User findByMail(String mail);
	
	List<User> getAllUsersReviewerOnParty(Long partyID);

	Long getNumberOfActiveUsers();

	List<User> getAllUsers();

	Optional<User> requestPasswordReset(String mail);

	Optional<User>  completePasswordReset(String newPassword, String key);
}