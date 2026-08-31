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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // activates @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        // spring forwards failures to /error, if this is blocked the real
                        // status code gets replaced by an empty 403
                        .requestMatchers("/error").permitAll()

                        // CORS preflight must never be authenticated
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // The tracking socket handshake carries no Authorization header;
                        // the JWT is checked on the STOMP CONNECT frame instead
                        // (StompAuthChannelInterceptor).
                        .requestMatchers("/api/ws/tracking/**").permitAll()

                        // ---- Shipment module ----
                        // Customers, business clients and logistics operators book shipments.
                        // Support agents and administrators only manage them.
                        .requestMatchers(HttpMethod.POST, "/api/shipments")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR")

                        // anyone can track a shipment with the tracking number, no login needed
                        .requestMatchers(HttpMethod.GET, "/api/shipments/tracking/**").permitAll()

                        // Reading is open to every authenticated role; the service layer
                        // scopes results so each user only sees shipments tied to them.
                        .requestMatchers(HttpMethod.GET, "/api/shipments/**")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR",
                                "SUPPORT_AGENT", "ADMINISTRATOR")

                        // Ownership is re-checked in the service layer: a customer may only
                        // edit the shipment they booked themselves.
                        .requestMatchers(HttpMethod.PUT, "/api/shipments/**")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Lifecycle transitions are operational work.
                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/*/operator")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Soft cancel — ownership is re-checked in the service layer.
                        .requestMatchers(HttpMethod.DELETE, "/api/shipments/**")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // ---- Tracking timeline + live location ----
                        // anyone can see the timeline with just a tracking number
                        .requestMatchers(HttpMethod.GET, "/api/tracking/*").permitAll()

                        // only the rider or admin can push a location or a checkpoint
                        .requestMatchers(HttpMethod.POST, "/api/tracking/location")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/tracking/events")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // ---- Routes (multi-leg) ----
                        // Only operators and admins plan, edit or reassign route legs.
                        .requestMatchers(HttpMethod.POST, "/api/routes", "/api/routes/*/refresh")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/routes/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        // Customers and business clients may view routes of shipments they
                        // are allowed to see; the service layer enforces that access.
                        .requestMatchers(HttpMethod.GET, "/api/routes/**").authenticated()

                        // ---- Live delivery monitoring ----
                        // ETA reads are authorized per shipment inside EtaService
                        .requestMatchers(HttpMethod.GET, "/api/eta/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/eta/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "SUPPORT_AGENT", "ADMINISTRATOR")

                        .requestMatchers(HttpMethod.GET, "/api/monitoring/active")
                        .hasAnyRole("LOGISTICS_OPERATOR", "SUPPORT_AGENT", "ADMINISTRATOR")

                        // ---- Profile ----
                        .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/users/me/activity").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/password").authenticated()

                        // ---- Business accounts ----
                        .requestMatchers(HttpMethod.POST, "/api/business-accounts")
                        .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/business-accounts/me")
                        .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/business-accounts/me")
                        .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/business-accounts")
                        .hasRole("ADMINISTRATOR")

                        .requestMatchers(HttpMethod.POST, "/api/pod/**")
                        .hasRole("LOGISTICS_OPERATOR")

                        .requestMatchers("/api/analytics/**", "/api/reports/**")
                        .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")

                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")

                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}