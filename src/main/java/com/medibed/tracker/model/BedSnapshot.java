package com.medibed.tracker.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** One row per bed-count change; powers the occupancy trend analytics. */
@Entity
@Table(name = "bed_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class BedSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Hospital hospital;

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

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
    }

    public static BedSnapshot of(Hospital h) {
        BedSnapshot s = new BedSnapshot();
        s.hospital = h;
        s.generalBeds = h.getGeneralBeds();
        s.icuBeds = h.getIcuBeds();
        s.oxygenBeds = h.getOxygenBeds();
        s.nicuBeds = h.getNicuBeds();
        s.emergencyBeds = h.getEmergencyBeds();
        return s;
    }
}
