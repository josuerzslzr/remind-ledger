package com.remindledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remindledger.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@Profile("!local")
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    /**
     * Configures the security filter chain for non-local profiles.
     *
     * <ul>
     *   <li>CSRF disabled (stateless JWT API).</li>
     *   <li>Stateless session management.</li>
     *   <li>Health, info, and OpenAPI endpoints are public; all others require authentication.</li>
     *   <li>JWT-based OAuth2 resource server with {@link BearerTokenAuthenticationEntryPoint} (401 + WWW-Authenticate)
     *       and a custom {@link AccessDeniedHandler} for 403 responses.</li>
     *   <li>{@link UserProvisioningFilter} runs after JWT validation to ensure a User row exists.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()))
            .addFilterAfter(new UserProvisioningFilter(userService),
                    BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Returns an {@link AccessDeniedHandler} that writes a 403 JSON {@link ProblemDetail}
     * when an authenticated user lacks the required scope or permissions (RFC 6750).
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, ex) -> {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "Insufficient scope or permissions");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), pd);
        };
    }
}
