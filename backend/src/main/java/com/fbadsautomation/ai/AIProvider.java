package com.fbadsautomation.ai;

import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.model.FacebookCTA;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface AIProvider {
    String getName();
    String getProviderName();
    String getApiUrl();
    Set<Capability> getCapabilities();

    List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction);
    CompletableFuture<List<AdContent>> generateAdContentAsync(String prompt, int numberOfVariations, String language, FacebookCTA callToAction);

    String generateImage(String prompt);
    CompletableFuture<String> generateImageAsync(String prompt);

    boolean supportsImageGeneration();

    String enhanceImage(String imagePath, String enhancementType, java.util.Map<String, Object> params) throws Exception;
    CompletableFuture<String> enhanceImageAsync(String imagePath, String enhancementType, java.util.Map<String, Object> params);
}
