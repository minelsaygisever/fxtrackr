package com.minelsaygisever.fxtrackr.validation;

import com.minelsaygisever.fxtrackr.exception.BulkProcessingException;
import com.minelsaygisever.fxtrackr.exception.CurrencyNotFoundException;
import com.minelsaygisever.fxtrackr.exception.InvalidAmountException;
import com.minelsaygisever.fxtrackr.exception.InvalidCsvHeaderException;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ValidationUtil {
    private static final List<String> CSV_HEADERS = List.of("amount","from","to");

    /**
     * Normalize and validate a currency code:
     * - Must not be null or blank
     * - Must be three letters (A–Z)
     */
    public String validateAndNormalizeCurrencyCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new CurrencyNotFoundException("Currency code is required");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new CurrencyNotFoundException("Invalid currency code: " + normalized);
        }
        return normalized;
    }

    /**
     * Validates and normalizes a BigDecimal amount:
     * - Must not be null
     * - Must be greater than zero
     * - Max 13 integer digits and 6 decimal digits
     * Returns amount scaled to 6 decimal places using HALF_UP.
     */
    public BigDecimal validateAndNormalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (amount.precision() - amount.scale() > 13) {
            throw new InvalidAmountException("Amount can have up to 13 integer digits");
        }
        return amount.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Validates CSV headers:
     * - Must contain 'amount', 'from', 'to'
     */
    public void validateCsvHeaders(CSVParser parser) {
        Set<String> headers = parser.getHeaderMap().keySet();
        if (!headers.containsAll(CSV_HEADERS)) {
            List<String> missing = new ArrayList<>(CSV_HEADERS);
            missing.removeAll(headers);
            throw new InvalidCsvHeaderException(
                    "Invalid CSV header: missing columns " + missing
            );
        }
    }

    /**
     * Validates CSV record:
     * - Must have values for 'amount', 'from', 'to'
     */
    public void validateCsvRecord(CSVRecord record, int line) {
        try {
            record.get("amount");
            record.get("from");
            record.get("to");
        } catch (IllegalArgumentException iae) {
            throw new BulkProcessingException(
                    "Malformed CSV row " + line + ": expecting columns " + CSV_HEADERS
            );
        }
    }
}
