package com.remindledger.repository;

import com.remindledger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
 * Finds a User by its Cognito `sub` identifier.
 *
 * @param cognitoSub the Cognito `sub` identifier associated with the user
 * @return an Optional containing the matching User if found, or empty otherwise
 */
Optional<User> findByCognitoSub(String cognitoSub);
}
