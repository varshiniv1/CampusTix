package com.university.campustix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${admin.username:username}")
    private String adminUsername;

    @Value("${admin.password:password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(form -> form
                .loginProcessingUrl("/api/v1/admin/login")
                .successHandler((req, res, auth) -> res.setStatus(200))
                .failureHandler((req, res, ex) -> res.setStatus(401))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/admin/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                .permitAll()
            )
            .authorizeHttpRequests(auth -> auth
                // Admin API — requires login
                .requestMatchers(HttpMethod.GET,  "/api/v1/admin/auth").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/v1/admin/analytics").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/events").hasRole("ADMIN")
                // Swagger UI — open
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Actuator — open (Prometheus scrapes this)
                .requestMatchers("/actuator/**").permitAll()
                // Everything else — open
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(passwordEncoder().encode(adminPassword))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
