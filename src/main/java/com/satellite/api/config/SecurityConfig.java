package com.satellite.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Satellite API.
 *
 * <p>Rules:
 * <ul>
 *   <li>GET endpoints are public — anyone can read satellite data</li>
 *   <li>POST, PUT, DELETE require authentication (HTTP Basic)</li>
 *   <li>Swagger UI and OpenAPI docs remain publicly accessible</li>
 * </ul>
 *
 * <p>Admin credentials are read from environment variables
 * ({@code ADMIN_USERNAME}, {@code ADMIN_PASSWORD}), defaulting to
 * {@code admin/admin} for local development.
 */
@Configuration
public class SecurityConfig {

    private static final String SATELLITES_API_PATTERN = "/api/satellites/**";
    private static final String[] PUBLIC_DOCS_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public read access
                .requestMatchers(HttpMethod.GET, SATELLITES_API_PATTERN).permitAll()

                // Write operations require authentication
                .requestMatchers(HttpMethod.POST, SATELLITES_API_PATTERN).authenticated()
                .requestMatchers(HttpMethod.PUT, SATELLITES_API_PATTERN).authenticated()
                .requestMatchers(HttpMethod.DELETE, SATELLITES_API_PATTERN).authenticated()

                // Swagger UI and OpenAPI docs
                .requestMatchers(PUBLIC_DOCS_PATHS).permitAll()

                // Everything else
                .anyRequest().authenticated()
            )
            // HTTP Basic authentication
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
