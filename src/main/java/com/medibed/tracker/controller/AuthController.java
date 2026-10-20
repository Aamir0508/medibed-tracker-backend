package com.medibed.tracker.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.AuthResponse;
import com.medibed.tracker.dto.Dtos.LoginRequest;
import com.medibed.tracker.dto.Dtos.RegisterRequest;
import com.medibed.tracker.dto.Dtos.UserResponse;
import com.medibed.tracker.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.refresh(authorization);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        service.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
