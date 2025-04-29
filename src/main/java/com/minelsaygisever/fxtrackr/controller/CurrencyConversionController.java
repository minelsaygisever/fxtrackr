package com.minelsaygisever.fxtrackr.controller;

import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.service.CurrencyConversionService;
import com.minelsaygisever.fxtrackr.annotation.swagger.CurrencyCodeParam;
import com.minelsaygisever.fxtrackr.annotation.swagger.ExchangeRateApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CurrencyConversionController {
    private final CurrencyConversionService currencyConversionService;

    @GetMapping("/exchange-rate")
    @ExchangeRateApi
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam @CurrencyCodeParam String from,
            @RequestParam @CurrencyCodeParam String to
    ) {
        log.info("Received /exchange-rate request: from='{}' to='{}'", from, to);
        return ResponseEntity.ok(currencyConversionService.getExchangeRate(from, to));
    }
}
