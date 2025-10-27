package com.chatalyst.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.chatalyst.backend.security.services.UserPrincipal;

import java.nio.charset.StandardCharsets;


import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {
    
    @Value("${JWT_SECRET}")
    private String jwtSecret;
    
    @Value("${JWT_EXPIRATION}")
    private int jwtExpirationMs;
    
    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Новый синтаксис для JJWT 0.12.x
        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }
    
       private SecretKey key() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
}
    
    public String getEmailFromJwtToken(String token) {
        // Новый синтаксис для JJWT 0.12.x
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    // НОВЫЙ МЕТОД: добавляем метод getUserNameFromJwtToken
    public String getUserNameFromJwtToken(String token) {
        // Поскольку в токене хранится email, возвращаем его же
        return getEmailFromJwtToken(token);
    }
    
    public boolean validateJwtToken(String authToken) {
        try {
            // Новый синтаксис для JJWT 0.12.x
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