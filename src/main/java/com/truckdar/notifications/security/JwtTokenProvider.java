package com.truckdar.notifications.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${app.jwt.secret:dHJ1Y2tkYXItdXNlci1pZGVudGl0eS1zZWNyZXQta2V5LWJhc2U2NC1lbmNvZGVkLWF0LWxlYXN0LTI1Ni1iaXRz}") String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

        String userIdStr = claims.getSubject();
        UUID userId = null;
        if (userIdStr != null) {
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        if (role != null && !role.isBlank()) {
            String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities = List.of(new SimpleGrantedAuthority(roleName));
        }

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email(email)
                .role(role)
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
}
