package com.fbadsautomation.model;

public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    ALL("All");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
