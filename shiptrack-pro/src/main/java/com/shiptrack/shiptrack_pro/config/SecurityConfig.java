package com.shiptrack.shiptrack_pro.config;

import com.shiptrack.shiptrack_pro.security.JwtAuthFilter;
import com.shiptrack.shiptrack_pro.security.OAuth2LoginSuccessHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    // =====================================================
    // CORS CONFIGURATION
    // =====================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:3000",
                        "http://localhost:3001"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // =====================================================
    // SECURITY FILTER CHAIN
    // =====================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =================================================
                // CORS
                // =================================================
                .cors(Customizer.withDefaults())

                // =================================================
                // CSRF
                // =================================================
                .csrf(csrf -> csrf.disable())

                // =================================================
                // SESSION
                // =================================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // =================================================
                // AUTHORIZATION RULES
                // =================================================
                .authorizeHttpRequests(auth -> auth

                        // -------------------------------------------------
                        // PUBLIC BASE ROUTES
                        // -------------------------------------------------
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        // -------------------------------------------------
                        // OAUTH2 LOGIN
                        // -------------------------------------------------
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // -------------------------------------------------
                        // AUTHENTICATION APIs
                        // Includes forgot-password endpoint
                        // -------------------------------------------------
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // -------------------------------------------------
                        // EXTERNAL MAP APIs
                        // -------------------------------------------------
                        .requestMatchers(
                                "/api/openstreetmap/**",
                                "/api/osrm/**",
                                "/api/gps/**"
                        ).permitAll()

                        // -------------------------------------------------
                        // WEBSOCKET TRACKING
                        // -------------------------------------------------
                        .requestMatchers(
                                "/api/ws/tracking/**"
                        ).permitAll()

                        // -------------------------------------------------
                        // CORS PREFLIGHT
                        // -------------------------------------------------
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // -------------------------------------------------
                        // SHIPMENT CREATION
                        // -------------------------------------------------
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/shipments"
                        ).hasAnyRole(
                                "CUSTOMER",
                                "BUSINESS_CLIENT"
                        )

                        // -------------------------------------------------
                        // DRIVER LOCATION UPDATE
                        // -------------------------------------------------
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/route/*/location"
                        ).hasAnyRole(
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR",
                                "DRIVER"
                        )

                        // =================================================
                        // TRACKING APIs
                        // =================================================

                        // Anyone can view tracking information
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tracking/**"
                        ).permitAll()

                        // Other tracking operations require permission
                        .requestMatchers(
                                "/api/tracking/**"
                        ).hasAnyRole(
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR",
                                "DRIVER"
                        )

                        // =================================================
                        // ROUTE APIs
                        // =================================================

                        // Anyone can view plural route APIs
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/routes/**"
                        ).permitAll()

                        // Anyone can view singular route APIs
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/route/**"
                        ).permitAll()

                        // Other plural route operations require permission
                        .requestMatchers(
                                "/api/routes/**"
                        ).hasAnyRole(
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR",
                                "DRIVER"
                        )

                        // Other singular route operations require permission
                        .requestMatchers(
                                "/api/route/**"
                        ).hasAnyRole(
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR",
                                "DRIVER"
                        )

                        // =================================================
                        // ETA PREDICTION
                        // =================================================
                        .requestMatchers(
                                "/api/eta/**"
                        ).authenticated()

                        // =================================================
                        // PROOF OF DELIVERY
                        // =================================================

                        // Create POD
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/pod/**"
                        ).hasRole(
                                "LOGISTICS_OPERATOR"
                        )

                        // View POD
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/pod/**"
                        ).hasAnyRole(
                                "CUSTOMER",
                                "BUSINESS_CLIENT",
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR"
                        )

                        // Verify POD
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/pod/*/verify"
                        ).hasAnyRole(
                                "LOGISTICS_OPERATOR",
                                "ADMINISTRATOR"
                        )

                        // =================================================
                        // ANALYTICS
                        // =================================================

                        // Customer analytics
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/analytics/customer"
                        ).hasRole(
                                "CUSTOMER"
                        )

                        // Business client analytics
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/analytics/business"
                        ).hasRole(
                                "BUSINESS_CLIENT"
                        )

                        // Admin analytics
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/analytics/admin"
                        ).hasRole(
                                "ADMINISTRATOR"
                        )

                        // =================================================
                        // REPORTS
                        // =================================================
                        .requestMatchers(
                                "/api/reports/**"
                        ).hasAnyRole(
                                "BUSINESS_CLIENT",
                                "ADMINISTRATOR"
                        )

                        // =================================================
                        // ADMIN APIs
                        // =================================================
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole(
                                "ADMINISTRATOR"
                        )

                        // =================================================
                        // EVERYTHING ELSE
                        // =================================================
                        .anyRequest().authenticated()
                )

                // =================================================
                // DISABLE BASIC AUTHENTICATION
                // =================================================
                .httpBasic(
                        basic -> basic.disable()
                )

                // =================================================
                // DISABLE FORM LOGIN
                // =================================================
                .formLogin(
                        form -> form.disable()
                )

                // =================================================
                // GITHUB OAUTH2 LOGIN
                // =================================================
                .oauth2Login(oauth2 ->
                        oauth2.successHandler(
                                oauth2LoginSuccessHandler
                        )
                )

                // =================================================
                // JWT AUTHENTICATION FILTER
                // =================================================
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}