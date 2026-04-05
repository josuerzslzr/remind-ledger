package com.remindledger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Replaces {@link SecurityConfig} when running with {@code spring.profiles.active=local}.
 * Keeps the standard OAuth2 resource-server filter chain active but uses a mock
 * {@link JwtDecoder} and auto-injects a Bearer token on requests that lack one,
 * so the API can be exercised from Swagger UI or curl without a real Cognito token.
 */
@Configuration
@EnableWebSecurity
@Profile("local")
public class DevSecurityConfig {

    private static final String DEV_TOKEN = "dev-token";

    /**
     * Configures HTTP security for the local development profile and builds the resulting filter chain.
     *
     * Disables CSRF protection, permits all requests, enables OAuth2 resource-server JWT processing,
     * and registers a request filter that injects a development bearer token before the
     * BearerTokenAuthenticationFilter.
     *
     * @param http the HttpSecurity instance to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .addFilterBefore(new DevTokenInjectFilter(),
                    BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Provides a JwtDecoder for local development that constructs a Jwt from the supplied token value.
     *
     * The produced Jwt contains a fixed subject and claims (email and name) and short-lived issued/expiry timestamps.
     *
     * @return a JwtDecoder that builds a Jwt with the provided token value and deterministic development claims and timestamps
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("dev-user-00000000-0000-0000-0000-000000000000")
                .claim("email", "dev@localhost")
                .claim("name", "Dev User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    /**
     * Injects a {@code Authorization: Bearer dev-token} header on any request
     * that doesn't already carry one, so curl and Swagger UI work without a token.
     */
    private static class DevTokenInjectFilter extends OncePerRequestFilter {

        /**
         * Ensures a request has an `Authorization` header, injecting `Authorization: Bearer dev-token` when absent.
         *
         * If the incoming request lacks an `Authorization` header, the request is wrapped so calls to
         * `getHeader("Authorization")` return `Bearer ` followed by `DEV_TOKEN`; otherwise the request
         * proceeds unchanged.
         *
         * @param request the incoming HTTP request
         * @param response the HTTP response
         * @param chain the filter chain to continue processing the request
         * @throws ServletException if an error occurs during filtering
         * @throws IOException if an I/O error occurs during filtering
         */
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            if (request.getHeader("Authorization") == null) {
                chain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return "Bearer " + DEV_TOKEN;
                        }
                        return super.getHeader(name);
                    }
                }, response);
            } else {
                chain.doFilter(request, response);
            }
        }
    }
}
