package de.konfetti.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
 * Adapter to persist client objects on JPA   
 */
public interface UserRepository extends JpaRepository<User, Long> {

//    User findByClientId(Long clientId);

    User findByEMail(String email);

    User findByName(String name);

    Optional<User> findOneByEMail(String email);

    Optional<User> findOneByResetKey(String key);
}
