package com.medibed.tracker.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.Appointment;
import com.medibed.tracker.model.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdOrderBySlotTimeAsc(Long doctorId);

    boolean existsByDoctorIdAndSlotTimeAndStatusNot(Long doctorId, LocalDateTime slotTime, AppointmentStatus status);

    List<Appointment> findByHospitalIdAndSlotTimeBetween(Long hospitalId, LocalDateTime from, LocalDateTime to);
}
