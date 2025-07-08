package com.minelsaygisever.fxtrackr.client;

import com.google.common.util.concurrent.RateLimiter;
import com.minelsaygisever.fxtrackr.dto.FixerError;
import com.minelsaygisever.fxtrackr.dto.FixerResponse;
import com.minelsaygisever.fxtrackr.dto.FixerSymbolsResponse;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixerRestClient {

    private final RestTemplate restTemplate;

    @Value("${fixer.api.key}")
    private String apiKey;

    @Value("${fixer.api.url}")
    private String apiUrl;

    // Using free-tier Fixer API: throttle to 1 request every 2.5 seconds to avoid rate-limit errors
    private final RateLimiter rateLimiter = RateLimiter.create(0.4);

    /**
     * Fetches all supported currency symbols from the Fixer API.
     * This method is intended to be called once on application startup to populate the database.
     * It respects the class-level rate limiter.
     *
     * @return A map of currency codes to currency names (e.g., "USD" -> "United States Dollar").
     * @throws ExternalApiException if the API call fails or returns an unsuccessful response.
     */
    public Map<String, String> getSupportedSymbols() {
        rateLimiter.acquire();
        String url = String.format("%s/symbols?access_key=%s", apiUrl, apiKey);
        log.debug("Calling Fixer Symbols URL: {}", url);

        try {
            ResponseEntity<FixerSymbolsResponse> response = restTemplate.getForEntity(url, FixerSymbolsResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalApiException("Unexpected HTTP response from Fixer API: HTTP " + response.getStatusCode());
            }

            FixerSymbolsResponse symbolsResponse = response.getBody();

            if (!symbolsResponse.isSuccess()) {
                String info = Optional.ofNullable(symbolsResponse.getError())
                        .map(FixerError::getInfo)
                        .orElse("Unknown error while fetching symbols.");
                throw new ExternalApiException("Fixer API returned an error: " + info);
            }

            return symbolsResponse.getSymbols();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to call Fixer API's /symbols endpoint", ex);
        }
    }

    /**
     * Fetches the latest exchange rates from the Fixer API against the base currency (EUR).
     * This is the primary method for getting rate data.
     * @return A map of currency codes to their rates against the base currency.
     */
    public Map<String, BigDecimal> getLatestRates() {
        rateLimiter.acquire();
        String url = String.format("%s/latest?access_key=%s", apiUrl, apiKey);
        log.debug("Calling Fixer URL for all latest rates.");

        try {
            ResponseEntity<FixerResponse> response = restTemplate.getForEntity(url, FixerResponse.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalApiException("Unexpected HTTP response from Fixer API: HTTP " + response.getStatusCode());
            }
            FixerResponse fixerResponse = response.getBody();
            if (!fixerResponse.isSuccess()) {
                String info = Optional.ofNullable(fixerResponse.getError()).map(FixerError::getInfo).orElse("Unknown error.");
                throw new ExternalApiException("Fixer API returned an error: " + info);
            }
            return fixerResponse.getRates();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to call Fixer API's /latest endpoint", ex);
        }
    }
}
