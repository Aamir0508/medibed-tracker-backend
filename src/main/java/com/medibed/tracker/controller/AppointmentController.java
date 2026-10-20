package com.medibed.tracker.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.AppointmentRequest;
import com.medibed.tracker.dto.Dtos.AppointmentResponse;
import com.medibed.tracker.dto.Dtos.RescheduleRequest;
import com.medibed.tracker.service.AppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    /** Public: a patient or family member books without an account. */
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse created = service.book(request);
        return ResponseEntity.created(URI.create("/api/appointments/" + created.id())).body(created);
    }

    // The calls below expose patient details or change bookings, so they are for hospital staff only.

    @GetMapping("/{id}")
    @PreAuthorize("@access.appointment(#id)")
    public AppointmentResponse one(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("@access.doctor(#doctorId)")
    public List<AppointmentResponse> byDoctor(@PathVariable("doctorId") Long doctorId) {
        return service.byDoctor(doctorId);
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("@access.appointment(#id)")
    public AppointmentResponse reschedule(@PathVariable("id") Long id, @Valid @RequestBody RescheduleRequest request) {
        return service.reschedule(id, request.slotTime());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.appointment(#id)")
    public ResponseEntity<Void> cancel(@PathVariable("id") Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
