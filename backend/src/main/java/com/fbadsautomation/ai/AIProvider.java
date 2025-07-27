package com.fbadsautomation.ai;

import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.Capability;
import com.fbadsautomation.model.FacebookCTA;
import java.util.List;
import java.util.Set;

public interface AIProvider {
    String getName();
    String getProviderName();
    String getApiUrl();
    Set<Capability> getCapabilities();
    List<AdContent> generateAdContent(String prompt, int numberOfVariations, String language, FacebookCTA callToAction);
    String generateImage(String prompt);
    boolean supportsImageGeneration();
}
