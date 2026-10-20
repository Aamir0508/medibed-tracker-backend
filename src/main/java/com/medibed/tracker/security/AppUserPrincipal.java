package com.medibed.tracker.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.medibed.tracker.model.AdminUser;
import com.medibed.tracker.model.Role;

/** The logged-in admin as Spring Security sees them. */
public class AppUserPrincipal implements UserDetails {

    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Long hospitalId;

    public AppUserPrincipal(AdminUser user) {
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.hospitalId = user.getHospital() == null ? null : user.getHospital().getId();
    }

    public Role getRole() {
        return role;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
