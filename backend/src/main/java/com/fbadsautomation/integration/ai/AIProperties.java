package com.fbadsautomation.integration.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data

public class AIProperties {
    
    @Value("${ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("gpt-3.5-turbo")
    private String openaiModel;
    
    @Value("${ai.gemini.api-key}")
    private String geminiApiKey;
    
    @Value("gemini-1.5-pro")
    private String geminiModel;
    // @Value("${ai.provider}")
    // private String provider;
    }
