package com.minelsaygisever.fxtrackr.client;

import com.google.common.util.concurrent.RateLimiter;
import com.minelsaygisever.fxtrackr.dto.FixerError;
import com.minelsaygisever.fxtrackr.dto.FixerResponse;
import com.minelsaygisever.fxtrackr.dto.FixerSymbolsResponse;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * Fetches and caches the exchange rate between two currencies.
     * @param from source currency code
     * @param to target currency code
     * @return calculated exchange rate
     */
    @Cacheable(value = "fixerRates", key = "#from + '_' + #to")
    public BigDecimal getRate(String from, String to) {
        rateLimiter.acquire();
        FixerResponse response = callFixerLatestRates(from, to);
        if (!response.isSuccess()) {
            String info = Optional.ofNullable(response.getError())
                    .map(FixerError::getInfo)
                    .orElse("Unknown error");
            throw new ExternalApiException("Fixer API error: " + info);
        }

        BigDecimal rateFrom = ofCurrency(response.getRates(), from);
        BigDecimal rateTo   = ofCurrency(response.getRates(), to);
        return rateTo.divide(rateFrom, 6, RoundingMode.HALF_UP);
    }

    /**
     * Calls Fixer API for latest rates.
     * @param from source currency code
     * @param to target currency code
     * @return parsed FixerResponse
     */
    private FixerResponse callFixerLatestRates(String from, String to) {
        String url = String.format("%s/latest?access_key=%s&symbols=%s,%s", apiUrl, apiKey, from, to);
        log.debug("Calling Fixer URL: {}", url);
        try {
            ResponseEntity<FixerResponse> response = restTemplate.getForEntity(url, FixerResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalApiException("Unexpected HTTP response from Fixer API: HTTP " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalApiException("Failed to call Fixer API", ex);
        }
    }

    /**
     * Retrieves rate value or throws if unsupported.
     */
    private BigDecimal ofCurrency(Map<String, BigDecimal> rates, String currency) {
        BigDecimal value = rates.get(currency);
        if (value == null) {
            throw new CurrencyNotFoundException("Currency not supported: " + currency);
        }
        return value;
    }
}
