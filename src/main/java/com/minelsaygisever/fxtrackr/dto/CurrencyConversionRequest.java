package com.minelsaygisever.fxtrackr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Schema(
        name        = "CurrencyConversionRequest",
        description = "Request payload for converting a monetary amount from one currency to another"
)
public class CurrencyConversionRequest {
    @Schema(description = "Amount to convert", example = "100.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 6, message = "Amount can have up to 6 decimal places")
    private BigDecimal amount;

    @Schema(description = "Source currency code (3 letters)", example = "USD")
    @NotBlank(message = "Field 'from' – Currency code is required")
    @Pattern(
            regexp  = "^[A-Za-z]{3}$",
            message = "Currency code must be three letters"
    )
    private String from;

    @Schema(description = "Target currency code (3 letters)", example = "EUR")
    @NotBlank(message = "Field 'to' – Currency code is required")
    @Pattern(
            regexp  = "^[A-Za-z]{3}$",
            message = "Currency code must be three letters"
    )
    private String to;
}
