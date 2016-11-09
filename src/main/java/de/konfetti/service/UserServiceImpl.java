package de.konfetti.service;

import de.konfetti.data.Party;
import de.konfetti.data.PartyRepository;
import de.konfetti.data.User;
import de.konfetti.data.UserRepository;
import de.konfetti.utils.Helper;
import de.konfetti.utils.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class UserServiceImpl extends BaseService implements UserService {

    @Value("${security.passwordsalt}")
    private String passwordSalt;

    public UserServiceImpl() {
    }

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PartyRepository partyRepository) {
        this.userRepository = userRepository;
        this.partyRepository = partyRepository;
    }

    @Override
    public User create() {
        // user gets created
        User user = new User();

        // user gets persisted and returned to user  
        User persited = userRepository.saveAndFlush(user);

        // return to caller
        return persited;
    }

    @Override
    public User findById(long id) {
        // gets the one with the given id
        return userRepository.findOne(id);
    }

    @Override
    public User findByName(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public User update(User user) {
        return userRepository.saveAndFlush(user);
    }

    @Override
    public User findByMail(String mail) {
        return userRepository.findByEMail(mail);
    }

    @Override
    public Stream<User> getAllUsersReviewerOnParty(Long partyID) {
        HashSet<User> result = new HashSet<>();
        Party party = partyRepository.findOne(partyID);
        if (party != null) {
            // admin = reviewer
            result.addAll(party.getReviewerUser());
            result.addAll(party.getAdminUsers());
        }
        return result.stream();
    }

    // TODO improve performance
    @Override
    public Long getNumberOfActiveUsers() {
        Long count = 0L;
        List<User> all = userRepository.findAll();
        long tsAWeekAgo = System.currentTimeMillis() - (7l * 24l * 60l * 60l * 1000l);
        for (User user : all) {
            if (user.getLastActivityTS() > tsAWeekAgo) count++;
        }
        return count;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> requestPasswordReset(String email) {
        return userRepository.findOneByEMail(email)
//				.filter(User::isActive)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(ZonedDateTime.now());
                    userRepository.save(user);
                    return user;
                });
    }

    @Override
    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);

        return userRepository.findOneByResetKey(key)
                .filter(user -> {
                    ZonedDateTime oneDayAgo = ZonedDateTime.now().minusHours(24);
                    return user.getResetDate().isAfter(oneDayAgo);
                })
                .map(user -> {
                    user.setPassword(Helper.hashPassword(this.passwordSalt, newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    userRepository.save(user);
                    return user;
                });
    }

}
