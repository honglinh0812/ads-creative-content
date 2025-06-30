package com.fbadsautomation.integration.ai;

import com.fbadsautomation.model.AdContent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface AIContentGenerator {
    /**
     * Generates ad content based on the provided prompt
     * @param prompt The prompt to generate content from
     * @param contentType The type of content to generate
     * @return The generated ad content
     */
    AdContent generateAdContent(String prompt, AdContent.ContentType contentType);
    
    /**
     * Generates ad content based on the provided prompt and returns raw content
     * @param prompt The prompt to generate content from
     * @param contentType The type of content to generate
     * @return List of maps containing the generated content fields
     */
    default List<Map<String, String>> generateAdContent(String prompt) {
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    /**
     * Gets the name of the AI provider
     * @return The name of the AI provider
     */
    String getProviderName();
}
