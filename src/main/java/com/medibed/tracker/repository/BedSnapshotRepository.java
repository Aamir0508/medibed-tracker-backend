package com.medibed.tracker.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.BedSnapshot;

public interface BedSnapshotRepository extends JpaRepository<BedSnapshot, Long> {

    List<BedSnapshot> findByHospitalIdAndRecordedAtAfterOrderByRecordedAtAsc(Long hospitalId, LocalDateTime after);
}
