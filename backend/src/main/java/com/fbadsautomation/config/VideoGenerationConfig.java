package com.fbadsautomation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for video generation components
 */
@Configuration
@EnableScheduling
public class VideoGenerationConfig {
    
    // RestTemplate bean removed to avoid conflict with WebConfig
    // The RestTemplate bean in WebConfig will be used instead
}
