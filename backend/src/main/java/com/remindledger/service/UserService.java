package com.remindledger.service;

import com.remindledger.model.User;
import com.remindledger.repository.UserRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds the user by Cognito {@code sub}, or auto-provisions a new row
     * on first login using claims from the JWT.
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
