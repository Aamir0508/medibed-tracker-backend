package com.medibed.tracker.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.AmbulanceCreateRequest;
import com.medibed.tracker.dto.Dtos.AmbulanceResponse;
import com.medibed.tracker.dto.Dtos.AmbulanceStatusUpdate;
import com.medibed.tracker.dto.Dtos.AmbulanceTrackingResponse;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.service.AmbulanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ambulance")
@RequiredArgsConstructor
public class AmbulanceController {

    private final AmbulanceService service;

    @PostMapping("/request")
    public ResponseEntity<AmbulanceResponse> request(@Valid @RequestBody AmbulanceCreateRequest request) {
        AmbulanceResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/ambulance/" + created.id() + "/status")).body(created);
    }

    @GetMapping("/{id}/status")
    public AmbulanceTrackingResponse status(@PathVariable("id") Long id) {
        return service.track(id);
    }

    @GetMapping("/nearest")
    public List<HospitalResponse> nearest(@RequestParam("hospitalId") Long hospitalId) {
        return service.nearest(hospitalId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@access.ambulance(#id)")
    public AmbulanceResponse updateStatus(@PathVariable("id") Long id, @Valid @RequestBody AmbulanceStatusUpdate update) {
        return service.updateStatus(id, update);
    }
}
