package com.medibed.tracker.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.medibed.tracker.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())              // stateless API, no cookies, so no CSRF surface
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // Families need read access without an account
                .requestMatchers(HttpMethod.GET, "/api/hospitals/**", "/api/doctors/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/appointments", "/api/ambulance/request").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ambulance/nearest", "/api/ambulance/*/status").permitAll()
                // everything else under /api needs a valid JWT (roles are checked with @PreAuthorize)
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeError(res, 401, "Unauthorized",
                        "Login required: send a valid 'Authorization: Bearer <token>' header"))
                .accessDeniedHandler((req, res, ex) -> writeError(res, 403, "Forbidden",
                        "You do not have permission to perform this action")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${medibed.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        cfg.setAllowedOriginPatterns(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static void writeError(HttpServletResponse res, int status, String error, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
