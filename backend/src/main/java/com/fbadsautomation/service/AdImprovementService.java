package com.fbadsautomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.AdGenerationRequest;
import com.fbadsautomation.dto.AdGenerationResponse;
import com.fbadsautomation.dto.AdImprovementRequest;
import com.fbadsautomation.dto.ReferenceAdData;
import com.fbadsautomation.dto.ReferenceAnalysisRequest;
import com.fbadsautomation.dto.ReferenceAnalysisResponse;
import com.fbadsautomation.exception.ApiException;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdStyle;
import com.fbadsautomation.model.AdType;
import com.fbadsautomation.model.AsyncJobStatus;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.util.ValidationMessages.Language;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final ChainOfThoughtPromptBuilder chainOfThoughtPromptBuilder;
    private final AdService adService;
    private final AsyncJobService asyncJobService;
    private final AsyncAIContentService asyncAIContentService;
    private final ObjectMapper objectMapper;

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
                .build();
    }

    /**
     * Sinh quảng cáo mới dựa trên nội dung người dùng và phong cách tham chiếu.
     */
    public AdGenerationResponse generateImprovedAds(AdImprovementRequest request, Long userId) {
        validateImprovementRequest(request);

        String finalLanguage = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "vi";
        String mergedPrompt = buildPromptFromRequest(request);
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
                request.getVariations());

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

        String prompt = buildPromptFromRequest(request);
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
        String safeLanguage = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "vi";
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
                request.getVariations()
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
                    request.getAdStyle());
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
                    request.getAdStyle());
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

    private String buildPromptFromRequest(AdImprovementRequest request) {
        AdType adType = adService.mapFrontendAdTypeToEnum(request.getAdType());
        Language language = "vi".equalsIgnoreCase(request.getLanguage())
                ? Language.VIETNAMESE
                : Language.ENGLISH;
        AdStyle adStyle = parseAdStyle(request.getCreativeStyle());
        boolean enforceCharacterLimits = !Boolean.TRUE.equals(request.getAllowUnlimitedLength());

        ChainOfThoughtPromptBuilder.ReferenceMetrics referenceMetrics = buildReferenceMetrics(request);

        return chainOfThoughtPromptBuilder.buildCoTPrompt(
                StringUtils.hasText(request.getProductDescription()) ? request.getProductDescription() : "N/A",
                null,
                adStyle,
                null,
                request.getTrendingKeywords(),
                language,
                request.getCallToAction(),
                adType,
                request.getNumberOfVariations(),
                request.getReferenceContent(),
                request.getReferenceLink(),
                enforceCharacterLimits,
                referenceMetrics
        );
    }

    private ChainOfThoughtPromptBuilder.ReferenceMetrics buildReferenceMetrics(AdImprovementRequest request) {
        Integer wordCount = null;
        Integer sentenceCount = null;
        Boolean containsCTA = null;
        Boolean containsPrice = null;

        if (request.getReferenceInsights() != null) {
            AdImprovementRequest.ReferenceInsights insights = request.getReferenceInsights();
            wordCount = insights.getWordCount();
            sentenceCount = insights.getSentenceCount();
            containsCTA = insights.getContainsCallToAction();
            containsPrice = insights.getContainsPrice();
        } else if (StringUtils.hasText(request.getReferenceContent())) {
            ReferenceAnalysisResponse.ReferenceInsights computedInsights = buildInsights(request.getReferenceContent());
            wordCount = computedInsights.getWordCount();
            sentenceCount = computedInsights.getSentenceCount();
            containsCTA = computedInsights.isContainsCallToAction();
            containsPrice = computedInsights.isContainsPrice();
        }

        if (wordCount == null && sentenceCount == null && containsCTA == null && containsPrice == null) {
            return null;
        }

        return new ChainOfThoughtPromptBuilder.ReferenceMetrics(wordCount, sentenceCount, containsCTA, containsPrice);
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

        return AdGenerationResponse.AdVariation.builder()
                .id(content.getId())
                .headline(content.getHeadline())
                .description(content.getDescription())
                .primaryText(content.getPrimaryText())
                .callToAction(content.getCallToAction() != null ? content.getCallToAction().name() : null)
                .callToActionLabel(callToActionLabel)
                .imageUrl(content.getImageUrl())
                .order(content.getPreviewOrder())
                .qualityScore(content.getQualityScore())
                .hasWarnings(content.getHasWarnings())
                .warnings(warnings)
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

    private AdStyle parseAdStyle(String adStyle) {
        if (!StringUtils.hasText(adStyle)) {
            return null;
        }
        try {
            return AdStyle.valueOf(adStyle.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ad style provided: {}", adStyle);
            return null;
        }
    }
}
