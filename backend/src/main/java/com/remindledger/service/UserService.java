package com.remindledger.service;

import com.remindledger.model.User;
import com.remindledger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    /**
     * Creates a UserService backed by the provided UserRepository.
     *
     * @param userRepository repository used to query and persist User entities
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Ensures a user row exists for the given Cognito {@code sub}.
     * Creates one if missing; updates email/name if provided and changed.
     * Called by {@link com.remindledger.config.UserProvisioningFilter}.
     */
    @Transactional
    public User provisionUser(String sub, String email, String name) {
        return userRepository.findByCognitoSub(sub)
                .map(existing -> {
                    boolean updated = false;
                    if (email != null && !email.equals(existing.getEmail())) {
                        existing.setEmail(email);
                        updated = true;
                    }
                    if (name != null && !name.equals(existing.getDisplayName())) {
                        existing.setDisplayName(name);
                        updated = true;
                    }
                    return updated ? userRepository.save(existing) : existing;
                })
                .orElseGet(() -> userRepository.save(new User(sub, email, name)));
    }

    /**
     * Finds the user by Cognito {@code sub}.
     * Assumes the user has already been provisioned by the filter.
     */
    public User getBySub(String sub) {
        return userRepository.findByCognitoSub(sub)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found for sub: " + sub + ". UserProvisioningFilter should have created it."));
    }
}
