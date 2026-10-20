package com.medibed.tracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medibed.tracker.dto.Dtos.AppointmentReport;
import com.medibed.tracker.dto.Dtos.DoctorResponse;
import com.medibed.tracker.dto.Dtos.OccupancyReport;
import com.medibed.tracker.dto.Dtos.ResponseTimeReport;
import com.medibed.tracker.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/hospital/{hospitalId}/occupancy")
    @PreAuthorize("@access.hospital(#hospitalId)")
    public OccupancyReport occupancy(@PathVariable("hospitalId") Long hospitalId,
                                     @RequestParam(name = "days", defaultValue = "7") int days) {
        return service.occupancy(hospitalId, days);
    }

    @GetMapping("/hospital/{hospitalId}/appointments")
    @PreAuthorize("@access.hospital(#hospitalId)")
    public AppointmentReport appointments(
            @PathVariable("hospitalId") Long hospitalId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.appointments(hospitalId, from, to);
    }

    @GetMapping("/doctors/top-rated")
    public List<DoctorResponse> topDoctors(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return service.topDoctors(limit);
    }

    @GetMapping("/ambulance/response-times")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseTimeReport responseTimes() {
        return service.ambulanceResponseTimes();
    }
}
