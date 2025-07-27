package com.fbadsautomation.security;

import io.jsonwebtoken.*;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component

public class JwtTokenProvider {

    @Value("${app.jwt.secret:defaultSecretKey}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private int jwtExpirationInMs;

    // Token blacklist for logout functionality
    private final Set<String> blacklistedTokens = new ConcurrentSkipListSet<>(); // Track token creation times for additional security
    private final ConcurrentHashMap<String, Long> tokenCreationTimes = new ConcurrentHashMap<>();

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String token = Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("ads-creative-content") // Add issuer for additional security
                .claim("userId", userId) // Add explicit userId claim
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
        // Track token creation time
        tokenCreationTimes.put(token, now.getTime());

        return token;
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            // Check if token is blacklisted
            if (blacklistedTokens.contains(authToken)) {
                log.warn("Token is blacklisted");
                return false;
            }

            // Validate token structure and signature
            if (!StringUtils.hasText(authToken)) {
                log.warn("JWT token is empty or null");
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(authToken)
                    .getBody();
        // Additional validation checks
            if (!claims.getIssuer().equals("ads-creative-content")) {
                log.warn("Invalid JWT issuer");
                return false;
            }

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Blacklist a token (for logout functionality)
     */
    public void blacklistToken(String token) {
        if (StringUtils.hasText(token)) {
            blacklistedTokens.add(token);
            tokenCreationTimes.remove(token);
            log.info("Token blacklisted successfully");
        }
    }

    /**
     * Clean up expired tokens from blacklist and tracking
     */
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        tokenCreationTimes.entrySet().removeIf(entry -> {
            boolean isExpired = (currentTime - entry.getValue()) > jwtExpirationInMs;
            if (isExpired) {
                blacklistedTokens.remove(entry.getKey());
            }
            return isExpired;
        });
    }
}
