package com.medibed.tracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.AmbulanceRequest;

public interface AmbulanceRequestRepository extends JpaRepository<AmbulanceRequest, Long> {

    List<AmbulanceRequest> findByDispatchedAtIsNotNull();
}
