package com.medibed.tracker.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medibed.tracker.model.AmbulanceStatus;
import com.medibed.tracker.model.AppointmentStatus;
import com.medibed.tracker.model.DoctorStatus;
import com.medibed.tracker.model.HospitalType;
import com.medibed.tracker.model.RecipientType;
import com.medibed.tracker.model.Role;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** All request/response shapes of the API. Validation rules mirror the "Validation Rules" slide. */
public final class Dtos {

    private Dtos() {
    }

    public static final String PHONE_REGEX = "^[0-9+()\\-\\s]{8,20}$";
    public static final String PHONE_MSG = "must be a valid phone number (digits, spaces, +, -, brackets; 8-20 characters)";

    // ------------------------------------------------------------------ hospitals

    public record HospitalRequest(
            @NotBlank @Size(min = 3, max = 120) String name,
            @NotBlank @Size(max = 80) String area,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String phone,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String emergencyPhone,
            @NotNull HospitalType type,
            @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
            Boolean preferred,
            @Size(max = 255) String photoUrl,
            @Min(0) Integer generalBeds,
            @Min(0) Integer icuBeds,
            @Min(0) Integer oxygenBeds,
            @Min(0) Integer nicuBeds,
            @Min(0) Integer emergencyBeds,
            List<String> facilities) {
    }

    public record BedUpdateRequest(
            @NotNull @Min(0) Integer generalBeds,
            @NotNull @Min(0) Integer icuBeds,
            @NotNull @Min(0) Integer oxygenBeds,
            @NotNull @Min(0) Integer nicuBeds,
            @NotNull @Min(0) Integer emergencyBeds) {
    }

    /** "beds" uses the same keys as the frontend: General, ICU, Oxygen, NICU, Emergency. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HospitalResponse(
            Long id, String name, String area, String address, String phone, String emergencyPhone,
            HospitalType type, Double rating, boolean preferred, String photoUrl,
            Map<String, Integer> beds, int totalAvailableBeds, List<String> facilities,
            List<DoctorResponse> doctors) {
    }

    // ------------------------------------------------------------------ doctors

    public record DoctorRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String specialization,
            @Size(max = 150) String qualification,
            @Size(max = 120) String languages,
            @Size(max = 300) String bio,
            @NotBlank @Size(max = 100) String department,
            @Size(max = 50) String roomNumber,
            @NotNull LocalTime availableFrom,
            @NotNull LocalTime availableTo,
            @NotNull DoctorStatus status,
            @Min(0) Integer queueCount,
            @Min(0) Integer experienceYears,
            @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
            @Min(0) Integer reviewCount,
            Boolean emergencyReady,
            @Size(max = 255) String photoUrl) {
    }

    public record DoctorStatusRequest(@NotNull DoctorStatus status, @Min(0) Integer queueCount) {
    }

    public record DoctorResponse(
            Long id, Long hospitalId, String hospitalName, String name, String specialization,
            String qualification, String languages, String bio, String department, String roomNumber,
            LocalTime availableFrom, LocalTime availableTo, DoctorStatus status,
            int queueCount, int estimatedWaitMinutes, boolean availableNow,
            int experienceYears, Double rating, int reviewCount, boolean emergencyReady, String photoUrl) {
    }

    // ------------------------------------------------------------------ appointments

    public record AppointmentRequest(
            @NotBlank @Size(min = 2, max = 100) String patientName,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String patientPhone,
            @NotNull Long doctorId,
            @NotNull @Future(message = "must be a future date and time") LocalDateTime slotTime) {
    }

    public record RescheduleRequest(
            @NotNull @Future(message = "must be a future date and time") LocalDateTime slotTime) {
    }

    public record AppointmentResponse(
            Long id, String patientName, String patientPhone, Long doctorId, String doctorName,
            Long hospitalId, String hospitalName, LocalDateTime slotTime, AppointmentStatus status) {
    }

    // ------------------------------------------------------------------ ambulance

    public record AmbulanceCreateRequest(
            @NotBlank @Size(min = 2, max = 100) String patientName,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String contactPhone,
            @NotBlank @Size(max = 255) String pickupLocation,
            @NotNull Long hospitalId) {
    }

    public record AmbulanceStatusUpdate(@NotNull AmbulanceStatus status, @Min(0) Integer etaMinutes) {
    }

    public record AmbulanceResponse(
            Long id, String patientName, String pickupLocation, Long hospitalId, String hospitalName,
            AmbulanceStatus status, Integer etaMinutes, LocalDateTime createdAt, LocalDateTime dispatchedAt) {
    }

    /** Public tracking view: deliberately leaves out the patient's name and phone. */
    public record AmbulanceTrackingResponse(
            Long id, AmbulanceStatus status, Integer etaMinutes, String hospitalName, String hospitalPhone) {
    }

    // ------------------------------------------------------------------ notifications

    public record NotificationResponse(
            Long id, RecipientType recipientType, String message, Long hospitalId,
            boolean read, LocalDateTime createdAt) {
    }

    // ------------------------------------------------------------------ analytics

    public record OccupancyPoint(LocalDateTime recordedAt, Map<String, Integer> beds, int totalAvailable) {
    }

    public record OccupancyReport(
            Long hospitalId, String hospitalName, int days, Map<String, Integer> currentBeds,
            int currentTotalAvailable, List<OccupancyPoint> trend) {
    }

    public record AppointmentReport(
            Long hospitalId, String hospitalName, LocalDate from, LocalDate to, long total,
            Map<AppointmentStatus, Long> byStatus, Map<String, Long> byDoctor) {
    }

    public record ResponseTimeReport(
            long totalRequests, long dispatchedRequests, Double averageMinutesToDispatch,
            Map<AmbulanceStatus, Long> byStatus) {
    }

    // ------------------------------------------------------------------ auth

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 40) @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                    message = "may contain letters, digits, '.', '_' and '-' only") String username,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
            @NotNull Role role,
            Long hospitalId) {
    }

    public record AuthResponse(
            String token, String tokenType, long expiresInSeconds, String username, Role role, Long hospitalId) {
    }

    public record UserResponse(Long id, String username, Role role, Long hospitalId) {
    }
}
