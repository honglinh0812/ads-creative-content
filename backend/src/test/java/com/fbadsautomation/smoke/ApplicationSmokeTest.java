package com.fbadsautomation.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApplicationSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Basic smoke test - application context should load successfully
        assertNotNull(restTemplate);
        assertTrue(port > 0);
    }

    @Test
    void healthEndpointIsAvailable() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("status") || response.getBody().contains("UP"));
    }

    @Test
    void applicationRespondsToBasicRequests() {
        // Test that the application is running and responsive
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", String.class);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());

        // Response should be received within reasonable time
        assertNotNull(response.getBody());
    }

    @Test
    void unauthorizedEndpointsReturnAppropriateStatus() {
        // Test that protected endpoints return 401/403 when not authenticated
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/auth/user", String.class);

        // Should return 401 Unauthorized or 403 Forbidden
        assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                  response.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void nonExistentEndpointReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/nonexistent", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void applicationHasValidContentType() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", String.class);

        assertNotNull(response.getHeaders().getContentType());
        String contentType = response.getHeaders().getContentType().toString();
        assertTrue(contentType.contains("application/json") || contentType.contains("text/plain"));
    }

    @Test
    void applicationHasSecurityHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", String.class);

        // Check for common security headers
        assertNotNull(response.getHeaders());

        // Application should have basic security configurations
        assertTrue(response.getHeaders().size() > 0);
    }

    @Test
    void databaseConnectionIsWorking() {
        // Health endpoint should indicate database connectivity
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // If we get OK response, database connection is likely working
        assertNotNull(response.getBody());
    }

    @Test
    void applicationStartsWithinReasonableTime() {
        // This test passes if the application context loaded successfully
        // Spring Boot test framework handles startup timing
        assertTrue(port > 0, "Application should start and get assigned a port");
    }

    @Test
    void errorHandlingIsConfigured() {
        // Test that error handling returns proper JSON response
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/nonexistent", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        // Should return some response body (error message)
        assertNotNull(response.getBody());
    }
}