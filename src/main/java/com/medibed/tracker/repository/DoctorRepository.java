package com.medibed.tracker.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.Doctor;
import com.medibed.tracker.model.DoctorStatus;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByHospitalId(Long hospitalId);

    List<Doctor> findByHospitalIdAndStatus(Long hospitalId, DoctorStatus status);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    List<Doctor> findByHospitalIdOrderByRatingDescReviewCountDesc(Long hospitalId, Pageable pageable);

    List<Doctor> findAllByOrderByRatingDescReviewCountDesc(Pageable pageable);
}
