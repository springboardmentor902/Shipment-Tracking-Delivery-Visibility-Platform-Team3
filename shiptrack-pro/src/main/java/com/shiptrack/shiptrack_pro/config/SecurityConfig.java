package com.shiptrack.shiptrack_pro.config;

import com.shiptrack.shiptrack_pro.security.JwtAuthFilter;
import com.shiptrack.shiptrack_pro.security.OAuth2LoginSuccessHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;


    // ==========================================
    // Password Encoder
    // ==========================================

   


    // ==========================================
    // Security Filter Chain
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            // ==========================================
            // CSRF
            // ==========================================

            .csrf(csrf -> csrf.disable())


            // ==========================================
            // Session
            // ==========================================

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )


            // ==========================================
            // Authorization Rules
            // ==========================================

            .authorizeHttpRequests(auth -> auth

                // ----------------------------------
                // External APIs
                // ----------------------------------

                .requestMatchers("/api/openstreetmap/**")
                .permitAll()

                .requestMatchers("/api/osrm/**")
                .permitAll()

                .requestMatchers("/api/gps/**")
                .permitAll()


                // ----------------------------------
                // OAuth2
                // ----------------------------------

                .requestMatchers("/login/**")
                .permitAll()

                .requestMatchers("/oauth2/**")
                .permitAll()


                // ----------------------------------
                // Authentication
                // ----------------------------------

                .requestMatchers("/api/auth/**")
                .permitAll()


                // ----------------------------------
                // WebSocket
                // ----------------------------------

                .requestMatchers("/api/ws/tracking/**")
                .permitAll()


                // ----------------------------------
                // Shipment Creation
                // ----------------------------------

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/shipments"
                )
                .hasAnyRole(
                        "CUSTOMER",
                        "BUSINESS_CLIENT"
                )


                // ----------------------------------
                // Driver Location
                // ----------------------------------

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/route/*/location"
                )
                .hasAnyRole(
                        "LOGISTICS_OPERATOR",
                        "ADMINISTRATOR",
                        "DRIVER"
                )


                // ----------------------------------
                // Routes & Tracking
                // ----------------------------------

                .requestMatchers(
                        "/api/tracking/**",
                        "/api/routes/**"
                )
                .hasAnyRole(
                        "LOGISTICS_OPERATOR",
                        "ADMINISTRATOR",
                        "DRIVER"
                )


                // ----------------------------------
                // ETA Prediction
                // ----------------------------------

                .requestMatchers("/api/eta/**")
                .authenticated()


                // ==================================
                // Proof of Delivery
                // ==================================

                // Create POD
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/pod/**"
                )
                .hasRole("LOGISTICS_OPERATOR")


                // View POD
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/pod/**"
                )
                .hasAnyRole(
                        "CUSTOMER",
                        "BUSINESS_CLIENT",
                        "LOGISTICS_OPERATOR",
                        "ADMINISTRATOR"
                )


                // Verify POD
                .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/pod/*/verify"
                )
                .hasAnyRole(
                        "LOGISTICS_OPERATOR",
                        "ADMINISTRATOR"
                )


                // ==================================
                // Analytics
                // ==================================

                // Customer Analytics
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/analytics/customer"
                )
                .hasRole("CUSTOMER")


                // Business Client Analytics
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/analytics/business"
                )
                .hasRole("BUSINESS_CLIENT")


                // Admin Analytics
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/analytics/admin"
                )
                .hasRole("ADMINISTRATOR")


                // ----------------------------------
                // Reports
                // ----------------------------------

                .requestMatchers("/api/reports/**")
                .hasAnyRole(
                        "BUSINESS_CLIENT",
                        "ADMINISTRATOR"
                )


                // ----------------------------------
                // Admin
                // ----------------------------------

                .requestMatchers("/api/admin/**")
                .hasRole("ADMINISTRATOR")


                // ----------------------------------
                // Everything Else
                // ----------------------------------

                .anyRequest()
                .authenticated()
            )


            // ==========================================
            // Disable Basic Authentication
            // ==========================================

            .httpBasic(basic -> basic.disable())


            // ==========================================
            // Disable Form Login
            // ==========================================

            .formLogin(form -> form.disable())


            // ==========================================
            // OAuth2 Login
            // ==========================================

            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2LoginSuccessHandler)
            )


            // ==========================================
            // JWT Filter
            // ==========================================

            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }
}