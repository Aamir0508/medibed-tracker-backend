package com.medibed.tracker.security;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;

/** Tokens invalidated by logout/refresh. Kept in memory until they would have expired anyway. */
@Component
public class TokenBlacklist {

    private final Map<String, Date> revoked = new ConcurrentHashMap<>();

    public void revoke(Claims claims) {
        Date now = new Date();
        revoked.entrySet().removeIf(e -> e.getValue().before(now));
        revoked.put(claims.getId(), claims.getExpiration());
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.containsKey(jti);
    }
}
