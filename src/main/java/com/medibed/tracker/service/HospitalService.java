package com.medibed.tracker.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.BedUpdateRequest;
import com.medibed.tracker.dto.Dtos.HospitalRequest;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.exception.BadRequestException;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.BedSnapshot;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.model.HospitalType;
import com.medibed.tracker.repository.BedSnapshotRepository;
import com.medibed.tracker.repository.HospitalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalService {

    private final HospitalRepository hospitals;
    private final BedSnapshotRepository snapshots;
    private final DoctorService doctorService;
    private final NotificationService notifications;
    private final BedEventPublisher events;

    @Value("${medibed.bed-alert-threshold:2}")
    private int alertThreshold;

    public List<HospitalResponse> findAll() {
        return hospitals.findAll(Sort.by("id")).stream().map(h -> Mapper.hospital(h, null)).toList();
    }

    /** One hospital together with its doctors. */
    public HospitalResponse get(Long id) {
        Hospital h = find(id);
        return Mapper.hospital(h, doctorService.byHospital(id));
    }

    public List<HospitalResponse> byArea(String area) {
        return hospitals.findByAreaIgnoreCase(area.trim()).stream().map(h -> Mapper.hospital(h, null)).toList();
    }

    public List<HospitalResponse> byType(String type) {
        HospitalType parsed;
        try {
            parsed = HospitalType.from(type);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("type", "must be Government or Private");
        }
        return hospitals.findByType(parsed).stream().map(h -> Mapper.hospital(h, null)).toList();
    }

    public List<HospitalResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("keyword", "is required");
        }
        return hospitals.findByNameContainingIgnoreCase(keyword.trim()).stream()
                .map(h -> Mapper.hospital(h, null)).toList();
    }

    @Transactional
    public HospitalResponse create(HospitalRequest r) {
        Hospital h = new Hospital();
        apply(h, r, true);
        h = hospitals.save(h);
        snapshots.save(BedSnapshot.of(h));
        return Mapper.hospital(h, null);
    }

    @Transactional
    public HospitalResponse update(Long id, HospitalRequest r) {
        Hospital h = find(id);
        apply(h, r, false);
        return Mapper.hospital(h, null);
    }

    /** A hospital pushes its live bed counts. Records a snapshot, raises low-bed alerts, pushes to SSE clients. */
    @Transactional
    public HospitalResponse updateBeds(Long id, BedUpdateRequest r) {
        Hospital h = find(id);
        int[] before = {h.getGeneralBeds(), h.getIcuBeds(), h.getOxygenBeds(), h.getNicuBeds(), h.getEmergencyBeds()};
        int[] after = {r.generalBeds(), r.icuBeds(), r.oxygenBeds(), r.nicuBeds(), r.emergencyBeds()};

        h.setGeneralBeds(after[0]);
        h.setIcuBeds(after[1]);
        h.setOxygenBeds(after[2]);
        h.setNicuBeds(after[3]);
        h.setEmergencyBeds(after[4]);
        snapshots.save(BedSnapshot.of(h));

        String[] labels = {"General", "ICU", "Oxygen", "NICU", "Emergency"};
        for (int i = 0; i < labels.length; i++) {
            if (after[i] < before[i] && after[i] <= alertThreshold) {
                String msg = after[i] == 0
                        ? labels[i] + " beds are FULL at " + h.getName()
                        : "Low " + labels[i] + " beds at " + h.getName() + ": only " + after[i] + " left";
                notifications.notifyStaff(h, msg);
            }
        }

        HospitalResponse response = Mapper.hospital(h, null);
        events.publish(response);
        return response;
    }

    /** Child rows (doctors, appointments, ...) are removed by ON DELETE CASCADE. */
    @Transactional
    public void delete(Long id) {
        hospitals.delete(find(id));
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Hospital h, HospitalRequest r, boolean creating) {
        h.setName(r.name().trim());
        h.setArea(r.area().trim());
        h.setAddress(r.address().trim());
        h.setPhone(r.phone().trim());
        h.setType(r.type());
        if (r.emergencyPhone() != null) {
            h.setEmergencyPhone(r.emergencyPhone().trim());
        }
        if (r.rating() != null) {
            h.setRating(r.rating());
        }
        if (r.preferred() != null) {
            h.setPreferred(r.preferred());
        }
        if (r.photoUrl() != null) {
            h.setPhotoUrl(r.photoUrl());
        }
        if (r.generalBeds() != null) {
            h.setGeneralBeds(r.generalBeds());
        }
        if (r.icuBeds() != null) {
            h.setIcuBeds(r.icuBeds());
        }
        if (r.oxygenBeds() != null) {
            h.setOxygenBeds(r.oxygenBeds());
        }
        if (r.nicuBeds() != null) {
            h.setNicuBeds(r.nicuBeds());
        }
        if (r.emergencyBeds() != null) {
            h.setEmergencyBeds(r.emergencyBeds());
        }
        List<String> facilities = r.facilities() == null ? List.of()
                : r.facilities().stream().filter(f -> f != null && !f.isBlank()).map(String::trim).toList();
        if (!facilities.isEmpty()) {
            h.setFacilities(new ArrayList<>(facilities));
        } else if (creating) {
            h.setFacilities(new ArrayList<>(List.of("General Medicine")));
        }
    }

    private Hospital find(Long id) {
        return hospitals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hospital", id));
    }
}
