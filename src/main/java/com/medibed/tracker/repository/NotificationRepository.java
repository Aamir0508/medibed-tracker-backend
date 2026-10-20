package com.medibed.tracker.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    long deleteByReadTrueAndCreatedAtBefore(LocalDateTime cutoff);
}
