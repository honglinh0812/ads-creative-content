package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.dto.AdImprovementRequest;
import com.fbadsautomation.dto.ReferenceAdData;
import com.fbadsautomation.dto.ReferenceAnalysisRequest;
import com.fbadsautomation.dto.ReferenceAnalysisResponse;
import com.fbadsautomation.dto.ReferenceStyleProfile;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AsyncJobStatus;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.util.ValidationMessages.Language;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdImprovementService {

    private final MetaAdLibraryService metaAdLibraryService;
    private final AdService adService;
    private final AsyncJobService asyncJobService;
    private final AsyncAIContentService asyncAIContentService;
    private final ObjectMapper objectMapper;
    private final QualityDetailsMapper qualityDetailsMapper;

    private static final Map<String, List<String>> STYLE_KEYWORDS = Map.of(
            "PROFESSIONAL", List.of("expert", "chuyên gia", "bảo hành", "uy tín"),
            "CASUAL", List.of("friendly", "thân thiện", "dễ thương", "chill"),
            "HUMOROUS", List.of("funny", "hài hước", "cười", "lol"),
            "URGENT", List.of("limited time", "ngay hôm nay", "cuối cùng", "gấp"),
            "LUXURY", List.of("luxury", "cao cấp", "đẳng cấp", "premium", "xa xỉ"),
            "EDUCATIONAL", List.of("tip", "how to", "bí quyết", "hướng dẫn"),
            "INSPIRATIONAL", List.of("truyền cảm hứng", "dream", "biến đổi", "làm chủ"),
            "MINIMALIST", List.of("đơn giản", "tối giản", "minimal", "clean")
    );

    private static final Map<FacebookCTA, List<String>> CTA_KEYWORDS;

    static {
        CTA_KEYWORDS = new EnumMap<>(FacebookCTA.class);
        CTA_KEYWORDS.put(FacebookCTA.SHOP_NOW, List.of("shop now", "mua ngay", "buy now"));
        CTA_KEYWORDS.put(FacebookCTA.LEARN_MORE, List.of("learn more", "tìm hiểu thêm", "chi tiết"));
        CTA_KEYWORDS.put(FacebookCTA.SIGN_UP, List.of("sign up", "đăng ký", "tham gia"));
        CTA_KEYWORDS.put(FacebookCTA.APPLY_NOW, List.of("apply now", "ứng tuyển", "đăng ký ngay"));
        CTA_KEYWORDS.put(FacebookCTA.DOWNLOAD, List.of("download", "tải về", "tải xuống"));
        CTA_KEYWORDS.put(FacebookCTA.GET_OFFER, List.of("get offer", "nhận ưu đãi", "ưu đãi"));
    }

    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\$|usd|đ|vnđ|% off|discount|giảm giá|chỉ còn)");

    private static final Set<String> SECOND_PERSON_KEYWORDS = Set.of(
            "you", "your", "yours", "yourself", "bạn", "của bạn", "anh", "chị", "em", "quý khách"
    );

    private static final Map<String, List<String>> TONE_KEYWORD_MAP = Map.of(
            "UPBEAT", List.of("!", "🔥", "wow", "đừng bỏ lỡ", "limited", "hot", "ưu đãi"),
            "INSPIRATIONAL", List.of("imagine", "transform", "hành trình", "cảm hứng", "dream"),
            "LUXURY", List.of("cao cấp", "premium", "đẳng cấp", "sang trọng", "bespoke"),
            "EDUCATIONAL", List.of("bí quyết", "tips", "how to", "mẹo", "insight", "guide"),
            "EMOTIONAL", List.of("yêu", "thương", "trân trọng", "love", "care")
    );

    /**
     * Kiểm tra link quảng cáo tham chiếu và lấy nội dung nếu có access token.
     */
    public ReferenceAnalysisResponse analyzeReference(ReferenceAnalysisRequest request) {
        if (!metaAdLibraryService.isValidAdLibraryUrl(request.getReferenceLink())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "URL Meta Ad Library không hợp lệ");
        }

        String adId = metaAdLibraryService.extractAdIdFromUrl(request.getReferenceLink());
        if (!StringUtils.hasText(adId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể trích xuất Ad ID từ URL");
        }

        Map<String, Object> metadata = Collections.emptyMap();
        String referenceContent = request.getFallbackContent();
        ReferenceAdData referenceAdData = null;

        if (StringUtils.hasText(request.getAccessToken())) {
            Map<String, Object> apiResult = metaAdLibraryService.extractOfficialAdContent(adId, request.getAccessToken());
            if (Boolean.TRUE.equals(apiResult.get("success"))) {
                //noinspection unchecked
                metadata = (Map<String, Object>) apiResult.getOrDefault("content", Collections.emptyMap());
                referenceAdData = metaAdLibraryService.buildReferenceAdData(metadata);
                referenceContent = metaAdLibraryService.summarizeReferenceAd(referenceAdData, metadata);
            } else {
                log.warn("Meta API fallback failed: {}", apiResult.get("message"));
            }
        }

        if (referenceAdData == null) {
            List<Map<String, Object>> scraped = metaAdLibraryService.extractAdTextAndImages(List.of(request.getReferenceLink()));
            if (!scraped.isEmpty()) {
                String text = (String) scraped.get(0).get("text");
                referenceAdData = ReferenceAdData.builder()
                        .primaryText(text)
                        .build();
                referenceContent = text;
            }
        }

        ReferenceAnalysisResponse.ReferenceInsights insights = buildInsights(referenceContent);
        String detectedStyle = detectStyle(referenceContent);
        FacebookCTA detectedCTA = detectCallToAction(referenceContent);
        ReferenceStyleProfile styleProfile = analyzeStyleProfile(referenceContent, detectedCTA);

        return ReferenceAnalysisResponse.builder()
                .success(true)
                .referenceAdId(adId)
                .referenceContent(referenceContent)
                .metadata(metadata)
                .message("Phân tích link tham chiếu thành công")
                .detectedStyle(detectedStyle)
                .suggestedCallToAction(detectedCTA)
                .referenceAdData(referenceAdData)
                .insights(insights)
                .styleProfile(styleProfile)
                .build();
    }

    /**
     * Sinh quảng cáo mới dựa trên nội dung người dùng và phong cách tham chiếu.
     */
    public AdGenerationResponse generateImprovedAds(AdImprovementRequest request, Long userId) {
        validateImprovementRequest(request);

        Language resolvedLanguage = resolveLanguageEnum(request.getLanguage());
        String finalLanguage = toLanguageCode(resolvedLanguage);
        String mergedPrompt = buildPromptFromRequest(request, resolvedLanguage);
        List<String> adLinks = new ArrayList<>();
        if (StringUtils.hasText(request.getReferenceLink())) {
            adLinks.add(request.getReferenceLink());
        }

        Ad tempAd = new Ad();
        tempAd.setName(request.getName());
        tempAd.setAdType(adService.mapFrontendAdTypeToEnum(request.getAdType()));
        tempAd.setPrompt(mergedPrompt);

        List<AdContent> contents = adService.generatePreviewContent(tempAd,
                mergedPrompt,
                null,
                determineTextProvider(request),
                determineImageProvider(request),
                request.getNumberOfVariations(),
                finalLanguage,
                adLinks,
                request.getReferenceContent(),
                request.getMediaFileUrl(),
                request.getCallToAction(),
                request.getWebsiteUrl(),
                request.getLeadFormQuestions(),
                request.getAudienceSegment(),
                request.getPersonaId(),
                request.getTrendingKeywords(),
                request.getVariations(),
                false);

        if (CollectionUtils.isEmpty(contents)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không sinh được biến thể nào");
        }

        List<AdGenerationResponse.AdVariation> variations = contents.stream()
                .map(content -> convertToAdVariation(content, finalLanguage))
                .toList();

        AdGenerationResponse.ValidationReport validationReport = buildValidationReport(contents);

        return AdGenerationResponse.builder()
                .adId(tempAd.getId())
                .variations(variations)
                .status("success")
                .message("Đã sinh các biến thể cải thiện thành công")
                .validationReport(validationReport)
                .build();
    }

    private int calculateTotalSteps(AdImprovementRequest request, String resolvedImageProvider) {
        int steps = 10;
        if (StringUtils.hasText(resolvedImageProvider)) {
            steps += request.getNumberOfVariations() != null ? request.getNumberOfVariations() : 3;
        }
        return steps;
    }

    private AdContent.ContentType mapFrontendContentTypeToEnum(String frontendType) {
        if (!StringUtils.hasText(frontendType)) {
            return AdContent.ContentType.COMBINED;
        }
        return switch (frontendType.toLowerCase()) {
            case "page_post_ad", "page_post" -> AdContent.ContentType.PAGE_POST;
            case "website_conversion_ad", "lead_form_ad", "combined" -> AdContent.ContentType.COMBINED;
            default -> AdContent.ContentType.COMBINED;
        };
    }

    /**
     * Khởi tạo job async cho luồng học tập quảng cáo nhằm tái sử dụng cùng cơ chế polling.
     */
    public Map<String, Object> generateImprovedAdsAsync(AdImprovementRequest request, Long userId) {
        validateImprovementRequest(request);

        if (!asyncJobService.canCreateJob(userId)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đang có quá nhiều tác vụ đang chạy. Vui lòng chờ hoàn tất trước khi tạo mới.");
        }

        Language resolvedLanguage = resolveLanguageEnum(request.getLanguage());
        String safeLanguage = toLanguageCode(resolvedLanguage);
        String prompt = buildPromptFromRequest(request, resolvedLanguage);
        List<String> adLinks = new ArrayList<>();
        if (StringUtils.hasText(request.getReferenceLink())) {
            adLinks.add(request.getReferenceLink().trim());
        }

        AdContent.ContentType contentType = mapFrontendContentTypeToEnum(request.getAdType());
        FacebookCTA callToAction = request.getCallToAction() != null
                ? request.getCallToAction()
                : FacebookCTA.LEARN_MORE;
        String textProvider = determineTextProvider(request);
        String imageProvider = determineImageProvider(request);
        String extractedReference = StringUtils.hasText(request.getReferenceContent())
                ? request.getReferenceContent()
                : (StringUtils.hasText(request.getProductDescription()) ? request.getProductDescription() : "");

        int totalSteps = calculateTotalSteps(request, imageProvider);
        String jobId = asyncJobService.createJob(userId,
                AsyncJobStatus.JobType.AD_CONTENT_GENERATION,
                totalSteps);

        asyncAIContentService.generateContentAsync(
                jobId,
                userId,
                request.getCampaignId(),
                prompt,
                contentType,
                textProvider,
                imageProvider,
                request.getNumberOfVariations(),
                safeLanguage,
                adLinks,
                extractedReference,
                callToAction,
                request.getMediaFileUrl(),
                request.getWebsiteUrl(),
                request.getLeadFormQuestions(),
                request.getAudienceSegment(),
                request.getPersonaId(),
                request.getTrendingKeywords(),
                request.getCreativeStyle(),
                request.getVariations(),
                false
        );

        return Map.of(
                "jobId", jobId,
                "status", "accepted",
                "message", "Async ad improvement started. Use the job ID to monitor progress."
        );
    }

    /**
     * Lưu các biến thể đã chọn dựa trên logic createAdWithExistingContent có sẵn.
     */
    public AdGenerationResponse saveImprovedAds(AdGenerationRequest request, Long userId) {
        String languageCode = request.getLanguage() != null ? request.getLanguage() : "vi";
        Map<String, Object> adResult;

        if (request.getSelectedVariations() != null && !request.getSelectedVariations().isEmpty()) {
            adResult = adService.createAdWithExistingContent(request.getCampaignId(),
                    request.getAdType(),
                    request.getPrompt(),
                    request.getName(),
                    null,
                    userId,
                    request.getSelectedVariations(),
                    request.getAdLinks(),
                    request.getMediaFileUrl(),
                    request.getWebsiteUrl(),
                    request.getLeadFormQuestions(),
                    request.getAdStyle(),
                    false);
        } else if (request.getSelectedVariation() != null) {
            adResult = adService.createAdWithExistingContent(request.getCampaignId(),
                    request.getAdType(),
                    request.getPrompt(),
                    request.getName(),
                    null,
                    userId,
                    request.getSelectedVariation(),
                    request.getAdLinks(),
                    request.getMediaFileUrl(),
                    request.getWebsiteUrl(),
                    request.getLeadFormQuestions(),
                    request.getAdStyle(),
                    false);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không có biến thể nào được chọn để lưu");
        }

        @SuppressWarnings("unchecked")
        List<AdContent> contents = (List<AdContent>) adResult.get("contents");

        Ad ad = null;
        int totalAdsCreated = 0;

        if (adResult.containsKey("ad")) {
            ad = (Ad) adResult.get("ad");
            totalAdsCreated = 1;
        } else if (adResult.containsKey("ads")) {
            @SuppressWarnings("unchecked")
            List<Ad> createdAds = (List<Ad>) adResult.get("ads");
            if (createdAds != null && !createdAds.isEmpty()) {
                ad = createdAds.get(0);
                totalAdsCreated = createdAds.size();
            }
        }

        if (ad == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tìm thấy quảng cáo nào sau khi lưu biến thể");
        }

        List<AdGenerationResponse.AdVariation> variations = contents.stream()
                .map(content -> convertToAdVariation(content, languageCode))
                .toList();

        AdGenerationResponse.ValidationReport validationReport = buildValidationReport(contents);

        String successMessage = totalAdsCreated > 1
                ? String.format("Đã tạo %d quảng cáo từ các biến thể đã chọn", totalAdsCreated)
                : "Đã lưu quảng cáo cải thiện thành công";

        return AdGenerationResponse.builder()
                .adId(ad.getId())
                .status("success")
                .message(successMessage)
                .variations(variations)
                .validationReport(validationReport)
                .build();
    }

    private void validateImprovementRequest(AdImprovementRequest request) {
        boolean hasProduct = StringUtils.hasText(request.getProductDescription());
        boolean hasReference = StringUtils.hasText(request.getReferenceContent())
                || StringUtils.hasText(request.getReferenceLink())
                || request.getReferenceAdData() != null;

        if (!hasProduct && !hasReference) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Vui lòng cung cấp nội dung sản phẩm hoặc link quảng cáo tham chiếu");
        }
    }

    private String determineTextProvider(AdImprovementRequest request) {
        return StringUtils.hasText(request.getTextProvider()) ? request.getTextProvider() : "openai";
    }

    private String determineImageProvider(AdImprovementRequest request) {
        if (request.getVariations() != null && !request.getVariations().isEmpty()) {
            return request.getVariations().stream()
                    .map(AdGenerationRequest.VariationProviderConfig::getImageProvider)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(request.getImageProvider());
        }
        return request.getImageProvider();
    }

    private String buildPromptFromRequest(AdImprovementRequest request, Language language) {
        String referenceContent = StringUtils.hasText(request.getReferenceContent())
                ? request.getReferenceContent()
                : "";
        String productContext = StringUtils.hasText(request.getProductDescription())
                ? request.getProductDescription()
                : "";
        Integer sentenceCount = null;
        Integer wordCount = null;
        Boolean containsCTA = null;
        Boolean containsPrice = null;
        ReferenceAnalysisResponse.ReferenceInsights referenceInsights = null;
        if (request.getReferenceInsights() != null) {
            sentenceCount = request.getReferenceInsights().getSentenceCount();
            wordCount = request.getReferenceInsights().getWordCount();
            containsCTA = request.getReferenceInsights().getContainsCallToAction();
            containsPrice = request.getReferenceInsights().getContainsPrice();
        } else if (StringUtils.hasText(referenceContent)) {
            referenceInsights = buildInsights(referenceContent);
            sentenceCount = referenceInsights.getSentenceCount();
            wordCount = referenceInsights.getWordCount();
            containsCTA = referenceInsights.isContainsCallToAction();
            containsPrice = referenceInsights.isContainsPrice();
        }

        FacebookCTA inferredCTA = request.getCallToAction();
        if (inferredCTA == null && StringUtils.hasText(referenceContent)) {
            inferredCTA = detectCallToAction(referenceContent);
        }

        ReferenceStyleProfile styleProfile = request.getReferenceStyle();
        if (styleProfile == null && StringUtils.hasText(referenceContent)) {
            styleProfile = analyzeStyleProfile(referenceContent, inferredCTA);
        }
        int variations = request.getNumberOfVariations() != null ? request.getNumberOfVariations() : 3;
        String languageLabel = language == Language.VIETNAMESE ? "Vietnamese" : "English";

        StringBuilder prompt = new StringBuilder();
        prompt.append("Write ").append(variations).append(" ad variations in ").append(languageLabel).append(". ");
        prompt.append("Reuse the reference ad's wording as closely as possible while replacing the product with the new context. ");
        prompt.append("Keep tone, structure, line breaks, punctuation, emoji usage, and formatting nearly identical to the reference. ");
        prompt.append("Do not enforce character limits; match the reference lengths naturally. ");
        if (inferredCTA != null) {
            prompt.append("Use CTA: ").append(inferredCTA.name()).append(". ");
        }
        if (sentenceCount != null && sentenceCount > 0) {
            prompt.append("Target roughly ").append(sentenceCount)
                    .append(" sentences per variation. ");
        }
        if (wordCount != null && wordCount > 0) {
            prompt.append("Match the reference word count (~").append(wordCount)
                    .append(" words). ");
        }
        if (Boolean.TRUE.equals(containsCTA)) {
            prompt.append("Ensure a CTA line is present and positioned similar to the reference. ");
        }
        if (Boolean.TRUE.equals(containsPrice)) {
            prompt.append("Preserve price/offer mentions if they appear in the reference (replace values accordingly). ");
        }
        if (styleProfile != null) {
            if (StringUtils.hasText(styleProfile.getPacing())) {
                prompt.append("Pacing: ").append(styleProfile.getPacing()).append(". ");
            }
            if (StringUtils.hasText(styleProfile.getHookType())) {
                prompt.append("Hook: ").append(styleProfile.getHookType()).append(". ");
            }
            if (StringUtils.hasText(styleProfile.getTone())) {
                prompt.append("Tone: ").append(styleProfile.getTone()).append(". ");
            }
            if (styleProfile.getUsesEmoji() != null) {
                prompt.append(styleProfile.getUsesEmoji() ? "Use emojis similar to the reference. " : "Avoid emojis if the reference avoids them. ");
            }
            if (styleProfile.getUsesQuestions() != null && styleProfile.getUsesQuestions()) {
                prompt.append("Include questions if the reference uses them. ");
            }
            if (styleProfile.getPunctuation() != null && !styleProfile.getPunctuation().isEmpty()) {
                prompt.append("Mimic punctuation patterns: ")
                        .append(String.join(", ", styleProfile.getPunctuation()))
                        .append(". ");
            }
            if (styleProfile.getStyleNotes() != null && !styleProfile.getStyleNotes().isEmpty()) {
                prompt.append("Style cues: ").append(String.join("; ", styleProfile.getStyleNotes())).append(". ");
            }
        }

        if (StringUtils.hasText(referenceContent)) {
            prompt.append("\nReference ad:\n").append(referenceContent);
        } else if (StringUtils.hasText(productContext)) {
            prompt.append("\nReference ad:\n").append(productContext);
        }

        if (StringUtils.hasText(productContext)) {
            prompt.append("\n\nProduct context (replace the reference product with this):\n")
                    .append(productContext);
        }

        prompt.append("\n\nReturn only valid JSON with fields: ")
                .append("headline, description, primaryText. ")
                .append("No extra text, no markdown.");

        if (prompt.length() > 3000) {
            log.warn("Prompt length {} chars exceeds 3000; consider trimming reference content", prompt.length());
        }
        return prompt.toString();
    }

    private Language resolveLanguageEnum(String requestedLanguage) {
        if ("vi".equalsIgnoreCase(requestedLanguage) || "vietnamese".equalsIgnoreCase(requestedLanguage)) {
            return Language.VIETNAMESE;
        }
        return Language.ENGLISH;
    }

    private String toLanguageCode(Language language) {
        return language == Language.VIETNAMESE ? "vi" : "en";
    }

    private AdGenerationResponse.AdVariation convertToAdVariation(AdContent content, String languageCode) {
        List<String> warnings = null;
        if (StringUtils.hasText(content.getValidationWarnings())) {
            try {
                warnings = objectMapper.readValue(content.getValidationWarnings(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                warnings = List.of(content.getValidationWarnings());
            }
        }

        String callToActionLabel = null;
        if (content.getCallToAction() != null) {
            callToActionLabel = com.fbadsautomation.util.CTAMapper.getDisplayLabel(
                    content.getCallToAction(),
                    languageCode
            );
        }

        AdGenerationResponse.QualityDetails qualityDetails = qualityDetailsMapper.buildDetails(content);
        Integer summarizedScore = qualityDetails != null
                ? (int) Math.round(qualityDetails.getTotalScore())
                : content.getQualityScore();

        return AdGenerationResponse.AdVariation.builder()
                .id(content.getId())
                .headline(content.getHeadline())
                .description(content.getDescription())
                .primaryText(content.getPrimaryText())
                .callToAction(content.getCallToAction() != null ? content.getCallToAction().name() : null)
                .callToActionLabel(callToActionLabel)
                .imageUrl(content.getImageUrl())
                .order(content.getPreviewOrder())
                .qualityScore(summarizedScore)
                .hasWarnings(content.getHasWarnings())
                .warnings(warnings)
                .qualityDetails(qualityDetails)
                .build();
    }

    private AdGenerationResponse.ValidationReport buildValidationReport(List<AdContent> contents) {
        int total = contents.size();
        int passed = (int) contents.stream()
                .filter(c -> c.getHasWarnings() == null || !c.getHasWarnings())
                .count();
        int withWarnings = (int) contents.stream()
                .filter(c -> c.getHasWarnings() != null && c.getHasWarnings())
                .count();
        int failed = total - passed - withWarnings;

        double avgScore = contents.stream()
                .filter(c -> c.getQualityScore() != null)
                .mapToInt(AdContent::getQualityScore)
                .average()
                .orElse(0.0);

        return new AdGenerationResponse.ValidationReport(total, passed, failed, withWarnings, avgScore);
    }

    private ReferenceAnalysisResponse.ReferenceInsights buildInsights(String referenceContent) {
        if (!StringUtils.hasText(referenceContent)) {
            return ReferenceAnalysisResponse.ReferenceInsights.builder()
                    .wordCount(0)
                    .sentenceCount(0)
                    .containsCallToAction(false)
                    .containsPrice(false)
                    .build();
        }
        String normalized = referenceContent.replaceAll("\\s+", " ").trim();
        int wordCount = normalized.isEmpty() ? 0 : normalized.split(" ").length;
        int sentenceCount = normalized.split("[.!?\\n]").length;
        boolean containsCTA = detectCallToAction(referenceContent) != null;
        boolean containsPrice = PRICE_PATTERN.matcher(referenceContent.toLowerCase()).find();

        return ReferenceAnalysisResponse.ReferenceInsights.builder()
                .wordCount(wordCount)
                .sentenceCount(sentenceCount)
                .containsCallToAction(containsCTA)
                .containsPrice(containsPrice)
                .build();
    }

    private String detectStyle(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String lower = content.toLowerCase();
        int bestScore = 0;
        String bestStyle = null;
        for (Map.Entry<String, List<String>> entry : STYLE_KEYWORDS.entrySet()) {
            int score = countMatches(lower, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestStyle = entry.getKey();
            }
        }
        return bestScore > 0 ? bestStyle : null;
    }

    private FacebookCTA detectCallToAction(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String lower = content.toLowerCase();
        int bestScore = 0;
        FacebookCTA bestCTA = null;
        for (Map.Entry<FacebookCTA, List<String>> entry : CTA_KEYWORDS.entrySet()) {
            int score = countMatches(lower, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestCTA = entry.getKey();
            }
        }
        return bestCTA;
    }

    private ReferenceStyleProfile analyzeStyleProfile(String content, FacebookCTA detectedCTA) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalized = content.trim();
        String firstSentence = extractFirstSentence(normalized);
        String hookType = classifyHook(firstSentence);
        String tone = classifyTone(normalized);
        String pacing = determinePacing(normalized);
        boolean usesEmoji = containsEmoji(normalized);
        boolean usesQuestions = normalized.contains("?");
        boolean usesSecondPerson = containsSecondPerson(normalized);
        List<String> emojiSamples = usesEmoji ? extractEmojiSamples(normalized) : Collections.emptyList();
        List<String> styleNotes = deriveStyleNotes(normalized, usesEmoji, usesQuestions);
        List<String> punctuation = detectPunctuationPatterns(normalized);
        String ctaVerb = detectedCTA != null ? describeCTA(detectedCTA) : detectCTAFromText(normalized);

        return ReferenceStyleProfile.builder()
                .hookType(hookType)
                .tone(tone)
                .pacing(pacing)
                .usesEmoji(usesEmoji)
                .usesQuestions(usesQuestions)
                .usesSecondPerson(usesSecondPerson)
                .emojiSamples(emojiSamples.isEmpty() ? null : emojiSamples)
                .styleNotes(styleNotes.isEmpty() ? null : styleNotes)
                .punctuation(punctuation.isEmpty() ? null : punctuation)
                .ctaVerb(ctaVerb)
                .build();
    }

    private String extractFirstSentence(String text) {
        String[] split = text.split("[\\n]|(?<=[.!?])\\s+");
        return split.length > 0 ? split[0] : text;
    }

    private String classifyHook(String firstSentence) {
        if (!StringUtils.hasText(firstSentence)) {
            return "STATEMENT";
        }
        String lowered = firstSentence.trim();
        if (lowered.endsWith("?")) {
            return "QUESTION";
        }
        if (lowered.contains("!")) {
            return "EXCLAMATION";
        }
        if (lowered.matches("^(\\d+|[0-9]+%|save|giảm|chỉ trong|only).*")) {
            return "PROMOTION";
        }
        if (lowered.matches("(?i).*(imagine|khi bạn|hãy thử|once).*")) {
            return "STORY";
        }
        return "STATEMENT";
    }

    private String classifyTone(String content) {
        String lower = content.toLowerCase();
        for (Map.Entry<String, List<String>> entry : TONE_KEYWORD_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return "BALANCED";
    }

    private String determinePacing(String content) {
        if (!StringUtils.hasText(content)) {
            return "BALANCED";
        }
        String[] sentences = content.split("[.!?\\n]+");
        if (sentences.length == 0) {
            return "BALANCED";
        }
        int totalWords = Arrays.stream(sentences)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(s -> s.split("\\s+").length)
                .sum();
        if (totalWords == 0) {
            return "BALANCED";
        }
        double avg = (double) totalWords / sentences.length;
        if (avg < 10) {
            return "SNAPPY";
        }
        if (avg > 22) {
            return "STORYTELLING";
        }
        return "BALANCED";
    }

    private boolean containsEmoji(String text) {
        return text.codePoints().anyMatch(this::isEmojiCodePoint);
    }

    private List<String> extractEmojiSamples(String text) {
        return text.codePoints()
                .filter(this::isEmojiCodePoint)
                .mapToObj(cp -> new String(Character.toChars(cp)))
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }

    private boolean isEmojiCodePoint(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == null) {
            return false;
        }
        return Character.getType(codePoint) == Character.OTHER_SYMBOL
                || block == Character.UnicodeBlock.EMOTICONS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS
                || block == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
                || block == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
                || block == Character.UnicodeBlock.DINGBATS;
    }

    private boolean containsSecondPerson(String content) {
        String lower = content.toLowerCase();
        return SECOND_PERSON_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private List<String> deriveStyleNotes(String content, boolean usesEmoji, boolean usesQuestions) {
        List<String> notes = new ArrayList<>();
        if (usesEmoji) {
            notes.add("Uses emoji as inline separators");
        }
        if (usesQuestions) {
            notes.add("Opens curiosity gaps with questions");
        }
        if (content.lines().filter(line -> line.trim().startsWith("-") || line.trim().startsWith("•")).count() >= 2) {
            notes.add("Relies on bullet/line breaks for scannability");
        }
        long uppercaseWords = Arrays.stream(content.split("\\s+"))
                .filter(word -> word.length() > 3 && word.equals(word.toUpperCase()))
                .count();
        if (uppercaseWords >= 3) {
            notes.add("Emphasizes key benefits with uppercase words");
        }
        if (content.contains("...")) {
            notes.add("Uses ellipsis for dramatic pauses");
        }
        return notes;
    }

    private List<String> detectPunctuationPatterns(String content) {
        List<String> cues = new ArrayList<>();
        long exclamations = content.chars().filter(ch -> ch == '!').count();
        if (exclamations >= 2) {
            cues.add("Multiple exclamation points");
        }
        long questions = content.chars().filter(ch -> ch == '?').count();
        if (questions >= 2) {
            cues.add("Question-heavy copy");
        }
        if (content.contains("...")) {
            cues.add("Ellipsis pacing");
        }
        return cues;
    }

    private String describeCTA(FacebookCTA cta) {
        if (cta == null) {
            return null;
        }
        return switch (cta) {
            case SHOP_NOW -> "Encourage immediate purchase";
            case LEARN_MORE -> "Invite readers to learn more";
            case SIGN_UP -> "Invite sign-up";
            case APPLY_NOW -> "Prompt to apply";
            case DOWNLOAD -> "Encourage download";
            case GET_OFFER -> "Highlight limited offer";
            default -> cta.name();
        };
    }

    private String detectCTAFromText(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String lower = content.toLowerCase();
        if (lower.contains("đăng ký ngay") || lower.contains("sign up")) {
            return "Invite sign-up";
        }
        if (lower.contains("mua ngay") || lower.contains("shop now")) {
            return "Encourage purchase";
        }
        if (lower.contains("tìm hiểu thêm") || lower.contains("learn more")) {
            return "Invite readers to learn more";
        }
        if (lower.contains("đặt lịch") || lower.contains("book now")) {
            return "Prompt to book";
        }
        return null;
    }

    private int countMatches(String content, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            int index = content.indexOf(keyword);
            while (index >= 0) {
                score++;
                index = content.indexOf(keyword, index + keyword.length());
            }
        }
        return score;
    }

}
