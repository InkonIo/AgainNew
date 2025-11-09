package com.chatalyst.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.chatalyst.backend.security.services.UserPrincipal;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;
import jakarta.servlet.http.HttpServletRequest;

@Component
@Slf4j
public class JwtUtils {
    
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;
    
    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Добавляем userId в claims для извлечения позже
        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .claim("userId", userPrincipal.getId()) // ← ДОБАВИЛИ userId!
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }
    
    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String getEmailFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public String getUserNameFromJwtToken(String token) {
        return getEmailFromJwtToken(token);
    }
    
    /**
     * Извлечь userId из токена
     */
    public Long getUserIdFromJwtToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("userId", Long.class);
    }
    
    /**
     * ✅ НОВЫЙ МЕТОД: Извлечь userId из Authentication
     * Используется в контроллерах для получения ID текущего пользователя
     */
    public Long extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
    
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && validateJwtToken(jwt)) {
            try {
                // Теперь извлекаем userId из claim, а не из subject
                return getUserIdFromJwtToken(jwt);
            } catch (Exception e) {
                log.error("Cannot extract userId from JWT: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT token validation error: {}", e.getMessage());
        }
        
        return false;
    }
}