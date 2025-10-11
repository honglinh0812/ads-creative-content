package com.fbadsautomation.config;

import com.fbadsautomation.security.JwtTokenProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration

public class RateLimitingConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor(jwtTokenProvider);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login-app",
                                   "/api/auth/forgot-password", "/api/auth/reset-password",
                                   "/api/public/**", "/api/health", "/api/images/**");
    }

    public static class RateLimitingInterceptor implements HandlerInterceptor {

        private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> userRequestCounts = new ConcurrentHashMap<>();
        private final JwtTokenProvider jwtTokenProvider;

        // Rate limit configurations per endpoint type
        private final Map<String, Integer> endpointLimits = new HashMap<>();
        private final int DEFAULT_REQUESTS_PER_MINUTE = 60;
        private final int AI_GENERATION_REQUESTS_PER_MINUTE = 10;
        private final int UPLOAD_REQUESTS_PER_MINUTE = 20;
        private final int USER_REQUESTS_PER_MINUTE = 100;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public RateLimitingInterceptor(JwtTokenProvider jwtTokenProvider) {
            this.jwtTokenProvider = jwtTokenProvider;
            initializeEndpointLimits();

            // Reset counters every minute
            scheduler.scheduleAtFixedRate(() -> {
                requestCounts.clear();
                userRequestCounts.clear();
            }, 1, 1, TimeUnit.MINUTES);
        }

        private void initializeEndpointLimits() {
            endpointLimits.put("/api/ads/generate", AI_GENERATION_REQUESTS_PER_MINUTE);
            endpointLimits.put("/api/upload", UPLOAD_REQUESTS_PER_MINUTE);
            endpointLimits.put("/api/campaigns", DEFAULT_REQUESTS_PER_MINUTE);
            endpointLimits.put("/api/ads", DEFAULT_REQUESTS_PER_MINUTE);
            // Facebook export rate limiting: 10 exports per minute per user
            endpointLimits.put("/api/facebook-export", 10);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String clientIp = getClientIpAddress(request);
            String requestURI = request.getRequestURI();

            // Check IP-based rate limiting
            String ipKey = clientIp + ":" + requestURI;
            int endpointLimit = getEndpointLimit(requestURI);

            AtomicInteger ipCount = requestCounts.computeIfAbsent(ipKey, k -> new AtomicInteger(0));
            if (ipCount.incrementAndGet() > endpointLimit) {
                log.warn("IP rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestURI);
                return sendRateLimitResponse(response, "IP rate limit exceeded");
            }

            // Check user-based rate limiting for authenticated requests
            String userId = getUserIdFromRequest(request);
            if (userId != null) {
                String userKey = "user:" + userId;
                AtomicInteger userCount = userRequestCounts.computeIfAbsent(userKey, k -> new AtomicInteger(0));
                if (userCount.incrementAndGet() > USER_REQUESTS_PER_MINUTE) {
                    log.warn("User rate limit exceeded for user: {} on endpoint: {}", userId, requestURI);
                    return sendRateLimitResponse(response, "User rate limit exceeded");
                }
            }

            return true;
        }

        private int getEndpointLimit(String requestURI) {
            for (Map.Entry<String, Integer> entry : endpointLimits.entrySet()) {
                if (requestURI.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return DEFAULT_REQUESTS_PER_MINUTE;
        }

        private String getUserIdFromRequest(HttpServletRequest request) {
            try {
                String token = getJwtFromRequest(request);
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromJWT(token);
                    return userId != null ? userId.toString() : null;
                }
            } catch (Exception e) {
                log.debug("Could not extract user ID from request: {}", e.getMessage());
            }
            return null;
        }

        private String getJwtFromRequest(HttpServletRequest request) {
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return null;
        }

        private boolean sendRateLimitResponse(HttpServletResponse response, String message) throws Exception {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60"); // Retry after 60 seconds
            response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"message\":\"Please try again later\",\"retryAfter\":60}",
                message
            ));
            return false;
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
    }
}
