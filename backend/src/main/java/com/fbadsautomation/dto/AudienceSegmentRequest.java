package com.fbadsautomation.dto;

import com.fbadsautomation.model.Gender;
import javax.validation.constraints.*;

public class AudienceSegmentRequest {

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Min(value = 13, message = "Minimum age must be at least 13 (Facebook policy)")
    @Max(value = 65, message = "Maximum age cannot exceed 65")
    private Integer minAge;

    @Min(value = 13, message = "Minimum age must be at least 13 (Facebook policy)")
    @Max(value = 65, message = "Maximum age cannot exceed 65")
    private Integer maxAge;

    @Size(max = 500, message = "Location cannot exceed 500 characters")
    private String location;

    @Size(max = 1000, message = "Interests cannot exceed 1000 characters")
    private String interests;

    // Constructors
    public AudienceSegmentRequest() {}

    public AudienceSegmentRequest(Gender gender, Integer minAge, Integer maxAge, String location, String interests) {
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.location = location;
        this.interests = interests;
    }

    // Custom validation
    @AssertTrue(message = "Max age must be greater than or equal to min age")
    public boolean isAgeRangeValid() {
        if (minAge != null && maxAge != null) {
            return maxAge >= minAge;
        }
        return true;
    }

    // Getters and Setters
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
}
