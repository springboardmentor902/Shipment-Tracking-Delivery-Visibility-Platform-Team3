package com.shiptrack.shiptrack_pro.config;

import com.shiptrack.shiptrack_pro.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                    // =========================
                    // Authentication
                    // =========================
                    .requestMatchers("/api/auth/**")
                    .permitAll()

                    // =========================
                    // WebSocket
                    // =========================
                    .requestMatchers("/api/ws/tracking/**")
                    .permitAll()

                    // =========================
                    // Shipments
                    // =========================
                    .requestMatchers(HttpMethod.POST, "/api/shipments")
                    .hasAnyRole(
                            "CUSTOMER",
                            "BUSINESS_CLIENT"
                    )

                    // =========================
                    // Routes & Tracking
                    // =========================
                    .requestMatchers(
                            "/api/tracking/**",
                            "/api/routes/**"
                    )
                    .hasAnyRole(
                            "LOGISTICS_OPERATOR",
                            "ADMINISTRATOR"
                    )

                    // =========================
                    // Driver Location
                    // =========================
                    .requestMatchers("/api/route/**")
                    .hasAnyRole(
                            "LOGISTICS_OPERATOR",
                            "ADMINISTRATOR"
                    )

                    // =========================
                    // ETA Prediction
                    // =========================
                    .requestMatchers("/api/eta/**")
                    .authenticated()

                    // =========================
                    // Proof of Delivery
                    // =========================
                    .requestMatchers(HttpMethod.POST, "/api/pod/**")
                    .hasRole("LOGISTICS_OPERATOR")

                    // =========================
                    // Analytics & Reports
                    // =========================
                    .requestMatchers(
                            "/api/analytics/**",
                            "/api/reports/**"
                    )
                    .hasAnyRole(
                            "BUSINESS_CLIENT",
                            "ADMINISTRATOR"
                    )

                    // =========================
                    // Admin
                    // =========================
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMINISTRATOR")

                    // =========================
                    // Everything else
                    // =========================
                    .anyRequest()
                    .authenticated()
            )

            .httpBasic(basic -> basic.disable())

            .formLogin(form -> form.disable())

            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}