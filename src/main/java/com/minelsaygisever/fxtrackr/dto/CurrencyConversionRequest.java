package com.minelsaygisever.fxtrackr.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class CurrencyConversionRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 6, message = "Amount can have up to 6 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency code is required")
    @Pattern(
            regexp  = "^[A-Za-z]{3}$",
            message = "Currency code must be three letters"
    )
    private String from;

    @NotBlank(message = "Currency code is required")
    @Pattern(
            regexp  = "^[A-Za-z]{3}$",
            message = "Currency code must be three letters"
    )
    private String to;
}
