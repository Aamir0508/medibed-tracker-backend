package com.medibed.tracker.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.AppointmentRequest;
import com.medibed.tracker.dto.Dtos.AppointmentResponse;
import com.medibed.tracker.exception.BadRequestException;
import com.medibed.tracker.exception.ConflictException;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.Appointment;
import com.medibed.tracker.model.AppointmentStatus;
import com.medibed.tracker.model.Doctor;
import com.medibed.tracker.model.DoctorStatus;
import com.medibed.tracker.repository.AppointmentRepository;
import com.medibed.tracker.repository.DoctorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private static final DateTimeFormatter PRETTY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final NotificationService notifications;

    @Transactional
    public AppointmentResponse book(AppointmentRequest r) {
        Doctor doctor = doctors.findById(r.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", r.doctorId()));
        checkSlot(doctor, r.slotTime(), null);

        Appointment a = new Appointment();
        a.setPatientName(r.patientName().trim());
        a.setPatientPhone(r.patientPhone().trim());
        a.setDoctor(doctor);
        a.setHospital(doctor.getHospital());
        a.setSlotTime(r.slotTime());
        a.setStatus(AppointmentStatus.BOOKED);
        a = appointments.save(a);

        notifications.notifyStaff(doctor.getHospital(), "New appointment: " + a.getPatientName() + " with "
                + doctor.getName() + " on " + a.getSlotTime().format(PRETTY));
        return Mapper.appointment(a);
    }

    public AppointmentResponse get(Long id) {
        return Mapper.appointment(find(id));
    }

    public List<AppointmentResponse> byDoctor(Long doctorId) {
        if (!doctors.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
        return appointments.findByDoctorIdOrderBySlotTimeAsc(doctorId).stream().map(Mapper::appointment).toList();
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, LocalDateTime newSlot) {
        Appointment a = find(id);
        if (a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("A " + a.getStatus().name().toLowerCase() + " appointment cannot be rescheduled");
        }
        if (!newSlot.equals(a.getSlotTime())) {
            checkSlot(a.getDoctor(), newSlot, a.getId());
        }
        a.setSlotTime(newSlot);
        a.setStatus(AppointmentStatus.RESCHEDULED);
        notifications.notifyStaff(a.getHospital(), "Appointment moved: " + a.getPatientName() + " with "
                + a.getDoctor().getName() + " is now " + newSlot.format(PRETTY));
        return Mapper.appointment(a);
    }

    /** Cancelling keeps the row (status CANCELLED) so analytics still count it. */
    @Transactional
    public void cancel(Long id) {
        Appointment a = find(id);
        if (a.getStatus() == AppointmentStatus.CANCELLED) {
            return;
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        notifications.notifyStaff(a.getHospital(), "Appointment cancelled: " + a.getPatientName() + " with "
                + a.getDoctor().getName() + " (" + a.getSlotTime().format(PRETTY) + ")");
    }

    // ------------------------------------------------------------------ helpers

    private void checkSlot(Doctor doctor, LocalDateTime slot, Long ignoreAppointmentId) {
        if (doctor.getStatus() == DoctorStatus.ON_LEAVE) {
            throw new ConflictException(doctor.getName() + " is on leave");
        }
        if (!Mapper.onShift(doctor.getAvailableFrom(), doctor.getAvailableTo(), slot.toLocalTime())) {
            throw new BadRequestException("slotTime", "is outside the doctor's shift ("
                    + doctor.getAvailableFrom() + " to " + doctor.getAvailableTo() + ")");
        }
        if (appointments.existsByDoctorIdAndSlotTimeAndStatusNot(doctor.getId(), slot, AppointmentStatus.CANCELLED)) {
            throw new ConflictException("That slot is already booked for " + doctor.getName());
        }
    }

    private Appointment find(Long id) {
        return appointments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }
}
