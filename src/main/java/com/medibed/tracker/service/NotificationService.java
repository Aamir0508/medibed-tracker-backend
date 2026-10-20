package com.medibed.tracker.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.NotificationResponse;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.model.Notification;
import com.medibed.tracker.model.RecipientType;
import com.medibed.tracker.repository.HospitalRepository;
import com.medibed.tracker.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final HospitalRepository hospitals;

    @Transactional
    public void notifyStaff(Hospital hospital, String message) {
        create(hospital, RecipientType.STAFF, message);
    }

    @Transactional
    public void notifyRequester(Hospital hospital, String message) {
        create(hospital, RecipientType.REQUESTER, message);
    }

    @Transactional
    public void notifyPatient(Hospital hospital, String message) {
        create(hospital, RecipientType.PATIENT, message);
    }

    private void create(Hospital hospital, RecipientType type, String message) {
        Notification n = new Notification();
        n.setHospital(hospital);
        n.setRecipientType(type);
        n.setMessage(message.length() > 255 ? message.substring(0, 255) : message);
        notifications.save(n);
    }

    public List<NotificationResponse> forHospital(Long hospitalId) {
        if (!hospitals.existsById(hospitalId)) {
            throw new ResourceNotFoundException("Hospital", hospitalId);
        }
        return notifications.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(Mapper::notification).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setRead(true);
        return Mapper.notification(n);
    }

    /** Nightly clean-up of old, already-read notifications (same rule as the DELETE in the SQL file). */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldReadNotifications() {
        long removed = notifications.deleteByReadTrueAndCreatedAtBefore(LocalDateTime.now().minusDays(30));
        log.info("Purged {} old read notifications", removed);
    }
}
