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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    public User create(String email, String password, String locale) {
        User user = new User();
        user.setEMail(email.toLowerCase().trim());
        String passMD5 = Helper.hashPassword(this.passwordSalt, password.trim());
        user.setPassword(passMD5);

        // set default spoken lang
        log.info("set default spoken lang");
        String[] langs = {locale};
        user.setSpokenLangs(langs);
        user.setLastActivityTS(System.currentTimeMillis());

        log.info("Create new User with eMail(" + email + ") and passwordhash(" + passMD5 + ")");
        return userRepository.saveAndFlush(user);
    }

    @Override
    public User createGuest(String locale) {
        User user = new User();

        // set default spoken lang
        log.info("set default spoken lang");
        String[] langs = {locale};
        user.setSpokenLangs(langs);
        user.setLastActivityTS(System.currentTimeMillis());

        log.info("Create new Guest ");
        return userRepository.saveAndFlush(user);
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
    public User findByMailIgnoreCase(String email) {
        return userRepository.findByEMail(email.toLowerCase());
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
