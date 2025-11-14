package com.fbadsautomation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Unified response DTO for a single platform competitor search.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Unified response for competitor search on a platform")
public class PlatformSearchResult {

    @Schema(description = "Indicates whether structured data is available")
    private boolean success;

    @Schema(description = "Target advertising platform")
    private AdPlatform platform;

    @Schema(description = "Original brand keyword")
    private String brandName;

    @Schema(description = "Region/market searched")
    private String region;

    @Schema(description = "Rendering mode for frontend")
    private PlatformSearchMode mode;

    @Schema(description = "Canonical error code if unsuccessful")
    private PlatformSearchErrorCode errorCode;

    @Schema(description = "User-facing message")
    private String message;

    @Schema(description = "Friendly suggestion to unblock user")
    private String friendlySuggestion;

    @Schema(description = "Whether the user should retry later")
    private Boolean retryable;

    @Schema(description = "Total number of structured ads")
    private Integer totalResults;

    @Schema(description = "Structured ads")
    private List<CompetitorAdDTO> ads;

    @Schema(description = "Iframe fallback URL when data unavailable")
    private String iframeUrl;

    @Schema(description = "Suggested alternative regions/platform hints")
    private List<String> fallbackRegions;

    @Schema(description = "Additional metadata for diagnostics")
    private Map<String, Object> metadata;
}
