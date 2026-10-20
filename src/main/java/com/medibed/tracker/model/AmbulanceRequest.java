package com.medibed.tracker.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ambulance_requests")
@Getter
@Setter
@NoArgsConstructor
public class AmbulanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_name", nullable = false, length = 100)
    private String patientName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbulanceStatus status = AmbulanceStatus.REQUESTED;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
