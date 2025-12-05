package com.fbadsautomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.dto.AdCopyReviewDTO;
import com.fbadsautomation.dto.AdCopyRewriteRequest;
import com.fbadsautomation.dto.AdCopyRewriteResponse;
import com.fbadsautomation.dto.AdOptimizationAnalyzeRequest;
import com.fbadsautomation.dto.AdOptimizationInsightDTO;
import com.fbadsautomation.dto.AdOptimizationInsightDTO.LengthMetrics;
import com.fbadsautomation.dto.AdOptimizationInsightDTO.Scorecard;
import com.fbadsautomation.dto.AdOptimizationSnapshotDTO;
import com.fbadsautomation.dto.SaveAdOptimizationInsightRequest;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.AdOptimizationSnapshot;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.AdOptimizationSnapshotRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.repository.UserRepository;
import com.fbadsautomation.model.Persona;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdOptimizationInsightService {

    private static final Logger log = LoggerFactory.getLogger(AdOptimizationInsightService.class);

    private static final int IDEAL_HEADLINE_MAX = 40;
    private static final int IDEAL_DESCRIPTION_MAX = 120;
    private static final int IDEAL_PRIMARY_TEXT_MAX = 280;
    private static final Locale EN_LOCALE = Locale.US;
    private static final Locale VI_LOCALE = new Locale("vi", "VN");
    private static final Pattern VIETNAMESE_CHAR_PATTERN = Pattern.compile("[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final AdOptimizationSnapshotRepository snapshotRepository;
    private final AdQualityScoringService adQualityScoringService;
    private final AdCopyReviewService adCopyReviewService;
    private final ObjectMapper objectMapper;

    @Value("${app.optimization.default-language:en}")
    private String defaultLanguage;

    @Autowired
    public AdOptimizationInsightService(UserRepository userRepository,
                                        AdRepository adRepository,
                                        AdOptimizationSnapshotRepository snapshotRepository,
                                        AdQualityScoringService adQualityScoringService,
                                        AdCopyReviewService adCopyReviewService,
                                        ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.adRepository = adRepository;
        this.snapshotRepository = snapshotRepository;
        this.adQualityScoringService = adQualityScoringService;
        this.adCopyReviewService = adCopyReviewService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdOptimizationInsightDTO> analyzeAds(Long userId, AdOptimizationAnalyzeRequest request) {
        List<Long> adIds = Optional.ofNullable(request.getAdIds()).orElse(Collections.emptyList());
        if (adIds.isEmpty()) {
            throw new IllegalArgumentException("adIds must not be empty");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String requestedLanguage = resolveLanguage(request.getLanguage());
        List<Ad> ads = adRepository.findByUserAndIdInWithCampaign(user, adIds);
        if (ads.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Ad> adMap = ads.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Ad::getId, Function.identity(), (left, right) -> left));

        return adIds.stream()
            .map(adMap::get)
            .filter(Objects::nonNull)
            .map(ad -> buildInsight(ad, user, requestedLanguage))
            .collect(Collectors.toList());
    }

    @Transactional
    public AdOptimizationSnapshotDTO saveSnapshot(Long userId, SaveAdOptimizationInsightRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getAdId() == null) {
            throw new IllegalArgumentException("adId is required");
        }
        if (request.getSuggestions() == null || request.getSuggestions().isEmpty()) {
            throw new IllegalArgumentException("suggestions are required");
        }

        AdOptimizationSnapshot snapshot = new AdOptimizationSnapshot();
        snapshot.setUser(user);
        snapshot.setAdId(request.getAdId());
        snapshot.setAdName(request.getAdName());
        snapshot.setCampaignName(request.getCampaignName());
        snapshot.setLanguage(resolveLanguage(request.getLanguage()));
        snapshot.setCreatedAt(LocalDateTime.now());

        try {
            snapshot.setSuggestionsJson(objectMapper.writeValueAsString(request.getSuggestions()));
            snapshot.setScorecardJson(objectMapper.writeValueAsString(request.getScorecard()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to store snapshot payload", e);
        }

        AdOptimizationSnapshot saved = snapshotRepository.save(snapshot);
        return toSnapshotDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdOptimizationSnapshotDTO> history(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return snapshotRepository.findByUserOrderByCreatedAtDesc(user, pageable)
            .map(this::toSnapshotDTO);
    }

    // ----------------------------------------------------------------------
    // Mapping helpers
    // ----------------------------------------------------------------------

    private AdOptimizationInsightDTO buildInsight(Ad ad,
                                                  User user,
                                                  String requestedLanguage) {
        AdOptimizationInsightDTO dto = new AdOptimizationInsightDTO();
        dto.setAdId(ad.getId());
        dto.setAdName(Optional.ofNullable(ad.getName()).orElse("Untitled Ad"));
        dto.setCampaignName(ad.getCampaign() != null ? ad.getCampaign().getName() : null);
        dto.setAdType(ad.getAdType() != null ? ad.getAdType().name() : null);
        dto.setCallToAction(ad.getCallToAction() != null ? ad.getCallToAction().name() : null);
        dto.setHasImage(hasMedia(ad));
        dto.setCreatedDate(ad.getCreatedDate());

        LengthMetrics lengths = new LengthMetrics(
            optionalLength(ad.getHeadline()),
            optionalLength(ad.getDescription()),
            optionalLength(ad.getPrimaryText())
        );
        dto.setLengthMetrics(lengths);

        String analysisLanguage = detectAdLanguage(ad, requestedLanguage);
        dto.setLanguage(analysisLanguage);

        AdQualityScoringService.AdQualityScore score = adQualityScoringService.calculateQualityScore(asAdContent(ad));
        dto.setScorecard(toScorecard(score));

        Map<String, List<String>> suggestionBuckets = new LinkedHashMap<>();
        enrichCopySuggestions(ad, suggestionBuckets, analysisLanguage, lengths);
        enrichVisualSuggestions(ad, suggestionBuckets, analysisLanguage);
        enrichCtaSuggestions(ad, suggestionBuckets, analysisLanguage);
        enrichHookSuggestions(ad, suggestionBuckets, analysisLanguage);
        enrichLengthSuggestions(lengths, suggestionBuckets, analysisLanguage);
        enrichTargetSuggestions(ad, suggestionBuckets, analysisLanguage);
        ensureFallbacks(suggestionBuckets, analysisLanguage);

        dto.setSuggestions(suggestionBuckets);
        dto.setSaved(snapshotRepository.existsByUserAndAdId(user, ad.getId()));

        Persona adPersona = ad.getPersona();
        dto.setPersona(toPersonaContext(adPersona));

        Long personaId = adPersona != null ? adPersona.getId() : null;
        adCopyReviewService.reviewAdCopy(ad, user, adPersona, personaId, null, analysisLanguage)
            .ifPresent(dto::setCopyReview);

        return dto;
    }

    private Scorecard toScorecard(AdQualityScoringService.AdQualityScore score) {
        Scorecard sc = new Scorecard();
        sc.setCompliance(score.getComplianceScore());
        sc.setLinguistic(score.getLinguisticScore());
        sc.setPersuasiveness(score.getPersuasivenessScore());
        sc.setCompleteness(score.getCompletenessScore());
        sc.setTotal(score.getTotalScore());
        sc.setGrade(score.getGrade());
        return sc;
    }

    private AdContent asAdContent(Ad ad) {
        AdContent content = new AdContent();
        content.setHeadline(ad.getHeadline());
        content.setDescription(ad.getDescription());
        content.setPrimaryText(ad.getPrimaryText());
        content.setCallToAction(ad.getCallToAction());
        return content;
    }

    private void enrichCopySuggestions(Ad ad, Map<String, List<String>> buckets, String language, LengthMetrics lengths) {
        if (lengths.getPrimaryTextLength() > IDEAL_PRIMARY_TEXT_MAX) {
            addSuggestion(buckets, "copy", localize(language,
                "Your primary text is %d characters. Break it into shorter lines + emojis to improve scannability.",
                "Nội dung chính hiện dài %d ký tự. Chia nhỏ thành nhiều câu/ngắt dòng để dễ đọc hơn.",
                lengths.getPrimaryTextLength()));
        }

        if (isNullOrBlank(ad.getPrimaryText())) {
            addSuggestion(buckets, "copy", localize(language,
                "Add a clear value proposition in the primary text so readers know why they should care.",
                "Bổ sung giá trị chính trong nội dung để người đọc hiểu ngay lợi ích."));
        }

        if (!containsNumberOrSymbol(ad.getHeadline()) && !containsNumberOrSymbol(ad.getPrimaryText())) {
            addSuggestion(buckets, "copy", localize(language,
                "Introduce concrete proof (numbers, social proof or offer details) to make the copy more convincing.",
                "Thêm minh chứng cụ thể (con số, phản hồi khách hàng, ưu đãi) để nội dung thuyết phục hơn."));
        }
    }

    private void enrichVisualSuggestions(Ad ad, Map<String, List<String>> buckets, String language) {
        if (!hasMedia(ad)) {
            addSuggestion(buckets, "visual", localize(language,
                "Add a branded image or short video so the ad doesn't rely on text only.",
                "Thêm hình ảnh hoặc video mang dấu ấn thương hiệu để quảng cáo không chỉ toàn chữ."));
        } else {
            addSuggestion(buckets, "visual", localize(language,
                "Overlay the key promise or CTA on the creative to make it readable at a glance.",
                "Đặt thông điệp/CTA chính ngay trên hình ảnh để người xem nắm ý nhanh."));
        }
    }

    private void enrichCtaSuggestions(Ad ad, Map<String, List<String>> buckets, String language) {
        if (ad.getCallToAction() == null) {
            addSuggestion(buckets, "cta", localize(language,
                "Pick a CTA (e.g. LEARN_MORE / SIGN_UP) so Meta can optimize delivery for that action.",
                "Chọn CTA cụ thể (LEARN_MORE / SIGN_UP...) để Meta tối ưu phân phối cho hành động đó."));
        } else {
            addSuggestion(buckets, "cta", localize(language,
                "Test 1-2 alternative CTAs to keep the experience fresh (current: %s).",
                "Thử thêm 1-2 CTA khác để trải nghiệm mới mẻ hơn (đang dùng: %s).",
                ad.getCallToAction().name()));
        }
    }

    private void enrichHookSuggestions(Ad ad, Map<String, List<String>> buckets, String language) {
        if (optionalLength(ad.getHeadline()) < 15) {
            addSuggestion(buckets, "hook", localize(language,
                "Add a sharper hook in the headline (question, bold claim or pain point).",
                "Thêm hook rõ ràng ở tiêu đề (đặt câu hỏi, nêu vấn đề hoặc cam kết nổi bật)."));
        } else if (optionalLength(ad.getHeadline()) > IDEAL_HEADLINE_MAX) {
            addSuggestion(buckets, "hook", localize(language,
                "Headline is %d chars. Trim it below %d so it doesn't truncate on mobile.",
                "Tiêu đề dài %d ký tự. Rút gọn dưới %d ký tự để tránh bị cắt trên mobile.",
                optionalLength(ad.getHeadline()), IDEAL_HEADLINE_MAX));
        }
    }

    private void enrichLengthSuggestions(LengthMetrics lengths, Map<String, List<String>> buckets, String language) {
        addSuggestion(buckets, "length", localize(language,
            "Headline: %d / %d chars • Description: %d / %d • Primary: %d / %d.",
            "Tiêu đề: %d / %d ký tự • Mô tả: %d / %d • Nội dung chính: %d / %d.",
            lengths.getHeadlineLength(), IDEAL_HEADLINE_MAX,
            lengths.getDescriptionLength(), IDEAL_DESCRIPTION_MAX,
            lengths.getPrimaryTextLength(), IDEAL_PRIMARY_TEXT_MAX));
    }

    private void enrichTargetSuggestions(Ad ad, Map<String, List<String>> buckets, String language) {
        String audience = ad.getCampaign() != null ? ad.getCampaign().getTargetAudience() : null;
        if (audience == null || audience.isBlank()) {
            addSuggestion(buckets, "target", localize(language,
                "Specify a persona or audience insight so AI copies can mirror exact pains.",
                "Nêu rõ chân dung khách hàng hoặc insight để AI bám sát vấn đề thực tế hơn."));
        } else {
            addSuggestion(buckets, "target", localize(language,
                "Make sure the copy mirrors this audience insight: \"%s\".",
                "Nhắc lại insight khách hàng trong nội dung: \"%s\".",
                audience.length() > 120 ? audience.substring(0, 117) + "..." : audience));
        }
    }

    private void ensureFallbacks(Map<String, List<String>> buckets, String language) {
        List<String> categories = List.of("copy", "visual", "cta", "hook", "length", "target");
        for (String category : categories) {
            if (!buckets.containsKey(category) || buckets.get(category).isEmpty()) {
                addSuggestion(buckets, category, localize(language,
                    "Looks good here—keep an eye on performance after publishing.",
                    "Khu vực này ổn rồi, hãy theo dõi hiệu quả sau khi chạy."));
            }
        }
    }

    private String resolveLanguage(String language) {
        String lang = Optional.ofNullable(language).filter(l -> !l.isBlank()).orElse(defaultLanguage);
        return lang.toLowerCase().startsWith("vi") ? "vi" : "en";
    }

    public Optional<AdCopyRewriteResponse> rewriteSection(Long userId,
                                                          Long adId,
                                                          AdCopyRewriteRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Ad ad = adRepository.findByIdAndUserWithRelations(adId, user)
            .orElseThrow(() -> new RuntimeException("Ad not found or not owned by user"));

        String fallbackLanguage = resolveLanguage(request.getLanguage());
        String language = detectAdLanguage(ad, fallbackLanguage);
        Persona persona = ad.getPersona();
        Long personaId = persona != null ? persona.getId() : null;
        return adCopyReviewService.rewriteSection(
                ad,
                user,
                persona,
                personaId,
                null,
                language,
                request.getSection(),
                request.getAdditionalGuidance()
            )
            .map(text -> new AdCopyRewriteResponse(request.getSection(), text));
    }

    private Locale resolveLocale(String language) {
        return "vi".equals(language) ? VI_LOCALE : EN_LOCALE;
    }

    private String localize(String language, String enTemplate, String viTemplate, Object... args) {
        Locale locale = resolveLocale(language);
        String template = "vi".equals(language) ? viTemplate : enTemplate;
        return String.format(locale, template, args);
    }

    private boolean hasMedia(Ad ad) {
        return (ad.getImageUrl() != null && !ad.getImageUrl().isBlank()) ||
               (ad.getVideoUrl() != null && !ad.getVideoUrl().isBlank());
    }

    private int optionalLength(String value) {
        return value == null ? 0 : value.length();
    }

    private boolean containsNumberOrSymbol(String value) {
        if (value == null) return false;
        return value.chars().anyMatch(ch -> Character.isDigit(ch) || ch == '%' || ch == '$');
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addSuggestion(Map<String, List<String>> buckets, String category, String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return;
        }
        List<String> list = buckets.computeIfAbsent(category, key -> new ArrayList<>());
        if (list.size() >= 3) {
            return;
        }
        if (!list.contains(suggestion)) {
            list.add(suggestion);
        }
    }

    private AdOptimizationSnapshotDTO toSnapshotDTO(AdOptimizationSnapshot snapshot) {
        AdOptimizationSnapshotDTO dto = new AdOptimizationSnapshotDTO();
        dto.setId(snapshot.getId());
        dto.setAdId(snapshot.getAdId());
        dto.setAdName(snapshot.getAdName());
        dto.setCampaignName(snapshot.getCampaignName());
        dto.setLanguage(snapshot.getLanguage());
        dto.setCreatedAt(snapshot.getCreatedAt());
        dto.setSuggestions(readSuggestions(snapshot.getSuggestionsJson()));
        dto.setScorecard(readScorecard(snapshot.getScorecardJson()));
        return dto;
    }

    private AdOptimizationInsightDTO.PersonaContext toPersonaContext(Persona persona) {
        if (persona == null) {
            return null;
        }
        AdOptimizationInsightDTO.PersonaContext ctx = new AdOptimizationInsightDTO.PersonaContext();
        ctx.setId(persona.getId());
        ctx.setName(persona.getName());
        ctx.setAge(persona.getAge());
        ctx.setGender(persona.getGender() != null ? persona.getGender().name() : null);
        ctx.setTone(persona.getTone());
        ctx.setInterests(persona.getInterests() != null ? new ArrayList<>(persona.getInterests()) : List.of());
        ctx.setPainPoints(persona.getPainPoints() != null ? new ArrayList<>(persona.getPainPoints()) : List.of());
        ctx.setDesiredOutcome(persona.getDesiredOutcome());
        ctx.setDescription(persona.getDescription());
        return ctx;
    }

    private String detectAdLanguage(Ad ad, String fallbackLanguage) {
        String normalizedFallback = resolveLanguage(fallbackLanguage);
        String combined = Stream.of(ad.getHeadline(), ad.getDescription(), ad.getPrimaryText())
            .filter(text -> text != null && !text.isBlank())
            .map(text -> text.toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(" "));
        if (!combined.isBlank() && VIETNAMESE_CHAR_PATTERN.matcher(combined).find()) {
            return "vi";
        }
        return normalizedFallback;
    }

    private Map<String, List<String>> readSuggestions(String json) {
        if (json == null) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse suggestions json", e);
            return Collections.emptyMap();
        }
    }

    private Scorecard readScorecard(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Scorecard.class);
        } catch (Exception e) {
            log.error("Failed to parse scorecard json", e);
            return null;
        }
    }
}
