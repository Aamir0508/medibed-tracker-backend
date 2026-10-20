package com.medibed.tracker.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.medibed.tracker.model.Role;
import com.medibed.tracker.repository.AmbulanceRequestRepository;
import com.medibed.tracker.repository.AppointmentRepository;
import com.medibed.tracker.repository.DoctorRepository;
import com.medibed.tracker.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Used from @PreAuthorize("@access.hospital(#id)") expressions.
 * SUPER_ADMIN may touch anything; a HOSPITAL_ADMIN only the hospital their account belongs to.
 * If the target does not exist, a SUPER_ADMIN gets through so the service can answer 404.
 */
@Component("access")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessGuard {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final AmbulanceRequestRepository ambulances;
    private final NotificationRepository notifications;

    public boolean hospital(Long hospitalId) {
        AppUserPrincipal user = current();
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.SUPER_ADMIN
                || (user.getHospitalId() != null && user.getHospitalId().equals(hospitalId));
    }

    public boolean doctor(Long doctorId) {
        AppUserPrincipal user = current();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        return doctors.findById(doctorId).map(d -> hospital(d.getHospital().getId())).orElse(false);
    }

    public boolean appointment(Long id) {
        AppUserPrincipal user = current();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        return appointments.findById(id).map(a -> hospital(a.getHospital().getId())).orElse(false);
    }

    public boolean ambulance(Long id) {
        AppUserPrincipal user = current();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        return ambulances.findById(id).map(a -> hospital(a.getHospital().getId())).orElse(false);
    }

    public boolean notification(Long id) {
        AppUserPrincipal user = current();
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        return notifications.findById(id).map(n -> hospital(n.getHospital().getId())).orElse(false);
    }

    private AppUserPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p) {
            return p;
        }
        return null;
    }
}
