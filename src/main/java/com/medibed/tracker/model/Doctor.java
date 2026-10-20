package com.medibed.tracker.model;

import java.time.LocalTime;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Hospital hospital;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String specialization;

    @Column(length = 150)
    private String qualification;

    @Column(length = 120)
    private String languages;

    @Column(length = 300)
    private String bio;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "available_from", nullable = false)
    private LocalTime availableFrom;

    @Column(name = "available_to", nullable = false)
    private LocalTime availableTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoctorStatus status = DoctorStatus.AVAILABLE;

    @Column(name = "queue_count", nullable = false)
    private int queueCount;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "emergency_ready", nullable = false)
    private boolean emergencyReady;

    @Column(name = "photo_url")
    private String photoUrl;
}
