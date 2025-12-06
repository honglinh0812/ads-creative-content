package com.fbadsautomation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbadsautomation.ai.AIProvider;
import com.fbadsautomation.dto.AdCopyReviewDTO;
import com.fbadsautomation.dto.AdCopyReviewDTO.SectionReview;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Persona;
import com.fbadsautomation.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdCopyReviewService {

    private static final String DEFAULT_SYSTEM_PROMPT = """
        You are an award-winning creative director specializing in paid social ads.
        Evaluate the provided ad copy honestly and return structured JSON feedback.
        Each section must include realistic rewrite suggestions that respect the persona's tone.
        """;

    private static final TypeReference<AdCopyReviewDTO> REVIEW_TYPE = new TypeReference<>() {};

    private final AIProviderService aiProviderService;
    private final PersonaService personaService;
    private final ObjectMapper objectMapper;

    public Optional<AdCopyReviewDTO> reviewAdCopy(Ad ad,
                                                  User user,
                                                  Persona persona,
                                                  Long personaId,
                                                  String personaBrief,
                                                  String languageCode) {
        AIProvider provider = aiProviderService.getProvider("openai");
        if (provider == null) {
            log.warn("OpenAI provider not available for ad copy review");
            return Optional.empty();
        }

        String personaSummary = buildPersonaSummary(user, persona, personaId, personaBrief, languageCode);
        String prompt = buildReviewPrompt(ad, personaSummary, languageCode);

        String raw = provider.generateTextCompletion(prompt, DEFAULT_SYSTEM_PROMPT, 900);
        if (!StringUtils.hasText(raw)) {
            log.warn("OpenAI returned empty review payload");
            return Optional.empty();
        }

        try {
            AdCopyReviewDTO dto = objectMapper.readValue(raw.trim(), REVIEW_TYPE);
            dto.setPersonaSummary(personaSummary);
            sanitizeSections(dto);
            return Optional.of(dto);
        } catch (Exception ex) {
            log.error("Failed to parse AI review JSON. Payload: {}", raw, ex);
            return Optional.empty();
        }
    }

    public Optional<String> rewriteSection(Ad ad,
                                           User user,
                                           Persona persona,
                                           Long personaId,
                                           String personaBrief,
                                           String languageCode,
                                           String sectionName,
                                           String additionalGuidance) {
        AIProvider provider = aiProviderService.getProvider("openai");
        if (provider == null) {
            return Optional.empty();
        }

        String personaSummary = buildPersonaSummary(user, persona, personaId, personaBrief, languageCode);
        String prompt = buildRewritePrompt(ad, personaSummary, sectionName, additionalGuidance, languageCode);
        String rewrite = provider.generateTextCompletion(prompt, DEFAULT_SYSTEM_PROMPT, 400);
        if (!StringUtils.hasText(rewrite)) {
            return Optional.empty();
        }
        return Optional.of(rewrite.trim());
    }

    private String buildReviewPrompt(Ad ad, String personaSummary, String languageCode) {
        Locale locale = resolveLocale(languageCode);
        boolean isVietnamese = "vi".equalsIgnoreCase(locale.getLanguage());
        String languageHint = isVietnamese
            ? "Trả lời toàn bộ bằng tiếng Việt. Các trường overallVerdict, strengths, improvements và rewrite phải bằng tiếng Việt – không được sử dụng tiếng Anh."
            : "Respond entirely in English. Fields overallVerdict, strengths, improvements, and rewrite must be written in English only.";

        return """
            %s

            Evaluate the following ad copy:
            - Headline: "%s"
            - Description: "%s"
            - Primary Text: "%s"

            Audience context:
            %s

            Respond strictly in JSON with this schema:
            {
              "overallScore": number 0-100,
              "overallVerdict": "short summary",
              "sections": [
                {
                  "section": "HEADLINE|DESCRIPTION|PRIMARY_TEXT",
                  "score": number 0-100,
                  "verdict": "one sentence verdict",
                  "strengths": ["bullet"],
                  "improvements": ["bullet"],
                  "rewrite": "improved copy for this section"
                }
              ]
            }
            """.formatted(languageHint,
                safe(ad.getHeadline()),
                safe(ad.getDescription()),
                safe(ad.getPrimaryText()),
                personaSummary);
    }

    private String buildRewritePrompt(Ad ad,
                                      String personaSummary,
                                      String section,
                                      String guidance,
                                      String languageCode) {
        Locale locale = resolveLocale(languageCode);
        boolean isVietnamese = "vi".equalsIgnoreCase(locale.getLanguage());
        String languageHint = isVietnamese
            ? "Viết câu trả lời bằng tiếng Việt. Không sử dụng tiếng Anh trong nội dung mới."
            : "Respond in English only. Do not use other languages in the rewritten text.";

        String currentText = switch (section != null ? section.toUpperCase(Locale.ROOT) : "") {
            case "HEADLINE" -> safe(ad.getHeadline());
            case "DESCRIPTION" -> safe(ad.getDescription());
            case "PRIMARY_TEXT" -> safe(ad.getPrimaryText());
            default -> safe(ad.getPrimaryText());
        };

        return """
            %s

            Rewrite the %s of this ad to better resonate with the persona.
            Current text: "%s"
            Persona context: %s
            Additional guidance: %s

            Return only the rewritten text without quotes or explanations.
            """.formatted(languageHint,
                section,
                currentText,
                personaSummary,
                safe(guidance));
    }

    private String buildPersonaSummary(User user,
                                       Persona persona,
                                       Long personaId,
                                       String personaBrief,
                                       String languageCode) {
        StringBuilder summary = new StringBuilder();
        if (StringUtils.hasText(personaBrief)) {
            summary.append(personaBrief.trim());
        }
        if (persona != null) {
            appendPersona(summary, persona, languageCode);
        } else if (personaId != null && user != null) {
            try {
                Optional<Persona> personaOpt = personaService.findByIdAndUser(personaId, user);
                personaOpt.ifPresent(p -> appendPersona(summary, p, languageCode));
            } catch (Exception ex) {
                log.warn("Failed to fetch persona {} for user {}: {}", personaId, user.getId(), ex.getMessage());
            }
        }
        if (summary.length() == 0) {
            summary.append("vi".equalsIgnoreCase(languageCode)
                ? "Khán giả phổ thông bận rộn trên mạng xã hội, thích các ưu đãi ngắn gọn và hữu ích."
                : "General audience of busy social media users looking for helpful offers.");
        }
        return summary.toString();
    }

    private void sanitizeSections(AdCopyReviewDTO dto) {
        if (dto.getSections() == null) {
            dto.setSections(new ArrayList<>());
            return;
        }
        dto.setSections(dto.getSections().stream()
            .peek(section -> {
                if (section.getStrengths() == null) {
                    section.setStrengths(new ArrayList<>());
                }
                if (section.getImprovements() == null) {
                    section.setImprovements(new ArrayList<>());
                }
                if (section.getRewrite() != null) {
                    section.setRewrite(section.getRewrite().trim());
                }
            })
            .collect(Collectors.toList()));
    }

    private Locale resolveLocale(String languageCode) {
        if ("vi".equalsIgnoreCase(languageCode)) {
            return new Locale("vi", "VN");
        }
        return Locale.US;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private void appendPersona(StringBuilder summary, Persona persona, String languageCode) {
        if (summary.length() > 0) {
            summary.append("\n\n");
        }
        if ("vi".equalsIgnoreCase(languageCode)) {
            summary.append(persona.toPromptStringVietnamese());
        } else {
            summary.append(persona.toPromptString());
        }
    }
}
