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

                        // ---- Shipment module ----
                        // Creating a shipment is a business action: business clients and
                        // logistics operators only. Customers can read but never create.
                        .requestMatchers(HttpMethod.POST, "/api/shipments")
                        .hasAnyRole("BUSINESS_CLIENT", "LOGISTICS_OPERATOR")

                        // anyone can track a shipment with the tracking number, no login needed
                        .requestMatchers(HttpMethod.GET, "/api/shipments/tracking/**").permitAll()

                        // Reading is open to every authenticated role; the service layer
                        // scopes results so each user only sees shipments tied to them.
                        .requestMatchers(HttpMethod.GET, "/api/shipments/**")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR",
                                "SUPPORT_AGENT", "ADMINISTRATOR")

                        .requestMatchers(HttpMethod.PUT, "/api/shipments/**")
                        .hasAnyRole("BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Lifecycle transitions are operational work.
                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/*/operator")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Soft cancel — ownership is re-checked in the service layer.
                        .requestMatchers(HttpMethod.DELETE, "/api/shipments/**")
                        .hasAnyRole("BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // ---- Tracking timeline + live location ----
                        // anyone can see the timeline with just a tracking number
                        .requestMatchers(HttpMethod.GET, "/api/tracking/*").permitAll()

                        // only the rider or admin can push a location
                        .requestMatchers(HttpMethod.POST, "/api/tracking/location")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // ---- Routes ----
                        .requestMatchers(HttpMethod.POST, "/api/routes")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/routes/*").authenticated()

                        // ---- Live delivery monitoring ----
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