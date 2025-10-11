package com.fbadsautomation.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class QueryPerformanceAspect {

    @Around("execution(* com.fbadsautomation.repository..*(..))")
    public Object logRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 100) { // Log queries taking more than 100ms
                log.warn("🐌 SLOW REPOSITORY QUERY - Method: {}, Execution Time: {}ms", methodName, executionTime);
            } else if (executionTime > 50) {
                log.info("⚠️ MEDIUM REPOSITORY QUERY - Method: {}, Execution Time: {}ms", methodName, executionTime);
            } else {
                log.debug("✅ Repository Query - Method: {}, Execution Time: {}ms", methodName, executionTime);
            }

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ FAILED REPOSITORY QUERY - Method: {}, Execution Time: {}ms, Error: {}",
                     methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.fbadsautomation.service..*(..))")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Only log for specific performance-critical services
        if (!isPerformanceCriticalService(className)) {
            return joinPoint.proceed();
        }

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 1000) { // Log service calls taking more than 1 second
                log.warn("🐌 SLOW SERVICE METHOD - Class: {}, Method: {}, Execution Time: {}ms",
                        className, methodName, executionTime);
            } else if (executionTime > 500) {
                log.info("⚠️ MEDIUM SERVICE METHOD - Class: {}, Method: {}, Execution Time: {}ms",
                        className, methodName, executionTime);
            } else {
                log.debug("✅ Service Method - Class: {}, Method: {}, Execution Time: {}ms",
                         className, methodName, executionTime);
            }

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ FAILED SERVICE METHOD - Class: {}, Method: {}, Execution Time: {}ms, Error: {}",
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.fbadsautomation.controller..*(..))")
    public Object logControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 2000) { // Log controller calls taking more than 2 seconds
                log.warn("🐌 SLOW CONTROLLER METHOD - Class: {}, Method: {}, Execution Time: {}ms",
                        className, methodName, executionTime);
            } else if (executionTime > 1000) {
                log.info("⚠️ MEDIUM CONTROLLER METHOD - Class: {}, Method: {}, Execution Time: {}ms",
                        className, methodName, executionTime);
            } else {
                log.debug("✅ Controller Method - Class: {}, Method: {}, Execution Time: {}ms",
                         className, methodName, executionTime);
            }

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ FAILED CONTROLLER METHOD - Class: {}, Method: {}, Execution Time: {}ms, Error: {}",
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    private boolean isPerformanceCriticalService(String className) {
        return className.contains("AI") ||
               className.contains("Campaign") ||
               className.contains("Ad") ||
               className.contains("Content") ||
               className.contains("Database") ||
               className.contains("Performance") ||
               className.contains("Async");
    }
}