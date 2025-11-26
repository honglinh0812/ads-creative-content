package com.fbadsautomation.integration.facebook;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FacebookAccountMetadataService {

    private static final Logger log = LoggerFactory.getLogger(FacebookAccountMetadataService.class);
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofHours(6);

    private final RestTemplate restTemplate;
    private final FacebookProperties facebookProperties;

    private Instant lastCurrencyRefresh;

    public void ensureCurrencyLoaded(boolean forceRefresh) {
        if (!facebookProperties.isAutoDetectCurrency()) {
            return;
        }
        synchronized (this) {
            if (!forceRefresh && lastCurrencyRefresh != null
                && Instant.now().isBefore(lastCurrencyRefresh.plus(DEFAULT_REFRESH_INTERVAL))) {
                return;
            }

            String accessToken = facebookProperties.getMarketingAccessToken();
            String adAccountId = facebookProperties.getDefaultAdAccountId();
            if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(adAccountId)) {
                log.debug("Skipping currency auto-detect. Missing token or ad account id.");
                return;
            }

            try {
                String normalizedAccountId = adAccountId.startsWith("act_")
                    ? adAccountId
                    : "act_" + adAccountId;
                String url = String.format("%s/v%s/%s?fields=currency&access_token=%s",
                    facebookProperties.getApiUrl(),
                    facebookProperties.getApiVersion(),
                    normalizedAccountId,
                    accessToken);

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("currency")) {
                    Object currency = body.get("currency");
                    if (currency instanceof String currencyCode && StringUtils.hasText(currencyCode)) {
                        String detected = currencyCode.trim().toUpperCase(Locale.ROOT);
                        if (!detected.equalsIgnoreCase(facebookProperties.getAccountCurrency())) {
                            log.info("Detected Facebook ad account currency: {} (previously: {})",
                                detected, facebookProperties.getAccountCurrency());
                        } else {
                            log.debug("Facebook ad account currency already set to {}", detected);
                        }
                        facebookProperties.setAccountCurrency(detected);
                        lastCurrencyRefresh = Instant.now();
                        return;
                    }
                }
                log.warn("Could not detect Facebook ad account currency. Response: {}", body);
            } catch (Exception ex) {
                log.warn("Failed to auto-detect Facebook account currency: {}", ex.getMessage());
            }
        }
    }
}
