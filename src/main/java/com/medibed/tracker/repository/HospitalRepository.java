package com.medibed.tracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.model.HospitalType;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByAreaIgnoreCase(String area);

    List<Hospital> findByType(HospitalType type);

    List<Hospital> findByNameContainingIgnoreCase(String keyword);

    List<Hospital> findByEmergencyBedsGreaterThan(int beds);
}
