package com.fbadsautomation.service;

import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.dto.ProviderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProviderService {

    private final List<AIProvider> aiProviders;
    private final Map<String, AIProvider> providerMap = new HashMap<>();

    @PostConstruct
    public void initProviderMap() {
        for (AIProvider provider : aiProviders) {
            String key = normalizeKey(provider.getProviderName());
            providerMap.put(key, provider);
        }
        log.info("✅ Loaded {} AI providers: {}", providerMap.size(), providerMap.keySet());
    }

    public List<ProviderResponse> getTextProviders() {
        return aiProviders.stream()
                .filter(provider -> provider.getCapabilities().contains("TEXT"))
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public List<ProviderResponse> getImageProviders() {
        return aiProviders.stream()
                .filter(provider -> provider.getCapabilities().contains("IMAGE"))
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public List<ProviderResponse> getAllProviders() {
        return aiProviders.stream()
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public AIProvider getProvider(String providerId) {
        if (providerId == null) return null;
        return providerMap.get(normalizeKey(providerId));
    }

    private ProviderResponse toProviderResponse(AIProvider provider) {
        String id = normalizeKey(provider.getProviderName());
        return new ProviderResponse(
                id,
                provider.getProviderName(),
                "Tạo nội dung bằng " + provider.getProviderName(),
                provider.getCapabilities()
        );
    }

    private String normalizeKey(String input) {
        return input.trim().toLowerCase().replace(" ", "_");
    }
}
