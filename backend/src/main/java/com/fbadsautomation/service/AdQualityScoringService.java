package com.fbadsautomation.service;

import com.fbadsautomation.model.AdContent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to calculate quality scores for ad content.
 * This provides automated quality assessment across multiple dimensions.
 *
 * Quality Score (0-100):
 * - Compliance Score: 30 points (character limits, prohibited words)
 * - Linguistic Quality: 30 points (readability, keyword density, grammar)
 * - Persuasiveness: 20 points (power words, CTA presence, value proposition)
 * - Completeness: 20 points (all required fields present)
 */
@Slf4j
@Service
public class AdQualityScoringService {

    // Facebook character limits
    private static final int HEADLINE_MAX_LENGTH = 40;
    private static final int DESCRIPTION_MAX_LENGTH = 125;
    private static final int PRIMARY_TEXT_MAX_LENGTH = 1000;

    // Prohibited words (Facebook advertising policies)
    private static final Set<String> PROHIBITED_WORDS = Set.of(
        "free", "click here", "buy now", "limited time",
        "guaranteed", "miracle", "breakthrough", "secret",
        "shocking", "weird trick", "you won't believe"
        // Add more as needed
    );

    // Power words that increase persuasiveness
    private static final Set<String> POWER_WORDS = Set.of(
        "new", "exclusive", "special", "premium", "quality",
        "professional", "certified", "trusted", "proven",
        "save", "discount", "offer", "deal", "value",
        "easy", "simple", "quick", "fast", "instant",
        "best", "top", "leading", "award-winning"
    );

    /**
     * Calculate comprehensive quality score for an ad.
     */
    public AdQualityScore calculateQualityScore(AdContent adContent) {
        log.debug("Calculating quality score for ad content: {}", adContent.getId());

        AdQualityScore score = new AdQualityScore();
        score.setAdContentId(adContent.getId());

        // Calculate each dimension
        score.setComplianceScore(calculateComplianceScore(adContent));
        score.setLinguisticScore(calculateLinguisticScore(adContent));
        score.setPersuasivenessScore(calculatePersuasivenessScore(adContent));
        score.setCompletenessScore(calculateCompletenessScore(adContent));

        // Calculate total score
        double totalScore = score.getComplianceScore() +
                          score.getLinguisticScore() +
                          score.getPersuasivenessScore() +
                          score.getCompletenessScore();

        score.setTotalScore(totalScore);
        score.setGrade(determineGrade(totalScore));
        score.setSuggestions(generateSuggestions(adContent, score));

        log.info("Quality score calculated: {} (Grade: {})", totalScore, score.getGrade());

        return score;
    }

    /**
     * Calculate compliance score (0-30 points).
     */
    private double calculateComplianceScore(AdContent adContent) {
        double score = 0;

        String headline = adContent.getHeadline() != null ? adContent.getHeadline() : "";
        String description = adContent.getDescription() != null ? adContent.getDescription() : "";
        String primaryText = adContent.getPrimaryText() != null ? adContent.getPrimaryText() : "";

        // Character limit compliance (15 points total)
        if (headline.length() <= HEADLINE_MAX_LENGTH) {
            score += 5;
        } else {
            // Partial score based on how close
            double excess = (double)(headline.length() - HEADLINE_MAX_LENGTH) / HEADLINE_MAX_LENGTH;
            score += Math.max(0, 5 - (excess * 5));
        }

        if (description.length() <= DESCRIPTION_MAX_LENGTH) {
            score += 5;
        } else {
            double excess = (double)(description.length() - DESCRIPTION_MAX_LENGTH) / DESCRIPTION_MAX_LENGTH;
            score += Math.max(0, 5 - (excess * 5));
        }

        if (primaryText.length() <= PRIMARY_TEXT_MAX_LENGTH) {
            score += 5;
        } else {
            double excess = (double)(primaryText.length() - PRIMARY_TEXT_MAX_LENGTH) / PRIMARY_TEXT_MAX_LENGTH;
            score += Math.max(0, 5 - (excess * 5));
        }

        // Prohibited words check (15 points)
        String allText = (headline + " " + description + " " + primaryText).toLowerCase();
        int prohibitedCount = 0;
        for (String word : PROHIBITED_WORDS) {
            if (allText.contains(word)) {
                prohibitedCount++;
            }
        }

        if (prohibitedCount == 0) {
            score += 15;
        } else {
            // Deduct 3 points per prohibited word, minimum 0
            score += Math.max(0, 15 - (prohibitedCount * 3));
        }

        return Math.round(score * 10.0) / 10.0; // Round to 1 decimal
    }

    /**
     * Calculate linguistic quality score (0-30 points).
     */
    private double calculateLinguisticScore(AdContent adContent) {
        double score = 0;

        String headline = adContent.getHeadline() != null ? adContent.getHeadline() : "";
        String description = adContent.getDescription() != null ? adContent.getDescription() : "";
        String primaryText = adContent.getPrimaryText() != null ? adContent.getPrimaryText() : "";

        // Keyword density (10 points)
        score += assessKeywordDensity(primaryText);

        // Readability (10 points)
        score += assessReadability(primaryText);

        // Basic grammar check (10 points)
        score += assessGrammar(headline, description, primaryText);

        return Math.round(score * 10.0) / 10.0;
    }

    /**
     * Calculate persuasiveness score (0-20 points).
     */
    private double calculatePersuasivenessScore(AdContent adContent) {
        double score = 0;

        String headline = adContent.getHeadline() != null ? adContent.getHeadline() : "";
        String description = adContent.getDescription() != null ? adContent.getDescription() : "";
        String primaryText = adContent.getPrimaryText() != null ? adContent.getPrimaryText() : "";

        // Call-to-action present (10 points)
        if (adContent.getCallToAction() != null) {
            score += 10;
        }

        // Power words usage (10 points)
        String allText = (headline + " " + description + " " + primaryText).toLowerCase();
        int powerWordCount = 0;
        for (String word : POWER_WORDS) {
            Pattern pattern = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(allText);
            if (matcher.find()) {
                powerWordCount++;
            }
        }

        // Award up to 10 points based on power word usage
        score += Math.min(10, powerWordCount * 1.5);

        return Math.round(score * 10.0) / 10.0;
    }

    /**
     * Calculate completeness score (0-20 points).
     */
    private double calculateCompletenessScore(AdContent adContent) {
        double score = 0;

        String headline = adContent.getHeadline();
        String description = adContent.getDescription();
        String primaryText = adContent.getPrimaryText();

        // All required fields present and non-empty (20 points total)
        if (headline != null && !headline.trim().isEmpty() && headline.length() >= 10) {
            score += 7;
        } else if (headline != null && !headline.trim().isEmpty()) {
            score += 3; // Partial credit for short headline
        }

        if (description != null && !description.trim().isEmpty() && description.length() >= 20) {
            score += 7;
        } else if (description != null && !description.trim().isEmpty()) {
            score += 3; // Partial credit for short description
        }

        if (primaryText != null && !primaryText.trim().isEmpty() && primaryText.length() >= 50) {
            score += 6;
        } else if (primaryText != null && !primaryText.trim().isEmpty()) {
            score += 2; // Partial credit for short primary text
        }

        return Math.round(score * 10.0) / 10.0;
    }

    /**
     * Assess keyword density (0-10 points).
     * Good density: 1-3% of text should be relevant keywords.
     */
    private double assessKeywordDensity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // Simple heuristic: count words longer than 5 characters
        String[] words = text.toLowerCase().split("\\s+");
        int totalWords = words.length;
        int keywordCount = 0;

        for (String word : words) {
            if (word.length() > 5) {
                keywordCount++;
            }
        }

        if (totalWords == 0) return 0;

        double density = (double) keywordCount / totalWords;

        // Optimal density: 15-35%
        if (density >= 0.15 && density <= 0.35) {
            return 10;
        } else if (density >= 0.10 && density <= 0.40) {
            return 7;
        } else if (density >= 0.05 && density <= 0.45) {
            return 5;
        } else {
            return 3;
        }
    }

    /**
     * Assess readability (0-10 points).
     * Based on average sentence length and word length.
     */
    private double assessReadability(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // Count sentences
        String[] sentences = text.split("[.!?]+");
        int sentenceCount = sentences.length;

        // Count words
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        if (sentenceCount == 0 || wordCount == 0) {
            return 5; // Neutral score for very short text
        }

        // Average words per sentence
        double avgWordsPerSentence = (double) wordCount / sentenceCount;

        // Average characters per word
        int totalChars = 0;
        for (String word : words) {
            totalChars += word.length();
        }
        double avgCharsPerWord = (double) totalChars / wordCount;

        // Optimal: 10-20 words per sentence, 4-6 chars per word
        double sentenceScore = 0;
        if (avgWordsPerSentence >= 10 && avgWordsPerSentence <= 20) {
            sentenceScore = 5;
        } else if (avgWordsPerSentence >= 5 && avgWordsPerSentence <= 25) {
            sentenceScore = 3;
        } else {
            sentenceScore = 2;
        }

        double wordScore = 0;
        if (avgCharsPerWord >= 4 && avgCharsPerWord <= 6) {
            wordScore = 5;
        } else if (avgCharsPerWord >= 3 && avgCharsPerWord <= 7) {
            wordScore = 3;
        } else {
            wordScore = 2;
        }

        return sentenceScore + wordScore;
    }

    /**
     * Assess basic grammar (0-10 points).
     * Simple heuristics for common issues.
     */
    private double assessGrammar(String headline, String description, String primaryText) {
        double score = 10; // Start with perfect score, deduct for issues

        String allText = headline + " " + description + " " + primaryText;

        // Check for double spaces
        if (allText.contains("  ")) {
            score -= 1;
        }

        // Check for proper capitalization at start
        if (headline != null && !headline.isEmpty() && !Character.isUpperCase(headline.charAt(0))) {
            score -= 2;
        }

        // Check for ending punctuation in primary text
        if (primaryText != null && !primaryText.isEmpty()) {
            char lastChar = primaryText.charAt(primaryText.length() - 1);
            if (lastChar != '.' && lastChar != '!' && lastChar != '?') {
                score -= 1;
            }
        }

        // Check for excessive punctuation
        if (allText.matches(".*[!?]{2,}.*")) {
            score -= 2;
        }

        // Check for all caps (spammy)
        if (headline != null && headline.equals(headline.toUpperCase()) && headline.length() > 5) {
            score -= 2;
        }

        return Math.max(0, score);
    }

    /**
     * Determine letter grade based on total score.
     */
    private String determineGrade(double totalScore) {
        if (totalScore >= 90) return "A+ (Excellent)";
        if (totalScore >= 85) return "A (Excellent)";
        if (totalScore >= 80) return "A- (Very Good)";
        if (totalScore >= 75) return "B+ (Good)";
        if (totalScore >= 70) return "B (Good)";
        if (totalScore >= 65) return "B- (Above Average)";
        if (totalScore >= 60) return "C+ (Average)";
        if (totalScore >= 55) return "C (Average)";
        if (totalScore >= 50) return "C- (Below Average)";
        if (totalScore >= 45) return "D (Poor)";
        return "F (Needs Improvement)";
    }

    /**
     * Generate improvement suggestions based on score breakdown.
     */
    private List<String> generateSuggestions(AdContent adContent, AdQualityScore score) {
        List<String> suggestions = new ArrayList<>();

        // Compliance suggestions
        if (score.getComplianceScore() < 25) {
            String headline = adContent.getHeadline() != null ? adContent.getHeadline() : "";
            String description = adContent.getDescription() != null ? adContent.getDescription() : "";
            String primaryText = adContent.getPrimaryText() != null ? adContent.getPrimaryText() : "";

            if (headline.length() > HEADLINE_MAX_LENGTH) {
                suggestions.add(String.format("Headline is too long (%d chars, max %d). Consider shortening.",
                    headline.length(), HEADLINE_MAX_LENGTH));
            }

            if (description.length() > DESCRIPTION_MAX_LENGTH) {
                suggestions.add(String.format("Description is too long (%d chars, max %d). Consider shortening.",
                    description.length(), DESCRIPTION_MAX_LENGTH));
            }

            if (primaryText.length() > PRIMARY_TEXT_MAX_LENGTH) {
                suggestions.add(String.format("Primary text is too long (%d chars, max %d). Consider shortening.",
                    primaryText.length(), PRIMARY_TEXT_MAX_LENGTH));
            }

            // Check for prohibited words
            String allText = (headline + " " + description + " " + primaryText).toLowerCase();
            for (String word : PROHIBITED_WORDS) {
                if (allText.contains(word)) {
                    suggestions.add(String.format("Avoid using prohibited word: '%s'", word));
                }
            }
        }

        // Linguistic suggestions
        if (score.getLinguisticScore() < 20) {
            suggestions.add("Improve readability by using shorter sentences and common words.");
        }

        // Persuasiveness suggestions
        if (score.getPersuasivenessScore() < 15) {
            if (adContent.getCallToAction() == null) {
                suggestions.add("Add a clear call-to-action (CTA) to improve persuasiveness.");
            }
            suggestions.add("Use more power words like 'exclusive', 'premium', 'special', 'proven'.");
        }

        // Completeness suggestions
        if (score.getCompletenessScore() < 15) {
            if (adContent.getHeadline() == null || adContent.getHeadline().isEmpty()) {
                suggestions.add("Add a headline to complete the ad.");
            }
            if (adContent.getDescription() == null || adContent.getDescription().isEmpty()) {
                suggestions.add("Add a description to complete the ad.");
            }
            if (adContent.getPrimaryText() == null || adContent.getPrimaryText().isEmpty()) {
                suggestions.add("Add primary text to complete the ad.");
            }
        }

        return suggestions;
    }

    /**
     * Data class for quality score result.
     */
    @Data
    public static class AdQualityScore {
        private Long adContentId;
        private double complianceScore;      // 0-30
        private double linguisticScore;      // 0-30
        private double persuasivenessScore;  // 0-20
        private double completenessScore;    // 0-20
        private double totalScore;           // 0-100
        private String grade;                // A+, A, B+, etc.
        private List<String> suggestions;    // Improvement suggestions

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("adContentId", adContentId);
            map.put("complianceScore", complianceScore);
            map.put("linguisticScore", linguisticScore);
            map.put("persuasivenessScore", persuasivenessScore);
            map.put("completenessScore", completenessScore);
            map.put("totalScore", totalScore);
            map.put("grade", grade);
            map.put("suggestions", suggestions);
            return map;
        }
    }
}
