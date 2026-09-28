package com.example.microservice_contract.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtUtil jwtUtil;  // inject JwtUtil instead

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/contracts/ping").permitAll()
                        .requestMatchers("/api/contracts/debug-auth").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contracts").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/contracts/*/signatures/can-sign").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/contracts/*/signatures/auth-status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contracts/*/signatures/sign").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/contracts/*/verify").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/contracts/client/**").hasAnyAuthority("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/contracts/freelancer/**").hasAnyAuthority("FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/contracts/*/extensions").hasAnyAuthority("CLIENT", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/contracts/*/extensions/*").hasAnyAuthority("CLIENT", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.POST,  "/api/contracts/*/extensions").hasAnyAuthority("CLIENT", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/contracts/*/extensions/*/review").hasAnyAuthority("CLIENT", "FREELANCER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/contracts/*/status").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/contracts/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}