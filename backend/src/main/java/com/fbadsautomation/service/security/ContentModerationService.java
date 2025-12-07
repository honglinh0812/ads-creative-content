package com.fbadsautomation.service.security;

import com.fbadsautomation.model.AdContent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);
    private static final Pattern[] DISALLOWED_PATTERNS = new Pattern[]{
        Pattern.compile("(?i)password"),
        Pattern.compile("(?i)api[_-]?key"),
        Pattern.compile("(?i)drop\\s+table"),
        Pattern.compile("(?i)execute\\s+script"),
        Pattern.compile("(?i)<script"),
        Pattern.compile("(?i)system\\s+override"),
        Pattern.compile("(?i)prompt\\s+injection"),
        Pattern.compile("(?i)ignore\\s+all\\s+previous")
    };

    private final PromptSecurityService promptSecurityService;

    public ContentModerationService(PromptSecurityService promptSecurityService) {
        this.promptSecurityService = promptSecurityService;
    }

    public void enforceSafety(List<AdContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (AdContent content : contents) {
            if (content == null) {
                continue;
            }
            List<String> warnings = new ArrayList<>();
            content.setHeadline(cleanAndInspect(content.getHeadline(), warnings));
            content.setPrimaryText(cleanAndInspect(content.getPrimaryText(), warnings));
            content.setDescription(cleanAndInspect(content.getDescription(), warnings));
            if (!warnings.isEmpty()) {
                content.setHasWarnings(true);
                content.setValidationWarnings(String.join("; ", warnings));
            }
        }
    }

    private String cleanAndInspect(String value, List<String> warnings) {
        if (value == null || value.isBlank()) {
            return value;
        }
        for (Pattern pattern : DISALLOWED_PATTERNS) {
            if (pattern.matcher(value).find()) {
                warnings.add("Potential unsafe content: " + pattern.pattern());
                log.warn("Moderation flagged generated content: pattern={} text={}", pattern.pattern(), value);
                value = pattern.matcher(value).replaceAll("[filtered]");
            }
        }
        return promptSecurityService.sanitizeModelOutput(value);
    }
}
