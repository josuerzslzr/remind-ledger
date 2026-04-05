package com.remindledger.service;

import com.remindledger.model.User;
import com.remindledger.repository.UserRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
     * Locate a User by the Cognito subject (`sub`) from the provided JWT or create and persist a new User using claims from that JWT when none exists.
     *
     * @param auth the JwtAuthenticationToken containing the Cognito JWT; the method reads the token subject as the Cognito `sub` and the `email` and `name` claims for provisioning
     * @return the existing User if one was found for the Cognito `sub`, otherwise a newly created and saved User built from the token's `sub`, `email`, and `name` claims
     */
    @Transactional
    public User getOrCreateUser(JwtAuthenticationToken auth) {
        String sub = auth.getToken().getSubject();
        return userRepository.findByCognitoSub(sub)
                .orElseGet(() -> {
                    String email = auth.getToken().getClaimAsString("email");
                    String name = auth.getToken().getClaimAsString("name");
                    return userRepository.save(new User(sub, email, name));
                });
    }
}
