package com.medibed.tracker.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.medibed.tracker.dto.Dtos.BedUpdateRequest;
import com.medibed.tracker.dto.Dtos.HospitalRequest;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.service.BedEventPublisher;
import com.medibed.tracker.service.HospitalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService service;
    private final BedEventPublisher events;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<HospitalResponse> create(@Valid @RequestBody HospitalRequest request) {
        HospitalResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/hospitals/" + created.id())).body(created);
    }

    @GetMapping
    public List<HospitalResponse> all() {
        return service.findAll();
    }

    /** Live bed feed: open with EventSource; an event named "bed-update" arrives on every change. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return events.subscribe();
    }

    @GetMapping("/search")
    public List<HospitalResponse> search(@RequestParam("keyword") String keyword) {
        return service.search(keyword);
    }

    @GetMapping("/area/{area}")
    public List<HospitalResponse> byArea(@PathVariable("area") String area) {
        return service.byArea(area);
    }

    @GetMapping("/type/{type}")
    public List<HospitalResponse> byType(@PathVariable("type") String type) {
        return service.byType(type);
    }

    @GetMapping("/{id}")
    public HospitalResponse one(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.hospital(#id)")
    public HospitalResponse update(@PathVariable("id") Long id, @Valid @RequestBody HospitalRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/beds")
    @PreAuthorize("@access.hospital(#id)")
    public HospitalResponse updateBeds(@PathVariable("id") Long id, @Valid @RequestBody BedUpdateRequest request) {
        return service.updateBeds(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
