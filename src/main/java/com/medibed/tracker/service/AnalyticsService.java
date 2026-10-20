package com.medibed.tracker.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.dto.Dtos.AppointmentReport;
import com.medibed.tracker.dto.Dtos.DoctorResponse;
import com.medibed.tracker.dto.Dtos.OccupancyPoint;
import com.medibed.tracker.dto.Dtos.OccupancyReport;
import com.medibed.tracker.dto.Dtos.ResponseTimeReport;
import com.medibed.tracker.exception.BadRequestException;
import com.medibed.tracker.exception.ResourceNotFoundException;
import com.medibed.tracker.model.AmbulanceRequest;
import com.medibed.tracker.model.AmbulanceStatus;
import com.medibed.tracker.model.Appointment;
import com.medibed.tracker.model.AppointmentStatus;
import com.medibed.tracker.model.Hospital;
import com.medibed.tracker.repository.AmbulanceRequestRepository;
import com.medibed.tracker.repository.AppointmentRepository;
import com.medibed.tracker.repository.BedSnapshotRepository;
import com.medibed.tracker.repository.HospitalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final HospitalRepository hospitals;
    private final BedSnapshotRepository snapshots;
    private final AppointmentRepository appointments;
    private final AmbulanceRequestRepository ambulances;
    private final DoctorService doctorService;

    /** Bed availability over the last N days, built from the snapshot written on every bed update. */
    public OccupancyReport occupancy(Long hospitalId, int days) {
        if (days < 1 || days > 90) {
            throw new BadRequestException("days", "must be between 1 and 90");
        }
        Hospital h = hospital(hospitalId);
        List<OccupancyPoint> trend = snapshots
                .findByHospitalIdAndRecordedAtAfterOrderByRecordedAtAsc(hospitalId, LocalDateTime.now().minusDays(days))
                .stream()
                .map(s -> new OccupancyPoint(s.getRecordedAt(),
                        Mapper.beds(s.getGeneralBeds(), s.getIcuBeds(), s.getOxygenBeds(), s.getNicuBeds(),
                                s.getEmergencyBeds()),
                        s.getGeneralBeds() + s.getIcuBeds() + s.getOxygenBeds() + s.getNicuBeds()
                                + s.getEmergencyBeds()))
                .toList();
        return new OccupancyReport(h.getId(), h.getName(), days, Mapper.beds(h), Mapper.totalBeds(h), trend);
    }

    /** Appointment volume between two dates (defaults: 30 days back to 30 days ahead). */
    public AppointmentReport appointments(Long hospitalId, LocalDate from, LocalDate to) {
        Hospital h = hospital(hospitalId);
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now().plusDays(30);
        if (end.isBefore(start)) {
            throw new BadRequestException("to", "must not be before 'from'");
        }
        List<Appointment> list = appointments.findByHospitalIdAndSlotTimeBetween(
                hospitalId, start.atStartOfDay(), end.atTime(LocalTime.MAX));
        Map<AppointmentStatus, Long> byStatus = list.stream().collect(Collectors.groupingBy(
                Appointment::getStatus, () -> new EnumMap<>(AppointmentStatus.class), Collectors.counting()));
        Map<String, Long> byDoctor = list.stream().collect(Collectors.groupingBy(
                a -> a.getDoctor().getName(), TreeMap::new, Collectors.counting()));
        return new AppointmentReport(h.getId(), h.getName(), start, end, list.size(), byStatus, byDoctor);
    }

    public List<DoctorResponse> topDoctors(int limit) {
        return doctorService.topRatedNetwork(limit);
    }

    /** Average time between an ambulance being requested and being dispatched. */
    public ResponseTimeReport ambulanceResponseTimes() {
        List<AmbulanceRequest> all = ambulances.findAll();
        Map<AmbulanceStatus, Long> byStatus = all.stream().collect(Collectors.groupingBy(
                AmbulanceRequest::getStatus, () -> new EnumMap<>(AmbulanceStatus.class), Collectors.counting()));
        List<AmbulanceRequest> dispatched = ambulances.findByDispatchedAtIsNotNull();
        Double avg = dispatched.isEmpty() ? null
                : Math.round(dispatched.stream()
                        .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getDispatchedAt()).toSeconds() / 60.0)
                        .average().orElse(0) * 10.0) / 10.0;
        return new ResponseTimeReport(all.size(), dispatched.size(), avg, byStatus);
    }

    private Hospital hospital(Long id) {
        return hospitals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hospital", id));
    }
}
