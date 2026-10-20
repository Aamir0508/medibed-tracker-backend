package com.medibed.tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibed.tracker.model.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
