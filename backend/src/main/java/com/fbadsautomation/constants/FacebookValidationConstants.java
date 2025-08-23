package com.fbadsautomation.constants;

import java.util.Arrays;
import java.util.List;

public final class FacebookValidationConstants {
    
    // Character limits (Facebook official requirements)
    public static final int MAX_HEADLINE_LENGTH = 40;
    public static final int MAX_DESCRIPTION_LENGTH = 30;
    public static final int MAX_PRIMARY_TEXT_LENGTH = 125; // Corrected from 10000/12500
    public static final int MIN_WORDS_PER_FIELD = 3;
    
    // Prohibited words (case-insensitive)
    public static final List<String> PROHIBITED_WORDS = Arrays.asList(
        "hate", "violence", "drugs", "miracle", "guaranteed", "cure", "instant",
        "free!!!", "act now!!!", "$$$", "???", "amazing!!!", "incredible!!!"
    );
    
    // Prohibited patterns
    public static final String EXCESSIVE_CAPS_PATTERN = "[A-Z]{4,}";
    public static final String EXCESSIVE_PUNCTUATION_PATTERN = "[!?$]{3,}";
    
    private FacebookValidationConstants() {
        // Utility class
    }
}