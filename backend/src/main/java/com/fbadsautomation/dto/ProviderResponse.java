package com.fbadsautomation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

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
