package com.fbadsautomation.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Aspect for auditing Facebook export operations
 * Logs export events for security, compliance, and analytics
 *
 * Security: Tracks user actions for audit trail
 * Performance: Minimal overhead, async logging
 * Maintainability: Centralized audit logic
 */
@Slf4j
@Aspect
@Component
public class FacebookExportAuditAspect {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log export initiation
     */
    @Before("execution(* com.fbadsautomation.service.FacebookExportService.exportAdToFacebookTemplate(..)) || " +
            "execution(* com.fbadsautomation.service.FacebookExportService.exportMultipleAdsToFacebookTemplate(..)) || " +
            "execution(* com.fbadsautomation.service.FacebookExportService.exportAdsBulk(..))")
    public void logExportStart(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String adInfo = extractAdInfo(args);

        log.info("📤 FACEBOOK EXPORT INITIATED - User: {}, Method: {}, Ads: {}, Timestamp: {}",
                username, methodName, adInfo, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Log successful export
     */
    @AfterReturning(
        pointcut = "execution(* com.fbadsautomation.service.FacebookExportService.exportAdToFacebookTemplate(..)) || " +
                   "execution(* com.fbadsautomation.service.FacebookExportService.exportMultipleAdsToFacebookTemplate(..)) || " +
                   "execution(* com.fbadsautomation.service.FacebookExportService.exportAdsBulk(..))",
        returning = "result"
    )
    public void logExportSuccess(JoinPoint joinPoint, Object result) {
        String username = getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String adInfo = extractAdInfo(args);
        long fileSize = extractFileSize(result);

        log.info("✅ FACEBOOK EXPORT SUCCESS - User: {}, Method: {}, Ads: {}, Size: {} bytes, Timestamp: {}",
                username, methodName, adInfo, fileSize, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Log failed export attempts
     */
    @AfterThrowing(
        pointcut = "execution(* com.fbadsautomation.service.FacebookExportService.exportAdToFacebookTemplate(..)) || " +
                   "execution(* com.fbadsautomation.service.FacebookExportService.exportMultipleAdsToFacebookTemplate(..)) || " +
                   "execution(* com.fbadsautomation.service.FacebookExportService.exportAdsBulk(..))",
        throwing = "exception"
    )
    public void logExportFailure(JoinPoint joinPoint, Exception exception) {
        String username = getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String adInfo = extractAdInfo(args);

        log.error("❌ FACEBOOK EXPORT FAILED - User: {}, Method: {}, Ads: {}, Error: {}, Timestamp: {}",
                username, methodName, adInfo, exception.getMessage(), LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Log preview operations (less critical, debug level)
     */
    @AfterReturning(
        pointcut = "execution(* com.fbadsautomation.service.FacebookExportService.previewFacebookFormat(..)) || " +
                   "execution(* com.fbadsautomation.service.FacebookExportService.previewMultipleFacebookFormat(..))"
    )
    public void logPreview(JoinPoint joinPoint) {
        String username = getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String adInfo = extractAdInfo(args);

        log.debug("👁️ FACEBOOK PREVIEW - User: {}, Method: {}, Ads: {}, Timestamp: {}",
                username, methodName, adInfo, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Extract current username from security context
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not extract username: {}", e.getMessage());
        }
        return "ANONYMOUS";
    }

    /**
     * Extract ad information from method arguments
     */
    private String extractAdInfo(Object[] args) {
        if (args == null || args.length == 0) {
            return "UNKNOWN";
        }

        Object firstArg = args[0];

        // Single ad ID
        if (firstArg instanceof Long) {
            return "ID:" + firstArg;
        }

        // Multiple ad IDs
        if (firstArg instanceof List) {
            @SuppressWarnings("unchecked")
            List<Long> adIds = (List<Long>) firstArg;
            return "Count:" + adIds.size() + ", IDs:" + (adIds.size() <= 5 ? adIds : adIds.subList(0, 5) + "...");
        }

        return "UNKNOWN";
    }

    /**
     * Extract file size from ResponseEntity
     */
    private long extractFileSize(Object result) {
        if (result == null) {
            return 0;
        }

        try {
            // ResponseEntity<byte[]>
            if (result.getClass().getName().contains("ResponseEntity")) {
                Object body = result.getClass().getMethod("getBody").invoke(result);
                if (body instanceof byte[]) {
                    return ((byte[]) body).length;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract file size: {}", e.getMessage());
        }

        return 0;
    }
}
