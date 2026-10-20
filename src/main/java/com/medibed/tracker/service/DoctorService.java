package com.medibed.tracker.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.DoctorRequest;
import com.medibed.tracker.dto.Dtos.DoctorResponse;
import com.medibed.tracker.dto.Dtos.DoctorStatusRequest;
import com.medibed.tracker.exception.BadRequestException;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.Doctor;
import com.medibed.tracker.model.DoctorStatus;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.repository.DoctorRepository;
import com.medibed.tracker.repository.HospitalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    /** "Doctor specialization: must match an allowed specialization list" */
    public static final Set<String> SPECIALIZATIONS = Set.of(
            "Anesthesiology", "Cardiology", "Dermatology", "ENT", "Emergency Medicine", "Endocrinology",
            "Gastroenterology", "General Medicine", "General Surgery", "Gynecology", "Nephrology", "Neurology",
            "Neurosurgery", "Oncology", "Ophthalmology", "Orthopedics", "Pediatrics", "Psychiatry",
            "Pulmonology", "Radiology", "Radiology & Oncology", "Urology");

    private final DoctorRepository doctors;
    private final HospitalRepository hospitals;

    @Value("${medibed.avg-consult-minutes:10}")
    private int avgConsultMinutes;

    public List<DoctorResponse> findAll() {
        return doctors.findAll().stream().map(this::toResponse).toList();
    }

    public List<DoctorResponse> byHospital(Long hospitalId) {
        requireHospital(hospitalId);
        return doctors.findByHospitalId(hospitalId).stream().map(this::toResponse).toList();
    }

    public List<DoctorResponse> bySpecialization(String specialization) {
        return doctors.findBySpecializationIgnoreCase(specialization.trim()).stream().map(this::toResponse).toList();
    }

    /** The key emergency query: AVAILABLE doctors whose shift covers the current time. */
    public List<DoctorResponse> availableNow(Long hospitalId) {
        requireHospital(hospitalId);
        LocalTime now = LocalTime.now();
        return doctors.findByHospitalIdAndStatus(hospitalId, DoctorStatus.AVAILABLE).stream()
                .filter(d -> Mapper.onShift(d.getAvailableFrom(), d.getAvailableTo(), now))
                .map(this::toResponse).toList();
    }

    public List<DoctorResponse> topRated(Long hospitalId, int limit) {
        requireHospital(hospitalId);
        return doctors.findByHospitalIdOrderByRatingDescReviewCountDesc(hospitalId, PageRequest.of(0, checkLimit(limit)))
                .stream().map(this::toResponse).toList();
    }

    public List<DoctorResponse> topRatedNetwork(int limit) {
        return doctors.findAllByOrderByRatingDescReviewCountDesc(PageRequest.of(0, checkLimit(limit)))
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DoctorResponse add(Long hospitalId, DoctorRequest r) {
        Hospital h = requireHospital(hospitalId);
        Doctor d = new Doctor();
        d.setHospital(h);
        apply(d, r);
        return toResponse(doctors.save(d));
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorRequest r) {
        Doctor d = find(id);
        apply(d, r);
        return toResponse(d);
    }

    @Transactional
    public DoctorResponse updateStatus(Long id, DoctorStatusRequest r) {
        Doctor d = find(id);
        d.setStatus(r.status());
        if (r.queueCount() != null) {
            d.setQueueCount(r.queueCount());
        }
        return toResponse(d);
    }

    @Transactional
    public void delete(Long id) {
        doctors.delete(find(id));
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Doctor d, DoctorRequest r) {
        d.setName(r.name().trim());
        d.setSpecialization(canonicalSpecialization(r.specialization()));
        d.setQualification(r.qualification());
        d.setLanguages(r.languages());
        d.setBio(r.bio());
        d.setDepartment(r.department().trim());
        d.setRoomNumber(r.roomNumber());
        d.setAvailableFrom(r.availableFrom());
        d.setAvailableTo(r.availableTo());
        d.setStatus(r.status());
        if (r.queueCount() != null) {
            d.setQueueCount(r.queueCount());
        }
        if (r.experienceYears() != null) {
            d.setExperienceYears(r.experienceYears());
        }
        if (r.rating() != null) {
            d.setRating(r.rating());
        }
        if (r.reviewCount() != null) {
            d.setReviewCount(r.reviewCount());
        }
        if (r.emergencyReady() != null) {
            d.setEmergencyReady(r.emergencyReady());
        }
        if (r.photoUrl() != null) {
            d.setPhotoUrl(r.photoUrl());
        }
    }

    private String canonicalSpecialization(String input) {
        String wanted = input.trim();
        for (String s : SPECIALIZATIONS) {
            if (s.equalsIgnoreCase(wanted)) {
                return s;
            }
        }
        throw new BadRequestException("specialization",
                "must be one of: " + String.join(", ", SPECIALIZATIONS.stream().sorted().toList()));
    }

    private int checkLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new BadRequestException("limit", "must be between 1 and 50");
        }
        return limit;
    }

    private Hospital requireHospital(Long id) {
        return hospitals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hospital", id));
    }

    private Doctor find(Long id) {
        return doctors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
    }

    private DoctorResponse toResponse(Doctor d) {
        return Mapper.doctor(d, avgConsultMinutes);
    }
}
