package com.minelsaygisever.fxtrackr.service;

import com.minelsaygisever.fxtrackr.client.FixerRestClient;
import com.minelsaygisever.fxtrackr.domain.Currency;
import com.minelsaygisever.fxtrackr.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * This component runs on application startup to initialize currency data.
 * It fetches all supported currency symbols from the external API (Fixer)
 * and populates the 'currencies' table in the database.
 */
@Component
public class CurrencyDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyDataInitializer.class);

    private final CurrencyRepository currencyRepository;
    private final FixerRestClient fixerRestClient; // Assume you have a service to call the Fixer API

    public CurrencyDataInitializer(CurrencyRepository currencyRepository, FixerRestClient fixerRestClient) {
        this.currencyRepository = currencyRepository;
        this.fixerRestClient = fixerRestClient;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application started. Initializing currency data...");

        try {
            Map<String, String> symbols = fixerRestClient.getSupportedSymbols();

            int newCurrenciesCount = 0;
            for (Map.Entry<String, String> entry : symbols.entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue();

                if (!currencyRepository.existsById(code)) {
                    Currency currency = new Currency(code, name, true); // Assume all are active by default
                    currencyRepository.save(currency);
                    newCurrenciesCount++;
                }
            }

            if (newCurrenciesCount > 0) {
                logger.info("Successfully initialized {} new currencies.", newCurrenciesCount);
            } else {
                logger.info("Currency data is already up-to-date. No new currencies were added.");
            }

        } catch (Exception e) {
            logger.error("Failed to initialize currency data from Fixer API. The application will continue, but some currencies may be missing.", e);
        }

        logger.info("Currency data initialization process finished.");
    }
}
