package com.fbadsautomation.controller;

import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.repository.AdContentRepository;
import com.fbadsautomation.service.AdQualityScoringService;
import com.fbadsautomation.service.AdQualityScoringService.AdQualityScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityScoringController {

    private final AdQualityScoringService qualityScoringService;
    private final AdContentRepository adContentRepository;

    /**
     * Calculate quality score for a single ad content.
     */
    @GetMapping("/score/{adContentId}")
    public ResponseEntity<Map<String, Object>> getQualityScore(@PathVariable Long adContentId) {
        log.info("Calculating quality score for ad content: {}", adContentId);

        Optional<AdContent> adContentOpt = adContentRepository.findById(adContentId);
        if (adContentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AdContent adContent = adContentOpt.get();
        AdQualityScore score = qualityScoringService.calculateQualityScore(adContent);

        return ResponseEntity.ok(score.toMap());
    }

    /**
     * Calculate quality scores for multiple ad contents in batch.
     */
    @PostMapping("/score/batch")
    public ResponseEntity<List<Map<String, Object>>> getQualityScoreBatch(
            @RequestBody List<Long> adContentIds) {
        log.info("Calculating quality scores for {} ad contents", adContentIds.size());

        List<Map<String, Object>> scores = new ArrayList<>();

        for (Long id : adContentIds) {
            Optional<AdContent> adContentOpt = adContentRepository.findById(id);
            if (adContentOpt.isPresent()) {
                AdQualityScore score = qualityScoringService.calculateQualityScore(adContentOpt.get());
                scores.add(score.toMap());
            }
        }

        return ResponseEntity.ok(scores);
    }

    /**
     * Get quality score statistics for an ad (all its content variations).
     */
    @GetMapping("/stats/ad/{adId}")
    public ResponseEntity<Map<String, Object>> getAdQualityStats(@PathVariable Long adId) {
        log.info("Calculating quality statistics for ad: {}", adId);

        List<AdContent> adContents = adContentRepository.findByAdId(adId);

        if (adContents.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<AdQualityScore> scores = adContents.stream()
                .map(qualityScoringService::calculateQualityScore)
                .toList();

        // Calculate statistics
        DoubleSummaryStatistics totalStats = scores.stream()
                .mapToDouble(AdQualityScore::getTotalScore)
                .summaryStatistics();

        DoubleSummaryStatistics complianceStats = scores.stream()
                .mapToDouble(AdQualityScore::getComplianceScore)
                .summaryStatistics();

        DoubleSummaryStatistics linguisticStats = scores.stream()
                .mapToDouble(AdQualityScore::getLinguisticScore)
                .summaryStatistics();

        DoubleSummaryStatistics persuasivenessStats = scores.stream()
                .mapToDouble(AdQualityScore::getPersuasivenessScore)
                .summaryStatistics();

        DoubleSummaryStatistics completenessStats = scores.stream()
                .mapToDouble(AdQualityScore::getCompletenessScore)
                .summaryStatistics();

        Map<String, Object> stats = new HashMap<>();
        stats.put("count", scores.size());
        stats.put("totalScore", Map.of(
                "average", totalStats.getAverage(),
                "min", totalStats.getMin(),
                "max", totalStats.getMax()
        ));
        stats.put("complianceScore", Map.of(
                "average", complianceStats.getAverage(),
                "min", complianceStats.getMin(),
                "max", complianceStats.getMax()
        ));
        stats.put("linguisticScore", Map.of(
                "average", linguisticStats.getAverage(),
                "min", linguisticStats.getMin(),
                "max", linguisticStats.getMax()
        ));
        stats.put("persuasivenessScore", Map.of(
                "average", persuasivenessStats.getAverage(),
                "min", persuasivenessStats.getMin(),
                "max", persuasivenessStats.getMax()
        ));
        stats.put("completenessScore", Map.of(
                "average", completenessStats.getAverage(),
                "min", completenessStats.getMin(),
                "max", completenessStats.getMax()
        ));

        // Include all individual scores
        stats.put("scores", scores.stream().map(AdQualityScore::toMap).toList());

        return ResponseEntity.ok(stats);
    }
}
