package com.fbadsautomation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for content validation
 */
@Configuration
@ConfigurationProperties(prefix = "validation.content")
public class ValidationConfig {

    private String strictness = "medium";
    private int minValidVariations = 1;
    private boolean allowPartialResults = true;
    private int maxAutoFixAttempts = 3;
    private int minQualityScore = 40;

    // Getters and setters
    public String getStrictness() {
        return strictness;
    }

    public void setStrictness(String strictness) {
        this.strictness = strictness;
    }

    public int getMinValidVariations() {
        return minValidVariations;
    }

    public void setMinValidVariations(int minValidVariations) {
        this.minValidVariations = minValidVariations;
    }

    public boolean isAllowPartialResults() {
        return allowPartialResults;
    }

    public void setAllowPartialResults(boolean allowPartialResults) {
        this.allowPartialResults = allowPartialResults;
    }

    public int getMaxAutoFixAttempts() {
        return maxAutoFixAttempts;
    }

    public void setMaxAutoFixAttempts(int maxAutoFixAttempts) {
        this.maxAutoFixAttempts = maxAutoFixAttempts;
    }

    public int getMinQualityScore() {
        return minQualityScore;
    }

    public void setMinQualityScore(int minQualityScore) {
        this.minQualityScore = minQualityScore;
    }

    /**
     * Check if validation is configured as strict mode
     */
    public boolean isStrictMode() {
        return "high".equalsIgnoreCase(strictness);
    }

    /**
     * Check if validation is configured as lenient mode
     */
    public boolean isLenientMode() {
        return "low".equalsIgnoreCase(strictness);
    }
}
