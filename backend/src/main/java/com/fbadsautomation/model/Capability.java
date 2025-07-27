package com.fbadsautomation.model;

/**
 * Enum representing AI provider capabilities
 */
public enum Capability {
    TEXT_GENERATION("Text Generation", "Generate text content like headlines and descriptions"),
    IMAGE_GENERATION("Image Generation", "Generate images and visual content"),
    CONTENT_OPTIMIZATION("Content Optimization", "Optimize content for better performance"),
    MULTI_LANGUAGE("Multi-Language", "Support for multiple languages"),
    BATCH_PROCESSING("Batch Processing", "Process multiple requests simultaneously"),
    REAL_TIME("Real-Time", "Real-time content generation"),
    CUSTOM_PROMPTS("Custom Prompts", "Support for custom prompt templates"),
    ANALYTICS_INTEGRATION("Analytics Integration", "Integration with analytics platforms");

    private final String displayName;
    private final String description;

    Capability(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
