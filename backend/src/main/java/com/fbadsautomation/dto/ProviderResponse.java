package com.fbadsautomation.dto;

import java.util.List;

public class ProviderResponse {
    private String id;
    private String name;
    private String description;
    private List<String> capabilities; // TEXT, IMAGE, VIDEO
    private boolean enabled;
    
    // Constructors
    public ProviderResponse() {
    }
    
    public ProviderResponse(String id, String name, String description, List<String> capabilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capabilities = capabilities;
        this.enabled = true; // Mặc định là enabled
    }
    
    public ProviderResponse(String id, String name, String description, List<String> capabilities, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capabilities = capabilities;
        this.enabled = enabled;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getCapabilities() {
        return capabilities;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // Setters
    public void setId(String id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // Builder pattern
    public static ProviderResponseBuilder builder() {
        return new ProviderResponseBuilder();
    }
    
    public static class ProviderResponseBuilder {
        private String id;
        private String name;
        private String description;
        private List<String> capabilities;
        private boolean enabled;
        
        public ProviderResponseBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public ProviderResponseBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public ProviderResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public ProviderResponseBuilder capabilities(List<String> capabilities) {
            this.capabilities = capabilities;
            return this;
        }
        
        public ProviderResponseBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public ProviderResponse build() {
            return new ProviderResponse(id, name, description, capabilities, enabled);
        }
    }
}
