package com.fbadsautomation.security;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip strict security headers for image endpoints to allow browser loading
        String requestURI = httpRequest.getRequestURI();
        boolean isImageRequest = requestURI != null && requestURI.startsWith("/api/images/");

        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data: https:; " +
            "connect-src 'self' https:; " +
            "media-src 'self'; " +
            "object-src 'none'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'"
        );

        // Referrer Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy (formerly Feature Policy)
        httpResponse.setHeader("Permissions-Policy",
            "camera=(), " +
            "microphone=(), " +
            "geolocation=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "gyroscope=(), " +
            "accelerometer=()"
        );

        // Additional security headers
        httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");

        // Relax CORP headers for image requests to allow browser embedding
        if (isImageRequest) {
            // Allow cross-origin for images (needed for <img> tags in frontend)
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
            // Don't set COEP for images
        } else {
            // Strict headers for other endpoints
            httpResponse.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
            httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        }

        // Remove server information
        httpResponse.setHeader("Server", "");

        chain.doFilter(request, response);
    }
}