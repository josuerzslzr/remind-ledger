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
