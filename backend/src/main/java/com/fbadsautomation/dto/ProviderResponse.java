package com.fbadsautomation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {
    private String id;
    private String name;
    private String description;
    private List<String> capabilities; // TEXT, IMAGE, VIDEO
    private boolean enabled;
    
    public ProviderResponse(String id, String name, String description, List<String> capabilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capabilities = capabilities;
        this.enabled = true; // Mặc định là enabled
    }
}

