package com.medibed.tracker.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.AmbulanceCreateRequest;
import com.medibed.tracker.dto.Dtos.AmbulanceResponse;
import com.medibed.tracker.dto.Dtos.AmbulanceStatusUpdate;
import com.medibed.tracker.dto.Dtos.AmbulanceTrackingResponse;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.AmbulanceRequest;
import com.medibed.tracker.model.AmbulanceStatus;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.repository.AmbulanceRequestRepository;
import com.medibed.tracker.repository.HospitalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmbulanceService {

    private static final int NEAREST_LIMIT = 5;

    private final AmbulanceRequestRepository requests;
    private final HospitalRepository hospitals;
    private final NotificationService notifications;

    @Transactional
    public AmbulanceResponse create(AmbulanceCreateRequest r) {
        Hospital h = hospitals.findById(r.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", r.hospitalId()));
        AmbulanceRequest a = new AmbulanceRequest();
        a.setPatientName(r.patientName().trim());
        a.setContactPhone(r.contactPhone());
        a.setPickupLocation(r.pickupLocation().trim());
        a.setHospital(h);
        a.setStatus(AmbulanceStatus.REQUESTED);
        a = requests.save(a);
        notifications.notifyStaff(h, "Ambulance requested for " + a.getPatientName() + " from " + a.getPickupLocation());
        return Mapper.ambulance(a);
    }

    public AmbulanceTrackingResponse track(Long id) {
        return Mapper.tracking(find(id));
    }

    @Transactional
    public AmbulanceResponse updateStatus(Long id, AmbulanceStatusUpdate u) {
        AmbulanceRequest a = find(id);
        a.setStatus(u.status());
        if (u.etaMinutes() != null) {
            a.setEtaMinutes(u.etaMinutes());
        }
        boolean moving = u.status() == AmbulanceStatus.DISPATCHED || u.status() == AmbulanceStatus.EN_ROUTE
                || u.status() == AmbulanceStatus.ARRIVED;
        if (moving && a.getDispatchedAt() == null) {
            a.setDispatchedAt(LocalDateTime.now());
        }
        String eta = a.getEtaMinutes() == null ? "" : " (ETA " + a.getEtaMinutes() + " min)";
        notifications.notifyRequester(a.getHospital(), "Ambulance " + a.getId() + " is now " + u.status() + eta);
        notifications.notifyStaff(a.getHospital(), "Ambulance " + a.getId() + " for " + a.getPatientName()
                + ": " + u.status() + eta);
        return Mapper.ambulance(a);
    }

    /**
     * Hospitals with at least one free emergency bed. Ranking: the reference hospital itself (if it has
     * capacity), then hospitals in the same area, then the rest, each group by most emergency beds.
     * (Real driving distance is on the roadmap - this version works with area names only.)
     */
    public List<HospitalResponse> nearest(Long hospitalId) {
        Hospital ref = hospitals.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        Comparator<Hospital> order = Comparator
                .comparingInt((Hospital h) -> h.getId().equals(ref.getId()) ? 0
                        : h.getArea().equalsIgnoreCase(ref.getArea()) ? 1 : 2)
                .thenComparing(Comparator.comparingInt(Hospital::getEmergencyBeds).reversed());
        return hospitals.findByEmergencyBedsGreaterThan(0).stream()
                .sorted(order).limit(NEAREST_LIMIT)
                .map(h -> Mapper.hospital(h, null)).toList();
    }

    private AmbulanceRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ambulance request", id));
    }
}
