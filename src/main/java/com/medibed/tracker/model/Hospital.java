package com.medibed.tracker.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hospitals")
@Getter
@Setter
@NoArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String area;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "emergency_phone", length = 30)
    private String emergencyPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HospitalType type;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(nullable = false)
    private boolean preferred;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "general_beds", nullable = false)
    private int generalBeds;

    @Column(name = "icu_beds", nullable = false)
    private int icuBeds;

    @Column(name = "oxygen_beds", nullable = false)
    private int oxygenBeds;

    @Column(name = "nicu_beds", nullable = false)
    private int nicuBeds;

    @Column(name = "emergency_beds", nullable = false)
    private int emergencyBeds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_facilities", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "facility", length = 100)
    private List<String> facilities = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
