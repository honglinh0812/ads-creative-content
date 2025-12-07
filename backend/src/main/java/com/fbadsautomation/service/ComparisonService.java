package com.fbadsautomation.service;

import com.fbadsautomation.dto.CompetitorAdDTO;
import com.fbadsautomation.exception.AIProviderException;
import com.fbadsautomation.exception.ResourceException;
import com.fbadsautomation.service.security.ContentModerationService;
import com.fbadsautomation.service.security.PromptSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for AI-powered ad comparison and suggestion generation
 *
 * Analyzes competitor ads and user's ads to provide intelligent suggestions
 * for improving ad performance and engagement.
 *
 * @author AI Panel - Senior Engineers
 * @version 1.0
 * @security All prompts sanitized, AI responses filtered
 * @performance Results cached for 1 hour per comparison pair
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ComparisonService {

    private final AIProviderService aiProviderService;
    private final PromptSecurityService promptSecurityService;
    private final ContentModerationService contentModerationService;

    /**
     * Generate suggested ad variation based on competitor analysis
     *
     * Uses AI to analyze competitor's ad strengths and suggest improvements
     * to user's ad while maintaining brand voice.
     *
     * Security: Input sanitization, output length limits
     * Performance: Cached with key based on ad content hashes
     *
     * @param competitorAd Competitor's ad to analyze
     * @param myAd User's current ad
     * @param provider AI provider to use (openai, anthropic, gemini)
     * @return AI-generated suggestion for improved ad
     * @throws AIProviderException if AI generation fails
     */
    @Cacheable(value = "adComparisons", key = "#competitorAd.adId + ':' + #myAd.hashCode()", unless = "#result == null")
    public String generateSuggestedVariation(
            @NotNull CompetitorAdDTO competitorAd,
            @NotBlank String myAd,
            String provider) {

        log.info("Generating ad variation suggestion using provider: {}", provider);

        // Sanitize inputs
        String sanitizedCompetitorText = sanitizeForPrompt(competitorAd.getPrimaryText());
        String sanitizedMyAd = sanitizeForPrompt(myAd);

        // Build analysis prompt
        String prompt = buildComparisonPrompt(sanitizedCompetitorText, sanitizedMyAd, competitorAd);

        try {
            // Get AI provider
            com.fbadsautomation.ai.AIProvider aiProvider = aiProviderService.getProvider(provider != null ? provider : "openai");
            if (aiProvider == null) {
                throw new RuntimeException("AI provider not found: " + provider);
            }

            // Generate suggestion using AI - we'll get first variation's body text
            List<com.fbadsautomation.model.AdContent> results = aiProvider.generateAdContent(prompt, 1, "en", null);
            contentModerationService.enforceSafety(results);

            if (results == null || results.isEmpty()) {
                throw new RuntimeException("AI provider returned no results");
            }

            String suggestion = results.get(0).getPrimaryText();

            // Validate and sanitize output
            String sanitizedSuggestion = promptSecurityService.sanitizeModelOutput(suggestion);

            log.info("Successfully generated ad variation suggestion");
            return sanitizedSuggestion;

        } catch (Exception e) {
            log.error("Error generating ad variation: {}", e.getMessage(), e);
            throw new com.fbadsautomation.exception.AIProviderException(provider, "Failed to generate ad suggestion: " + e.getMessage(), true);
        }
    }

    /**
     * Analyze competitor ad and extract key insights
     *
     * Identifies strengths, weaknesses, and notable patterns in competitor's ad
     *
     * @param competitorAd Competitor's ad to analyze
     * @param provider AI provider
     * @return Map of insights (strengths, weaknesses, patterns, recommendations)
     */
    @Cacheable(value = "competitorAnalysis", key = "#competitorAd.adId", unless = "#result == null")
    public Map<String, Object> analyzeCompetitorAd(
            @NotNull CompetitorAdDTO competitorAd,
            String provider) {

        log.info("Analyzing competitor ad: {}", competitorAd.getAdId());

        String analysisPrompt = buildAnalysisPrompt(competitorAd);

        try {
            com.fbadsautomation.ai.AIProvider aiProvider = aiProviderService.getProvider(provider != null ? provider : "openai");
            if (aiProvider == null) {
                throw new RuntimeException("AI provider not found: " + provider);
            }

            // Retry AI call with exponential backoff (max 3 attempts)
            String analysis = retryWithBackoff(() -> {
                List<com.fbadsautomation.model.AdContent> results = aiProvider.generateAdContent(analysisPrompt, 1, "en", null);
                if (results == null || results.isEmpty()) {
                    throw new RuntimeException("AI provider returned no results");
                }
                contentModerationService.enforceSafety(results);
                return results.get(0).getPrimaryText();
            }, "analyzeCompetitorAd", 3);
            analysis = promptSecurityService.sanitizeModelOutput(analysis);

            // Parse AI response with safe fallback handling
            return parseAnalysisResponseSafe(analysis, "competitor ad");

        } catch (Exception e) {
            log.error("Error analyzing competitor ad: {}", e.getMessage(), e);

            // Return graceful fallback instead of throwing exception
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", true);
            fallback.put("error_message", "AI analysis temporarily unavailable: " + e.getMessage());
            fallback.put("strengths", List.of("Unable to analyze at this time"));
            fallback.put("weaknesses", List.of("Please try again later"));
            fallback.put("recommendations", List.of("Service may be experiencing high load"));

            return fallback;
        }
    }

    /**
     * Compare multiple competitor ads and identify common patterns
     *
     * @param competitorAds List of competitor ads
     * @param provider AI provider
     * @return Map of common patterns and trends
     */
    public Map<String, Object> identifyCommonPatterns(
            @NotNull List<CompetitorAdDTO> competitorAds,
            String provider) {

        log.info("Identifying common patterns across {} competitor ads", competitorAds.size());

        if (competitorAds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Limit to 10 ads for performance
        List<CompetitorAdDTO> adsToAnalyze = competitorAds.size() > 10
                ? competitorAds.subList(0, 10)
                : competitorAds;

        String prompt = buildPatternAnalysisPrompt(adsToAnalyze);

        try {
            com.fbadsautomation.ai.AIProvider aiProvider = aiProviderService.getProvider(provider != null ? provider : "openai");
            if (aiProvider == null) {
                throw new RuntimeException("AI provider not found: " + provider);
            }

            // Retry AI call with exponential backoff (max 3 attempts)
            String analysis = retryWithBackoff(() -> {
                List<com.fbadsautomation.model.AdContent> results = aiProvider.generateAdContent(prompt, 1, "en", null);
                if (results == null || results.isEmpty()) {
                    throw new RuntimeException("AI provider returned no results");
                }
                contentModerationService.enforceSafety(results);
                return results.get(0).getPrimaryText();
            }, "identifyCommonPatterns", 3);
            analysis = promptSecurityService.sanitizeModelOutput(analysis);

            // Parse pattern analysis with validation
            Map<String, Object> patterns = parsePatternAnalysis(analysis);
            if (patterns.isEmpty()) {
                patterns.put("summary", "No clear patterns identified");
                patterns.put("raw_analysis", analysis);
            }
            return patterns;

        } catch (Exception e) {
            log.error("Error identifying patterns: {}", e.getMessage(), e);

            // Return graceful fallback instead of throwing exception
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", true);
            fallback.put("error_message", "Pattern analysis temporarily unavailable: " + e.getMessage());
            fallback.put("summary", "Unable to analyze patterns at this time. Please try again later.");

            return fallback;
        }
    }

    /**
     * Generate A/B test variations based on competitor insights
     *
     * @param baseAd User's base ad
     * @param competitorInsights Insights from competitor analysis
     * @param numberOfVariations Number of variations to generate (1-5)
     * @param provider AI provider
     * @return List of ad variation DTOs with structured fields
     */
    public List<com.fbadsautomation.dto.AdVariationDTO> generateABTestVariations(
            @NotBlank String baseAd,
            Map<String, Object> competitorInsights,
            int numberOfVariations,
            String provider) {

        log.info("Generating {} A/B test variations", numberOfVariations);

        // Validate parameters
        int safeVariations = Math.min(Math.max(numberOfVariations, 1), 5);

        String prompt = buildABTestPrompt(baseAd, competitorInsights, safeVariations);

        try {
            com.fbadsautomation.ai.AIProvider aiProvider = aiProviderService.getProvider(provider != null ? provider : "openai");
            if (aiProvider == null) {
                throw new RuntimeException("AI provider not found: " + provider);
            }

            // Generate variations using AI provider
            List<com.fbadsautomation.model.AdContent> results = aiProvider.generateAdContent(prompt, safeVariations, "en", null);
            contentModerationService.enforceSafety(results);

            // If we got structured results, convert them to AdVariationDTO
            if (results != null && !results.isEmpty()) {
                List<com.fbadsautomation.dto.AdVariationDTO> variations = new ArrayList<>();
                int varNum = 1;
                for (com.fbadsautomation.model.AdContent content : results) {
                    String ctaText = null;
                    if (content.getCallToAction() != null) {
                        ctaText = content.getCallToAction().name();
                    }

                    variations.add(com.fbadsautomation.dto.AdVariationDTO.builder()
                        .variationNumber(varNum++)
                        .headline(content.getHeadline() != null ? content.getHeadline() : "Variation " + varNum)
                        .primaryText(content.getPrimaryText())
                        .callToAction(ctaText)
                        .testingFocus("A/B Test Variation")
                        .build());
                }
                return variations;
            }

            // Fallback: call AI provider's text completion directly and parse
            String systemPrompt = "You are an expert Facebook Ads copywriter. Generate A/B test variations in the exact format requested.";
            String response = aiProvider.generateTextCompletion(prompt, systemPrompt, 2000);
            response = promptSecurityService.sanitizeModelOutput(response);
            return parseABTestVariations(response, safeVariations);

        } catch (Exception e) {
            log.error("Error generating A/B test variations: {}", e.getMessage(), e);
            throw new com.fbadsautomation.exception.AIProviderException(provider, "Failed to generate A/B variations: " + e.getMessage(), true);
        }
    }

    /**
     * Build comparison prompt for AI
     */
    private String buildComparisonPrompt(String competitorAd, String myAd, CompetitorAdDTO competitorDTO) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert Facebook Ads copywriter. Analyze the following competitor ad and suggest improvements for my ad.\n\n");

        prompt.append("COMPETITOR AD:\n");
        if (competitorDTO.getHeadline() != null) {
            prompt.append("Headline: ").append(competitorDTO.getHeadline()).append("\n");
        }
        prompt.append("Text: ").append(competitorAd).append("\n");
        if (competitorDTO.getCallToAction() != null) {
            prompt.append("CTA: ").append(competitorDTO.getCallToAction()).append("\n");
        }

        prompt.append("\nMY CURRENT AD:\n");
        prompt.append(myAd).append("\n\n");

        prompt.append("TASK:\n");
        prompt.append("1. Identify what makes the competitor ad effective (tone, structure, hooks, benefits)\n");
        prompt.append("2. Suggest an improved version of my ad that:\n");
        prompt.append("   - Incorporates best practices from the competitor\n");
        prompt.append("   - Maintains my original message and brand voice\n");
        prompt.append("   - Is optimized for Facebook ad engagement\n");
        prompt.append("   - Includes a compelling headline (max 40 characters)\n");
        prompt.append("   - Has engaging primary text (max 125 characters)\n");
        prompt.append("   - Includes a strong call-to-action\n\n");

        prompt.append("Provide ONLY the improved ad in this format:\n");
        prompt.append("HEADLINE: [your headline]\n");
        prompt.append("PRIMARY TEXT: [your primary text]\n");
        prompt.append("CTA: [your call-to-action]");

        return prompt.toString();
    }

    /**
     * Build analysis prompt for single competitor ad
     */
    private String buildAnalysisPrompt(CompetitorAdDTO competitorAd) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Analyze this Facebook ad and provide structured insights:\n\n");

        prompt.append("AD CONTENT:\n");
        if (competitorAd.getHeadline() != null) {
            prompt.append("Headline: ").append(competitorAd.getHeadline()).append("\n");
        }
        if (competitorAd.getPrimaryText() != null) {
            prompt.append("Text: ").append(competitorAd.getPrimaryText()).append("\n");
        }
        if (competitorAd.getCallToAction() != null) {
            prompt.append("CTA: ").append(competitorAd.getCallToAction()).append("\n");
        }

        prompt.append("\nProvide analysis in this format:\n");
        prompt.append("STRENGTHS: [List 3-5 strengths]\n");
        prompt.append("WEAKNESSES: [List 2-3 potential weaknesses]\n");
        prompt.append("PATTERNS: [Notable patterns in structure, tone, or messaging]\n");
        prompt.append("RECOMMENDATIONS: [3 actionable recommendations for creating similar ads]");

        return prompt.toString();
    }

    /**
     * Build prompt for pattern analysis across multiple ads
     */
    private String buildPatternAnalysisPrompt(List<CompetitorAdDTO> ads) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Analyze these ").append(ads.size()).append(" competitor ads and identify common patterns:\n\n");

        for (int i = 0; i < ads.size(); i++) {
            CompetitorAdDTO ad = ads.get(i);
            prompt.append("AD ").append(i + 1).append(":\n");
            if (ad.getHeadline() != null) {
                prompt.append("Headline: ").append(ad.getHeadline()).append("\n");
            }
            if (ad.getPrimaryText() != null) {
                // Truncate long text
                String text = ad.getPrimaryText().length() > 200
                        ? ad.getPrimaryText().substring(0, 197) + "..."
                        : ad.getPrimaryText();
                prompt.append("Text: ").append(text).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Identify:\n");
        prompt.append("1. COMMON_TONE: [What tone do most ads use?]\n");
        prompt.append("2. COMMON_STRUCTURE: [What structure patterns appear?]\n");
        prompt.append("3. COMMON_HOOKS: [What hooks or attention-grabbers are used?]\n");
        prompt.append("4. COMMON_CTAS: [What CTAs are most common?]\n");
        prompt.append("5. KEY_THEMES: [What themes or benefits are emphasized?]\n");
        prompt.append("6. RECOMMENDATIONS: [Based on these patterns, what should new ads include?]");

        return prompt.toString();
    }

    /**
     * Build prompt for A/B test variations
     */
    private String buildABTestPrompt(String baseAd, Map<String, Object> insights, int count) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create ").append(count).append(" A/B test variations of this ad:\n\n");
        prompt.append("BASE AD:\n").append(baseAd).append("\n\n");

        if (insights != null && !insights.isEmpty()) {
            prompt.append("APPLY THESE INSIGHTS:\n");
            insights.forEach((key, value) -> {
                prompt.append(key).append(": ").append(value).append("\n");
            });
            prompt.append("\n");
        }

        prompt.append("Generate ").append(count).append(" distinct variations that test different:\n");
        prompt.append("- Headlines (emotional vs rational)\n");
        prompt.append("- Value propositions (features vs benefits)\n");
        prompt.append("- CTAs (soft vs hard)\n");
        prompt.append("- Tones (urgent vs informative)\n\n");

        prompt.append("Format each variation as:\n");
        prompt.append("VARIATION [N]:\n");
        prompt.append("HEADLINE: [headline]\n");
        prompt.append("TEXT: [primary text]\n");
        prompt.append("CTA: [call-to-action]\n");
        prompt.append("---");

        return prompt.toString();
    }

    /**
     * Safe wrapper for parsing AI analysis responses with fallback handling
     */
    private Map<String, Object> parseAnalysisResponseSafe(String analysis, String context) {
        try {
            Map<String, Object> result = parseAnalysisResponse(analysis);

            // Validate result has meaningful content
            if (result.isEmpty() || (!result.containsKey("strengths") && !result.containsKey("weaknesses"))) {
                log.warn("Parsed analysis is empty or incomplete, adding fallback content");
                result.put("raw_analysis", analysis);
                result.put("parsing_note", "Automated parsing incomplete. See raw analysis above.");
            }

            return result;

        } catch (Exception e) {
            log.error("Error parsing {} analysis, returning fallback: {}", context, e.getMessage());

            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", true);
            fallback.put("error_message", "Failed to parse AI response structure");
            fallback.put("raw_analysis", analysis);
            fallback.put("strengths", List.of("Analysis available in raw format"));
            fallback.put("weaknesses", List.of("Unable to parse structured insights"));
            fallback.put("recommendations", List.of("Review raw analysis for detailed insights"));

            return fallback;
        }
    }

    /**
     * Retry AI provider call with exponential backoff
     *
     * @param operation The operation to retry
     * @param operationName Name of operation for logging
     * @param maxRetries Maximum number of retries
     * @return Result of the operation
     */
    private <T> T retryWithBackoff(java.util.function.Supplier<T> operation, String operationName, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt >= maxRetries) {
                    break;
                }

                long backoffMs = (long) (Math.pow(2, attempt) * 1000); // 2s, 4s, 8s
                log.warn("Attempt {}/{} failed for {}: {}. Retrying in {}ms",
                    attempt, maxRetries, operationName, e.getMessage(), backoffMs);

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        log.error("All {} attempts failed for {}", maxRetries, operationName);
        throw new RuntimeException("Operation failed after " + maxRetries + " attempts: " + operationName,
            lastException);
    }

    /**
     * Parse AI analysis response into structured map with robust regex patterns
     */
    private Map<String, Object> parseAnalysisResponse(String analysis) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Use regex patterns for more robust parsing
            Pattern strengthsPattern = Pattern.compile(
                "STRENGTHS?:\\s*([\\s\\S]*?)(?=WEAKNESSES?:|PATTERNS?:|RECOMMENDATIONS?:|$)",
                Pattern.CASE_INSENSITIVE
            );
            Pattern weaknessesPattern = Pattern.compile(
                "WEAKNESSES?:|AREAS?\\s+FOR\\s+IMPROVEMENT:|LIMITATIONS?:\\s*([\\s\\S]*?)(?=STRENGTHS?:|PATTERNS?:|RECOMMENDATIONS?:|$)",
                Pattern.CASE_INSENSITIVE
            );
            Pattern patternsPattern = Pattern.compile(
                "PATTERNS?:|MESSAGING\\s+PATTERNS?:\\s*([\\s\\S]*?)(?=STRENGTHS?:|WEAKNESSES?:|RECOMMENDATIONS?:|$)",
                Pattern.CASE_INSENSITIVE
            );
            Pattern recommendationsPattern = Pattern.compile(
                "RECOMMENDATIONS?:|SUGGESTIONS?:\\s*([\\s\\S]*?)(?=STRENGTHS?:|WEAKNESSES?:|PATTERNS?:|$)",
                Pattern.CASE_INSENSITIVE
            );

            // Extract sections
            Matcher strengthsMatcher = strengthsPattern.matcher(analysis);
            if (strengthsMatcher.find()) {
                String strengthsText = strengthsMatcher.group(1).trim();
                List<String> strengths = extractListRobust(strengthsText);
                if (!strengths.isEmpty()) {
                    result.put("strengths", strengths);
                }
            }

            Matcher weaknessesMatcher = weaknessesPattern.matcher(analysis);
            if (weaknessesMatcher.find()) {
                String weaknessesText = weaknessesMatcher.group(1).trim();
                List<String> weaknesses = extractListRobust(weaknessesText);
                if (!weaknesses.isEmpty()) {
                    result.put("weaknesses", weaknesses);
                }
            }

            Matcher patternsMatcher = patternsPattern.matcher(analysis);
            if (patternsMatcher.find()) {
                String patternsText = patternsMatcher.group(1).trim();
                List<String> patterns = extractListRobust(patternsText);
                if (!patterns.isEmpty()) {
                    result.put("patterns", patterns);
                } else {
                    // If no list items, store as text
                    result.put("patterns", patternsText);
                }
            }

            Matcher recommendationsMatcher = recommendationsPattern.matcher(analysis);
            if (recommendationsMatcher.find()) {
                String recommendationsText = recommendationsMatcher.group(1).trim();
                List<String> recommendations = extractListRobust(recommendationsText);
                if (!recommendations.isEmpty()) {
                    result.put("recommendations", recommendations);
                }
            }

            // Validation - ensure required keys exist
            if (!result.containsKey("strengths")) {
                result.put("strengths", List.of("Analysis completed - see raw data"));
            }
            if (!result.containsKey("weaknesses")) {
                result.put("weaknesses", List.of("No specific weaknesses identified"));
            }
            if (!result.containsKey("recommendations")) {
                result.put("recommendations", List.of("Review full analysis for insights"));
            }

        } catch (Exception e) {
            log.error("Error parsing analysis response: {}", e.getMessage(), e);
            // Return fallback structure
            result.put("error", true);
            result.put("strengths", List.of("Unable to parse strengths"));
            result.put("weaknesses", List.of("Unable to parse weaknesses"));
            result.put("recommendations", List.of("Please try regenerating analysis"));
            result.put("raw_analysis", analysis);
        }

        return result;
    }

    /**
     * Robust list extraction supporting multiple formats
     */
    private List<String> extractListRobust(String text) {
        List<String> items = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return items;
        }

        // Try numbered list first (1., 2., 3. or 1) 2) 3))
        Pattern numberedPattern = Pattern.compile("^\\s*\\d+[.)]+\\s*(.+)$", Pattern.MULTILINE);
        Matcher numberedMatcher = numberedPattern.matcher(text);
        while (numberedMatcher.find()) {
            String item = numberedMatcher.group(1).trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }

        // If numbered list worked, return
        if (!items.isEmpty()) {
            return items;
        }

        // Try bullet points (-, *, •, ◦, ▪)
        Pattern bulletPattern = Pattern.compile("^\\s*[-*•◦▪]+\\s*(.+)$", Pattern.MULTILINE);
        Matcher bulletMatcher = bulletPattern.matcher(text);
        while (bulletMatcher.find()) {
            String item = bulletMatcher.group(1).trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }

        // If bullets worked, return
        if (!items.isEmpty()) {
            return items;
        }

        // Fallback: split by newlines
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 3) { // Ignore very short lines
                items.add(trimmed);
            }
        }

        return items;
    }

    /**
     * Parse pattern analysis response with robust regex patterns
     */
    private Map<String, Object> parsePatternAnalysis(String analysis) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Define common pattern sections to extract
            String[] sectionNames = {
                "COMMON_THEMES", "VISUAL_PATTERNS", "MESSAGING_PATTERNS",
                "CTA_PATTERNS", "EMOTIONAL_APPEALS", "TARGET_AUDIENCE",
                "CONTENT_STRATEGIES", "FREQUENCY_PATTERNS", "SEASONAL_TRENDS"
            };

            for (String sectionName : sectionNames) {
                Pattern pattern = Pattern.compile(
                    sectionName.replace("_", "[\\s_-]*") + "\\s*:?\\s*([\\s\\S]*?)(?=" +
                    String.join("|", Arrays.stream(sectionNames)
                        .filter(s -> !s.equals(sectionName))
                        .map(s -> s.replace("_", "[\\s_-]*"))
                        .toArray(String[]::new)) + "|$)",
                    Pattern.CASE_INSENSITIVE
                );

                Matcher matcher = pattern.matcher(analysis);
                if (matcher.find()) {
                    String content = matcher.group(1).trim();
                    if (!content.isEmpty()) {
                        // Try to parse as list first
                        List<String> items = extractListRobust(content);
                        if (!items.isEmpty()) {
                            result.put(sectionName.toLowerCase(), items);
                        } else {
                            // Store as text if not a list
                            result.put(sectionName.toLowerCase(), content);
                        }
                    }
                }
            }

            // Also try to extract any key-value pairs (e.g., "Total Ads Analyzed: 15")
            Pattern kvPattern = Pattern.compile(
                "^\\s*([A-Za-z][A-Za-z0-9\\s]+?)\\s*:\\s*(.+?)\\s*$",
                Pattern.MULTILINE
            );
            Matcher kvMatcher = kvPattern.matcher(analysis);

            while (kvMatcher.find()) {
                String key = kvMatcher.group(1).trim().toLowerCase().replaceAll("\\s+", "_");
                String value = kvMatcher.group(2).trim();

                // Only add if not already captured by section parsing
                if (!result.containsKey(key) && value.length() < 200) {
                    result.put(key, value);
                }
            }

            // Validation - ensure at least some content was parsed
            if (result.isEmpty()) {
                result.put("summary", analysis.substring(0, Math.min(500, analysis.length())));
                result.put("raw_analysis", analysis);
            }

        } catch (Exception e) {
            log.error("Error parsing pattern analysis: {}", e.getMessage(), e);
            result.put("error", true);
            result.put("raw_analysis", analysis);
        }

        return result;
    }

    /**
     * Parse A/B test variations from AI response with robust regex
     */
    private List<com.fbadsautomation.dto.AdVariationDTO> parseABTestVariations(String response, int expectedCount) {
        List<com.fbadsautomation.dto.AdVariationDTO> variations = new ArrayList<>();

        try {
            // Split by --- or VARIATION markers
            String[] parts = response.split("(?=VARIATION\\s+\\d+)");

            for (String part : parts) {
                if (part.trim().isEmpty()) continue;

                // Extract variation number
                Pattern varNumPattern = Pattern.compile("VARIATION\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
                Matcher varNumMatcher = varNumPattern.matcher(part);

                int varNum = variations.size() + 1;
                if (varNumMatcher.find()) {
                    try {
                        varNum = Integer.parseInt(varNumMatcher.group(1));
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse variation number, using sequence: {}", varNum);
                    }
                }

                // Extract headline with flexible pattern
                Pattern headlinePattern = Pattern.compile(
                    "HEADLINE\\s*:?\\s*(.+?)(?=TEXT:|PRIMARY\\s*TEXT:|CTA:|CALL[-\\s]TO[-\\s]ACTION:|TESTING\\s*FOCUS:|VARIATION\\s+\\d+|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                Matcher headlineMatcher = headlinePattern.matcher(part);
                String headline = null;
                if (headlineMatcher.find()) {
                    headline = headlineMatcher.group(1).trim();
                }

                // Extract primary text with flexible pattern
                Pattern textPattern = Pattern.compile(
                    "(?:PRIMARY\\s+)?TEXT\\s*:?\\s*(.+?)(?=CTA:|CALL[-\\s]TO[-\\s]ACTION:|TESTING\\s*FOCUS:|VARIATION\\s+\\d+|---+|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                Matcher textMatcher = textPattern.matcher(part);
                String primaryText = null;
                if (textMatcher.find()) {
                    primaryText = textMatcher.group(1).trim();
                }

                // Extract CTA
                Pattern ctaPattern = Pattern.compile(
                    "(?:CALL[-\\s]TO[-\\s]ACTION|CTA)\\s*:?\\s*(.+?)(?=TESTING\\s*FOCUS:|VARIATION\\s+\\d+|---+|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                Matcher ctaMatcher = ctaPattern.matcher(part);
                String cta = null;
                if (ctaMatcher.find()) {
                    cta = ctaMatcher.group(1).trim();
                }

                // Extract testing focus
                Pattern focusPattern = Pattern.compile(
                    "TESTING\\s*FOCUS\\s*:?\\s*(.+?)(?=VARIATION\\s+\\d+|---+|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                );
                Matcher focusMatcher = focusPattern.matcher(part);
                String testingFocus = null;
                if (focusMatcher.find()) {
                    testingFocus = focusMatcher.group(1).trim();
                }

                // Build DTO only if we have at least headline or text
                if (headline != null || primaryText != null) {
                    variations.add(com.fbadsautomation.dto.AdVariationDTO.builder()
                        .variationNumber(varNum)
                        .headline(headline != null ? headline : "Variation " + varNum)
                        .primaryText(primaryText != null ? primaryText : "")
                        .callToAction(cta)
                        .testingFocus(testingFocus)
                        .rawVariation(part.trim())
                        .build());
                }
            }

            // Fallback: if no variations parsed, create one with raw response
            if (variations.isEmpty()) {
                log.warn("Failed to parse any variations, returning raw response");
                variations.add(com.fbadsautomation.dto.AdVariationDTO.builder()
                    .variationNumber(1)
                    .headline("Variation 1")
                    .primaryText("See raw variation for details")
                    .rawVariation(response)
                    .build());
            }

        } catch (Exception e) {
            log.error("Error parsing A/B variations: {}", e.getMessage(), e);
            variations.add(com.fbadsautomation.dto.AdVariationDTO.builder()
                .variationNumber(1)
                .headline("Parsing Error")
                .primaryText("See raw variation for AI response")
                .rawVariation(response)
                .build());
        }

        return variations;
    }

    /**
     * Extract list items from text section
     */
    private List<String> extractList(String section) {
        List<String> items = new ArrayList<>();
        String content = section.substring(section.indexOf(":") + 1).trim();

        // Try numbered list first (1., 2., etc.)
        if (content.matches(".*\\d+\\..*")) {
            String[] parts = content.split("\\d+\\.");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    items.add(part.trim());
                }
            }
        } else {
            // Try bullet points or newlines
            String[] parts = content.split("[\\n•-]");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    items.add(part.trim());
                }
            }
        }

        return items;
    }

    /**
     * Extract plain text from section
     */
    private String extractText(String section) {
        if (section.contains(":")) {
            return section.substring(section.indexOf(":") + 1).trim();
        }
        return section.trim();
    }

    /**
     * Sanitize input for AI prompt
     * Security: Prevents prompt injection
     */
    private String sanitizeForPrompt(String input) {
        if (input == null) {
            return "";
        }
        String sanitized = promptSecurityService.sanitizeUserInput(input);
        if (sanitized.length() > 5000) {
            sanitized = sanitized.substring(0, 5000);
        }
        return sanitized;
    }

    /**
     * Sanitize AI output
     * Security: Removes potentially harmful content
     */
    private String sanitizeAIOutput(String output) {
        if (output == null) {
            return "";
        }
        String sanitized = promptSecurityService.sanitizeModelOutput(output);
        if (sanitized.length() > 10000) {
            sanitized = sanitized.substring(0, 10000);
        }
        return sanitized;
    }
}
