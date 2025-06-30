package com.fbadsautomation.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // Prevent sending null fields in JSON
public class AdRequest {
    private String adType;
    private String prompt;
    private String textProvider; // Renamed from aiProvider for clarity
    private String imageProvider;
    private String videoProvider;
    private Boolean generateVideo; // Use Boolean to handle null if not sent
    private Integer videoDuration; // Use Integer to handle null if not sent
    private int numberOfVariations;
    private String language;

    // Constructors
    public AdRequest() {
        this.numberOfVariations = 1; // Default value
    }

    // Getters and Setters
    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    // Renamed getter/setter
    public String getTextProvider() {
        return textProvider;
    }

    public void setTextProvider(String textProvider) {
        this.textProvider = textProvider;
    }

    public String getImageProvider() {
        return imageProvider;
    }

    public void setImageProvider(String imageProvider) {
        this.imageProvider = imageProvider;
    }
    
    public String getVideoProvider() {
        return videoProvider;
    }

    public void setVideoProvider(String videoProvider) {
        this.videoProvider = videoProvider;
    }

    public Boolean getGenerateVideo() {
        return generateVideo;
    }

    public void setGenerateVideo(Boolean generateVideo) {
        this.generateVideo = generateVideo;
    }

    public Integer getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Integer videoDuration) {
        this.videoDuration = videoDuration;
    }

    public int getNumberOfVariations() {
        return numberOfVariations;
    }

    public void setNumberOfVariations(int numberOfVariations) {
        this.numberOfVariations = numberOfVariations;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}

