package com.remindledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!local")
public class SecurityConfig {

    /**
     * Configures the application's HTTP security and returns the built SecurityFilterChain.
     *
     * Configures CSRF as disabled, session management as stateless, permits unauthenticated access to
     * /actuator/** and OpenAPI/Swagger endpoints, requires authentication for all other requests,
     * enables JWT-based OAuth2 resource server support, and uses the provided AuthenticationEntryPoint
     * for authentication failures.
     *
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint authEntryPoint) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authEntryPoint));

        return http.build();
    }

    /**
     * Creates an AuthenticationEntryPoint that writes a JSON ProblemDetail and sets HTTP status 401 when authentication fails.
     *
     * <p>The response detail message is derived from the authentication exception:
     * "invalid_token" -> "Token is invalid or expired",
     * "insufficient_scope" -> "Token has insufficient scope",
     * otherwise the provider description is used if present, falling back to "Authentication required".</p>
     *
     * @return an AuthenticationEntryPoint that writes a 401 JSON ProblemDetail with a context-specific detail message
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            String detail = "Authentication required";
            if (ex instanceof OAuth2AuthenticationException oauthEx) {
                detail = switch (oauthEx.getError().getErrorCode()) {
                    case "invalid_token" -> "Token is invalid or expired";
                    case "insufficient_scope" -> "Token has insufficient scope";
                    default -> oauthEx.getError().getDescription() != null
                            ? oauthEx.getError().getDescription()
                            : detail;
                };
            }
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), pd);
        };
    }
}
