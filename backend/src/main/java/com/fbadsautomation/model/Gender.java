package com.fbadsautomation.model;

public enum Gender {
    MALE("Male", "Nam"),
    FEMALE("Female", "Nữ"),
    ALL("All", "Tất cả");

    private final String displayName;
    private final String displayNameVietnamese;

    Gender(String displayName, String displayNameVietnamese) {
        this.displayName = displayName;
        this.displayNameVietnamese = displayNameVietnamese;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Phase 3: Added for ChainOfThoughtPromptBuilder bilingual support
     */
    public String getDisplayNameVietnamese() {
        return displayNameVietnamese;
    }
}
