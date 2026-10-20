package com.medibed.tracker.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.DoctorRequest;
import com.medibed.tracker.dto.Dtos.DoctorResponse;
import com.medibed.tracker.dto.Dtos.DoctorStatusRequest;
import com.medibed.tracker.service.DoctorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService service;

    @PostMapping("/hospital/{hospitalId}")
    @PreAuthorize("@access.hospital(#hospitalId)")
    public ResponseEntity<DoctorResponse> add(@PathVariable("hospitalId") Long hospitalId,
                                              @Valid @RequestBody DoctorRequest request) {
        DoctorResponse created = service.add(hospitalId, request);
        return ResponseEntity.created(URI.create("/api/doctors/" + created.id())).body(created);
    }

    @GetMapping
    public List<DoctorResponse> all() {
        return service.findAll();
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<DoctorResponse> byHospital(@PathVariable("hospitalId") Long hospitalId) {
        return service.byHospital(hospitalId);
    }

    @GetMapping("/specialization/{spec}")
    public List<DoctorResponse> bySpecialization(@PathVariable("spec") String spec) {
        return service.bySpecialization(spec);
    }

    @GetMapping("/hospital/{hospitalId}/available-now")
    public List<DoctorResponse> availableNow(@PathVariable("hospitalId") Long hospitalId) {
        return service.availableNow(hospitalId);
    }

    @GetMapping("/hospital/{hospitalId}/top-rated")
    public List<DoctorResponse> topRated(@PathVariable("hospitalId") Long hospitalId,
                                         @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return service.topRated(hospitalId, limit);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.doctor(#id)")
    public DoctorResponse update(@PathVariable("id") Long id, @Valid @RequestBody DoctorRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@access.doctor(#id)")
    public DoctorResponse updateStatus(@PathVariable("id") Long id, @Valid @RequestBody DoctorStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.doctor(#id)")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
