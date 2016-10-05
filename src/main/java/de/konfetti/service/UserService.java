package de.konfetti.service;

import java.util.List;
import java.util.Optional;

import de.konfetti.data.User;

public interface UserService {

    User create();

	User update(User user);
    
    User findById(long userId);
    
    User findByClientId(long clientId);

	User findByMail(String mail);
	
	List<User> getAllUsersAdminOnParty(Long partyID);

	List<User> getAllUsersReviewerOnParty(Long partyID);

	Long getNumberOfActiveUsers();

	List<User> getAllUsers();

	Optional<User> requestPasswordReset(String mail);
}