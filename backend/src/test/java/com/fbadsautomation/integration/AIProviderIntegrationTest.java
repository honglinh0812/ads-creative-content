package com.fbadsautomation.integration;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.exception.AIProviderException;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.service.AIProviderFallbackService;
import com.fbadsautomation.service.DeadLetterQueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class AIProviderIntegrationTest {

    @Autowired(required = false)
    private Map<String, AIProvider> aiProviders;

    @Autowired(required = false)
    private AIProviderFallbackService fallbackService;

    @Autowired(required = false)
    private DeadLetterQueueService deadLetterQueueService;

    @Test
    void testAIProvidersAreConfigured() {
        // Test that AI providers are properly configured
        assertNotNull(aiProviders, "AI providers map should be configured");

        // Check that at least one provider is available
        assertFalse(aiProviders.isEmpty(), "At least one AI provider should be configured");

        // Log available providers for debugging
        System.out.println("Available AI providers: " + aiProviders.keySet());
    }

    @Test
    void testFallbackServiceIsConfigured() {
        assertNotNull(fallbackService, "AI fallback service should be configured");
    }

    @Test
    void testDeadLetterQueueServiceIsConfigured() {
        assertNotNull(deadLetterQueueService, "Dead letter queue service should be configured");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_INTEGRATION_TEST", matches = "true")
    void testOpenAIProviderIntegration() {
        AIProvider openaiProvider = aiProviders.get("openai");
        assumeProviderAvailable(openaiProvider, "OpenAI");

        String prompt = "Create a compelling ad for a fitness app";
        int variations = 2;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.DOWNLOAD;

        assertDoesNotThrow(() -> {
            List<AdContent> content = openaiProvider.generateAdContent(prompt, variations, language, callToAction);

            assertNotNull(content);
            assertTrue(content.size() <= variations);

            for (AdContent ad : content) {
                assertNotNull(ad.getHeadline());
                assertNotNull(ad.getPrimaryText());
                assertFalse(ad.getHeadline().trim().isEmpty());
                assertFalse(ad.getPrimaryText().trim().isEmpty());

                // Basic content validation
                assertTrue(ad.getHeadline().length() <= 125); // Facebook headline limit
                assertTrue(ad.getPrimaryText().length() <= 2200); // Facebook primary text limit
            }
        });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_INTEGRATION_TEST", matches = "true")
    void testGeminiProviderIntegration() {
        AIProvider geminiProvider = aiProviders.get("gemini");
        assumeProviderAvailable(geminiProvider, "Gemini");

        String prompt = "Create an engaging ad for an e-commerce store";
        int variations = 1;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        assertDoesNotThrow(() -> {
            List<AdContent> content = geminiProvider.generateAdContent(prompt, variations, language, callToAction);

            assertNotNull(content);
            assertEquals(variations, content.size());

            AdContent ad = content.get(0);
            assertNotNull(ad.getHeadline());
            assertNotNull(ad.getPrimaryText());
            assertFalse(ad.getHeadline().trim().isEmpty());
            assertFalse(ad.getPrimaryText().trim().isEmpty());
        });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_INTEGRATION_TEST", matches = "true")
    void testAnthropicProviderIntegration() {
        AIProvider anthropicProvider = aiProviders.get("anthropic");
        assumeProviderAvailable(anthropicProvider, "Anthropic");

        String prompt = "Create professional ad copy for a B2B software solution";
        int variations = 1;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.LEARN_MORE;

        assertDoesNotThrow(() -> {
            List<AdContent> content = anthropicProvider.generateAdContent(prompt, variations, language, callToAction);

            assertNotNull(content);
            assertEquals(variations, content.size());

            AdContent ad = content.get(0);
            assertNotNull(ad.getHeadline());
            assertNotNull(ad.getPrimaryText());
            assertFalse(ad.getHeadline().trim().isEmpty());
            assertFalse(ad.getPrimaryText().trim().isEmpty());
        });
    }

    @Test
    void testFallbackServiceIntegration() {
        assumeServiceAvailable(fallbackService, "Fallback service");

        String prompt = "Create ad copy for a test product";
        int variations = 2;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.SHOP_NOW;

        assertDoesNotThrow(() -> {
            List<AdContent> content = fallbackService.generateWithFallback(prompt, variations, language, callToAction);

            assertNotNull(content);
            assertTrue(content.size() > 0);
            assertTrue(content.size() <= variations);

            // Should get either AI-generated content or fallback content
            AdContent ad = content.get(0);
            assertNotNull(ad.getHeadline());
            assertNotNull(ad.getPrimaryText());
            assertFalse(ad.getHeadline().trim().isEmpty());
            assertFalse(ad.getPrimaryText().trim().isEmpty());
        });
    }

    @Test
    void testProviderHealthCheck() {
        assumeServiceAvailable(fallbackService, "Fallback service");

        Map<String, AIProviderFallbackService.ProviderHealth> healthStatus =
            fallbackService.getProviderHealthStatus();

        assertNotNull(healthStatus);
        assertFalse(healthStatus.isEmpty());

        for (Map.Entry<String, AIProviderFallbackService.ProviderHealth> entry : healthStatus.entrySet()) {
            String providerName = entry.getKey();
            AIProviderFallbackService.ProviderHealth health = entry.getValue();

            assertNotNull(health);
            assertEquals(providerName, health.getProviderName());
            assertNotNull(health.getStatus());

            // Status should be one of the expected values
            assertTrue(health.getStatus().matches("AVAILABLE|UNAVAILABLE|CIRCUIT_OPEN"));

            System.out.println(String.format("Provider %s: %s (Available: %s, Circuit Open: %s)",
                providerName, health.getStatus(), health.isAvailable(), health.isCircuitOpen()));
        }
    }

    @Test
    void testDeadLetterQueueIntegration() {
        assumeServiceAvailable(deadLetterQueueService, "Dead letter queue service");

        // Test basic DLQ operations
        assertDoesNotThrow(() -> {
            DeadLetterQueueService.DLQStats stats = deadLetterQueueService.getDLQStats(24);
            assertNotNull(stats);
            assertTrue(stats.getTotalFailures() >= 0);

            long dlqSize = deadLetterQueueService.getDLQSize();
            assertTrue(dlqSize >= 0);
        });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_INTEGRATION_TEST", matches = "true")
    void testProviderErrorHandling() {
        // Test with invalid prompt to trigger error handling
        String invalidPrompt = ""; // Empty prompt should cause validation error
        int variations = 1;
        String language = "en";
        FacebookCTA callToAction = FacebookCTA.LEARN_MORE;

        for (Map.Entry<String, AIProvider> entry : aiProviders.entrySet()) {
            String providerName = entry.getKey();
            AIProvider provider = entry.getValue();

            try {
                List<AdContent> content = provider.generateAdContent(invalidPrompt, variations, language, callToAction);
                // If no exception is thrown, content should still be valid
                if (content != null && !content.isEmpty()) {
                    assertNotNull(content.get(0).getHeadline());
                }
            } catch (AIProviderException e) {
                // Expected for invalid prompts
                assertNotNull(e.getProviderName());
                assertNotNull(e.getErrorCode());
                System.out.println(String.format("Provider %s handled error correctly: %s",
                    providerName, e.getMessage()));
            } catch (Exception e) {
                fail(String.format("Provider %s should throw AIProviderException, but threw: %s",
                    providerName, e.getClass().getSimpleName()));
            }
        }
    }

    @Test
    void testProviderResponseValidation() {
        // Test that all providers handle various prompt types
        String[] testPrompts = {
            "Create a short ad for a mobile app",
            "Generate compelling copy for a luxury product",
            "Write an ad for a sustainable product targeting environmentally conscious consumers"
        };

        for (String prompt : testPrompts) {
            assertDoesNotThrow(() -> {
                if (fallbackService != null) {
                    List<AdContent> content = fallbackService.generateWithFallback(prompt, 1, "en", FacebookCTA.LEARN_MORE);

                    assertNotNull(content);
                    assertFalse(content.isEmpty());

                    AdContent ad = content.get(0);
                    assertNotNull(ad.getHeadline());
                    assertNotNull(ad.getPrimaryText());

                    // Validate content meets basic advertising requirements
                    assertTrue(ad.getHeadline().length() > 5, "Headline should be substantial");
                    assertTrue(ad.getPrimaryText().length() > 10, "Primary text should be substantial");

                    // Check for inappropriate content (basic validation)
                    assertFalse(ad.getHeadline().toLowerCase().contains("error"));
                    assertFalse(ad.getPrimaryText().toLowerCase().contains("failed"));
                }
            });
        }
    }

    private void assumeProviderAvailable(AIProvider provider, String providerName) {
        org.junit.jupiter.api.Assumptions.assumeTrue(provider != null,
            providerName + " provider is not available for integration testing");
    }

    private void assumeServiceAvailable(Object service, String serviceName) {
        org.junit.jupiter.api.Assumptions.assumeTrue(service != null,
            serviceName + " is not available for integration testing");
    }
}