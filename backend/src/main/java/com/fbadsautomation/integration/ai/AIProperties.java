package com.fbadsautomation.integration.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AIProperties {
    
    @Value("${ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("gpt-3.5-turbo")
    private String openaiModel;
    
    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;
    
    @Value("gemini-1.5-pro")
    private String geminiModel;
    
    // Getter and setter methods
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    
    public String getOpenaiModel() { return openaiModel; }
    public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }
    
    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }
    
    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }
    
    // @Value("${ai.provider}")
    // private String provider;
}
