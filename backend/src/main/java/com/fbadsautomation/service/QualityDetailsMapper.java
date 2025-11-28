package com.fbadsautomation.service;

import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.model.AdContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QualityDetailsMapper {

    private final AdQualityScoringService adQualityScoringService;

    public AdGenerationResponse.QualityDetails buildDetails(AdContent content) {
        AdQualityScoringService.AdQualityScore score = adQualityScoringService.calculateQualityScore(content);
        return mapScore(score);
    }

    public AdGenerationResponse.QualityDetails mapScore(AdQualityScoringService.AdQualityScore score) {
        if (score == null) {
            return null;
        }
        AdGenerationResponse.QualityDetails details = new AdGenerationResponse.QualityDetails();
        details.setComplianceScore(score.getComplianceScore());
        details.setLinguisticScore(score.getLinguisticScore());
        details.setPersuasivenessScore(score.getPersuasivenessScore());
        details.setCompletenessScore(score.getCompletenessScore());
        details.setTotalScore(score.getTotalScore());
        details.setGrade(score.getGrade());
        details.setSuggestions(score.getSuggestions());
        details.setStrengths(score.getStrengths());
        return details;
    }
}
