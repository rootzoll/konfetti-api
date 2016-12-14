package de.konfetti.service;

import de.konfetti.data.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserService {

    User create(String email, String password, String locale);

	User update(User user);
    
    User findById(long userId);

	User findByName(String name);

	User findByMailIgnoreCase(String mail);
	
	Stream<User> getAllUsersReviewerOnParty(Long partyID);

	Long getNumberOfActiveUsers();

	List<User> getAllUsers();

	Optional<User> requestPasswordReset(String mail);

	Optional<User>  completePasswordReset(String newPassword, String key);
}