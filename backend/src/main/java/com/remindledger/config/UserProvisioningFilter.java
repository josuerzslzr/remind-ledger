package com.remindledger.config;

import com.remindledger.model.User;
import com.remindledger.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after JWT validation. Ensures a {@link User} row exists for the
 * authenticated Cognito {@code sub} and stores it as a request attribute
 * so controllers can retrieve it without repeating the lookup.
 */
public class UserProvisioningFilter extends OncePerRequestFilter {

    public static final String USER_ATTRIBUTE = UserProvisioningFilter.class.getName() + ".USER";

    private final UserService userService;

    public UserProvisioningFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {
            String sub = jwt.getToken().getSubject();
            String email = jwt.getToken().getClaimAsString("email");
            String name = jwt.getToken().getClaimAsString("name");

            User user = userService.provisionUser(sub, email, name);
            request.setAttribute(USER_ATTRIBUTE, user);
        }

        chain.doFilter(request, response);
    }
}
