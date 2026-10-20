package com.medibed.tracker.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.NotificationResponse;
import com.medibed.tracker.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("@access.hospital(#hospitalId)")
    public List<NotificationResponse> forHospital(@PathVariable("hospitalId") Long hospitalId) {
        return service.forHospital(hospitalId);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("@access.notification(#id)")
    public NotificationResponse markRead(@PathVariable("id") Long id) {
        return service.markRead(id);
    }
}
