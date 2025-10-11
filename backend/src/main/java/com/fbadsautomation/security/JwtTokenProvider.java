package com.fbadsautomation.security;

import io.jsonwebtoken.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:defaultSecretKey}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600000}") // Changed to 1 hour (3600000ms)
    private int jwtExpirationInMs;

    @Value("${app.jwt.refresh.expiration:604800000}") // 7 days for refresh token
    private int refreshTokenExpirationInMs;

    // Token blacklist for logout functionality
    private final Set<String> blacklistedTokens = new ConcurrentSkipListSet<>();
    private final Set<String> blacklistedRefreshTokens = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<String, Long> tokenCreationTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> refreshTokenToUserId = new ConcurrentHashMap<>();

    // Scheduled executor for cleanup
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        // Schedule cleanup every hour
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String jti = generateSecureRandomString(); // JWT ID for tracking

        String token = Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("ads-creative-content")
                .setId(jti) // Unique token ID
                .claim("userId", userId)
                .claim("type", "access") // Token type
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

        // Track token creation time
        tokenCreationTimes.put(token, now.getTime());
        log.debug("Generated access token for user: {} with JTI: {}", userId, jti);

        return token;
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

        String jti = generateSecureRandomString();

        String refreshToken = Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("ads-creative-content")
                .setId(jti)
                .claim("userId", userId)
                .claim("type", "refresh")
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

        // Store refresh token mapping
        refreshTokenToUserId.put(refreshToken, userId.toString());
        log.debug("Generated refresh token for user: {} with JTI: {}", userId, jti);

        return refreshToken;
    }

    public TokenPair generateTokenPair(Long userId) {
        String accessToken = generateToken(userId);
        String refreshToken = generateRefreshToken(userId);
        return new TokenPair(accessToken, refreshToken);
    }

    private String generateSecureRandomString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        return validateToken(authToken, "access");
    }

    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken, "refresh") && !blacklistedRefreshTokens.contains(refreshToken);
    }

    private boolean validateToken(String token, String expectedType) {
        try {
            // Check if token is blacklisted
            if ("access".equals(expectedType) && blacklistedTokens.contains(token)) {
                log.warn("Access token is blacklisted");
                return false;
            }

            if ("refresh".equals(expectedType) && blacklistedRefreshTokens.contains(token)) {
                log.warn("Refresh token is blacklisted");
                return false;
            }

            // Validate token structure and signature
            if (!StringUtils.hasText(token)) {
                log.warn("JWT token is empty or null");
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            // Additional validation checks
            if (!claims.getIssuer().equals("ads-creative-content")) {
                log.warn("Invalid JWT issuer: {}", claims.getIssuer());
                return false;
            }

            // Check token type
            String tokenType = (String) claims.get("type");
            if (!expectedType.equals(tokenType)) {
                log.warn("Invalid token type. Expected: {}, Got: {}", expectedType, tokenType);
                return false;
            }

            // Check if token has required claims
            if (claims.get("userId") == null) {
                log.warn("Missing userId claim in JWT");
                return false;
            }

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    public String refreshAccessToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(refreshToken)
                    .getBody();

            Long userId = Long.parseLong(claims.getSubject());
            return generateToken(userId);
        } catch (Exception ex) {
            log.error("Error refreshing access token: {}", ex.getMessage());
            throw new IllegalArgumentException("Cannot refresh access token");
        }
    }

    /**
     * Blacklist access token (for logout functionality)
     */
    public void blacklistToken(String token) {
        if (StringUtils.hasText(token)) {
            blacklistedTokens.add(token);
            tokenCreationTimes.remove(token);
            log.info("Access token blacklisted successfully");
        }
    }

    /**
     * Blacklist refresh token (for logout functionality)
     */
    public void blacklistRefreshToken(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            blacklistedRefreshTokens.add(refreshToken);
            refreshTokenToUserId.remove(refreshToken);
            log.info("Refresh token blacklisted successfully");
        }
    }

    /**
     * Blacklist both access and refresh tokens (for complete logout)
     */
    public void blacklistTokenPair(String accessToken, String refreshToken) {
        blacklistToken(accessToken);
        blacklistRefreshToken(refreshToken);
    }

    /**
     * Clean up expired tokens from blacklist and tracking
     */
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();

        // Clean up access tokens
        tokenCreationTimes.entrySet().removeIf(entry -> {
            boolean isExpired = (currentTime - entry.getValue()) > jwtExpirationInMs;
            if (isExpired) {
                blacklistedTokens.remove(entry.getKey());
            }
            return isExpired;
        });

        // Clean up refresh tokens
        refreshTokenToUserId.entrySet().removeIf(entry -> {
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(jwtSecret)
                        .parseClaimsJws(entry.getKey())
                        .getBody();
                boolean isExpired = claims.getExpiration().before(new Date());
                if (isExpired) {
                    blacklistedRefreshTokens.remove(entry.getKey());
                }
                return isExpired;
            } catch (Exception e) {
                // If token can't be parsed, remove it
                blacklistedRefreshTokens.remove(entry.getKey());
                return true;
            }
        });

        log.info("Cleaned up expired tokens. Remaining access tokens: {}, refresh tokens: {}",
                tokenCreationTimes.size(), refreshTokenToUserId.size());
    }

    /**
     * Token pair class for returning both access and refresh tokens
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
