package com.medibed.tracker.service;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medibed.tracker.dto.Dtos.AmbulanceResponse;
import com.medibed.tracker.dto.Dtos.AmbulanceTrackingResponse;
import com.medibed.tracker.dto.Dtos.AppointmentResponse;
import com.medibed.tracker.dto.Dtos.DoctorResponse;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.dto.Dtos.NotificationResponse;
import com.medibed.tracker.model.AmbulanceRequest;
import com.medibed.tracker.model.Appointment;
import com.medibed.tracker.model.Doctor;
import com.medibed.tracker.model.DoctorStatus;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.model.Notification;

/** Entity -> response DTO conversion. Entities are never returned from controllers. */
public final class Mapper {

    private Mapper() {
    }

    public static Map<String, Integer> beds(int general, int icu, int oxygen, int nicu, int emergency) {
        Map<String, Integer> beds = new LinkedHashMap<>();
        beds.put("General", general);
        beds.put("ICU", icu);
        beds.put("Oxygen", oxygen);
        beds.put("NICU", nicu);
        beds.put("Emergency", emergency);
        return beds;
    }

    public static Map<String, Integer> beds(Hospital h) {
        return beds(h.getGeneralBeds(), h.getIcuBeds(), h.getOxygenBeds(), h.getNicuBeds(), h.getEmergencyBeds());
    }

    public static int totalBeds(Hospital h) {
        return h.getGeneralBeds() + h.getIcuBeds() + h.getOxygenBeds() + h.getNicuBeds() + h.getEmergencyBeds();
    }

    public static HospitalResponse hospital(Hospital h, List<DoctorResponse> doctors) {
        return new HospitalResponse(h.getId(), h.getName(), h.getArea(), h.getAddress(), h.getPhone(),
                h.getEmergencyPhone(), h.getType(), h.getRating(), h.isPreferred(), h.getPhotoUrl(),
                beds(h), totalBeds(h), List.copyOf(h.getFacilities()), doctors);
    }

    /** True if {@code now} falls inside the shift. Handles overnight shifts such as 22:00 - 06:00. */
    public static boolean onShift(LocalTime from, LocalTime to, LocalTime now) {
        if (!from.isAfter(to)) {
            return !now.isBefore(from) && !now.isAfter(to);
        }
        return !now.isBefore(from) || !now.isAfter(to);
    }

    public static DoctorResponse doctor(Doctor d, int avgConsultMinutes) {
        boolean availableNow = d.getStatus() == DoctorStatus.AVAILABLE
                && onShift(d.getAvailableFrom(), d.getAvailableTo(), LocalTime.now());
        return new DoctorResponse(d.getId(), d.getHospital().getId(), d.getHospital().getName(), d.getName(),
                d.getSpecialization(), d.getQualification(), d.getLanguages(), d.getBio(), d.getDepartment(),
                d.getRoomNumber(), d.getAvailableFrom(), d.getAvailableTo(), d.getStatus(), d.getQueueCount(),
                d.getQueueCount() * avgConsultMinutes, availableNow, d.getExperienceYears(), d.getRating(),
                d.getReviewCount(), d.isEmergencyReady(), d.getPhotoUrl());
    }

    public static AppointmentResponse appointment(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getPatientName(), a.getPatientPhone(), a.getDoctor().getId(),
                a.getDoctor().getName(), a.getHospital().getId(), a.getHospital().getName(), a.getSlotTime(),
                a.getStatus());
    }

    public static AmbulanceResponse ambulance(AmbulanceRequest r) {
        return new AmbulanceResponse(r.getId(), r.getPatientName(), r.getPickupLocation(), r.getHospital().getId(),
                r.getHospital().getName(), r.getStatus(), r.getEtaMinutes(), r.getCreatedAt(), r.getDispatchedAt());
    }

    public static AmbulanceTrackingResponse tracking(AmbulanceRequest r) {
        return new AmbulanceTrackingResponse(r.getId(), r.getStatus(), r.getEtaMinutes(),
                r.getHospital().getName(), r.getHospital().getPhone());
    }

    public static NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getRecipientType(), n.getMessage(), n.getHospital().getId(),
                n.isRead(), n.getCreatedAt());
    }
}
