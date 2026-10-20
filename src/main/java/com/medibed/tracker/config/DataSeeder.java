package com.medibed.tracker.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibed.tracker.dto.Dtos.DoctorRequest;
import com.medibed.tracker.dto.Dtos.HospitalRequest;
import com.medibed.tracker.dto.Dtos.HospitalResponse;
import com.medibed.tracker.model.AdminUser;
import com.medibed.tracker.model.Role;
import com.medibed.tracker.repository.AdminUserRepository;
import com.medibed.tracker.repository.HospitalRepository;
import com.medibed.tracker.service.DoctorService;
import com.medibed.tracker.service.HospitalService;

import lombok.RequiredArgsConstructor;

/** On first start: creates the SUPER_ADMIN and loads the 28 Mumbai hospitals and their doctors. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AdminUserRepository admins;
    private final HospitalRepository hospitals;
    private final HospitalService hospitalService;
    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;

    @Value("${medibed.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${medibed.admin.username}")
    private String adminUsername;

    @Value("${medibed.admin.password}")
    private String adminPassword;

    public record SeedDoctor(String hospital, DoctorRequest doctor) {
    }

    @Override
    public void run(String... args) throws Exception {
        if (admins.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.SUPER_ADMIN);
            admins.save(admin);
            log.warn("Created SUPER_ADMIN '{}'. Change medibed.admin.password / ADMIN_PASSWORD before deploying.",
                    adminUsername);
        }
        if (seedEnabled && hospitals.count() == 0) {
            seedHospitalsAndDoctors();
        }
    }

    private void seedHospitalsAndDoctors() throws Exception {
        List<HospitalRequest> hospitalSeed = read("seed/hospitals.json", new TypeReference<List<HospitalRequest>>() { });
        Map<String, Long> idByName = new HashMap<>();
        for (HospitalRequest r : hospitalSeed) {
            HospitalResponse created = hospitalService.create(r);
            idByName.put(created.name(), created.id());
        }
        List<SeedDoctor> doctorSeed = read("seed/doctors.json", new TypeReference<List<SeedDoctor>>() { });
        int added = 0;
        for (SeedDoctor sd : doctorSeed) {
            Long hospitalId = idByName.get(sd.hospital());
            if (hospitalId != null) {
                doctorService.add(hospitalId, sd.doctor());
                added++;
            }
        }
        log.info("Seeded {} hospitals and {} doctors", hospitalSeed.size(), added);
    }

    private <T> T read(String path, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        }
    }
}
