package com.medibed.tracker.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.AuthResponse;
import com.medibed.tracker.dto.Dtos.LoginRequest;
import com.medibed.tracker.dto.Dtos.RegisterRequest;
import com.medibed.tracker.dto.Dtos.UserResponse;
import com.medibed.tracker.exception.BadRequestException;
import com.medibed.tracker.exception.ConflictException;
import com.medibed.tracker.model.AdminUser;
import com.medibed.tracker.model.Role;
import com.medibed.tracker.repository.AdminUserRepository;
import com.medibed.tracker.repository.HospitalRepository;
import com.medibed.tracker.security.AppUserDetailsService;
import com.medibed.tracker.security.AppUserPrincipal;
import com.medibed.tracker.security.JwtService;
import com.medibed.tracker.security.TokenBlacklist;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository users;
    private final HospitalRepository hospitals;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final AppUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse login(LoginRequest r) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(r.username(), r.password()));
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        users.findByUsername(principal.getUsername()).ifPresent(u -> u.setLastLogin(LocalDateTime.now()));
        return respond(principal);
    }

    /** Only a SUPER_ADMIN reaches this (checked in the controller). Passwords are stored as BCrypt hashes. */
    @Transactional
    public UserResponse register(RegisterRequest r) {
        if (users.existsByUsername(r.username())) {
            throw new ConflictException("Username '" + r.username() + "' is already taken");
        }
        AdminUser u = new AdminUser();
        u.setUsername(r.username());
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setRole(r.role());
        if (r.role() == Role.HOSPITAL_ADMIN) {
            if (r.hospitalId() == null) {
                throw new BadRequestException("hospitalId", "is required for a HOSPITAL_ADMIN");
            }
            u.setHospital(hospitals.findById(r.hospitalId())
                    .orElseThrow(() -> new BadRequestException("hospitalId", "does not match any hospital")));
        }
        u = users.save(u);
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(),
                u.getHospital() == null ? null : u.getHospital().getId());
    }

    /** Swap a still-valid token for a fresh one; the old one stops working. */
    public AuthResponse refresh(String authorizationHeader) {
        Claims claims = parse(authorizationHeader);
        AppUserPrincipal principal = (AppUserPrincipal) userDetailsService.loadUserByUsername(claims.getSubject());
        blacklist.revoke(claims);
        return respond(principal);
    }

    public void logout(String authorizationHeader) {
        blacklist.revoke(parse(authorizationHeader));
    }

    private Claims parse(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing bearer token");
        }
        try {
            return jwtService.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired token");
        }
    }

    private AuthResponse respond(AppUserPrincipal p) {
        return new AuthResponse(jwtService.generate(p), "Bearer", jwtService.expiresInSeconds(),
                p.getUsername(), p.getRole(), p.getHospitalId());
    }
}
