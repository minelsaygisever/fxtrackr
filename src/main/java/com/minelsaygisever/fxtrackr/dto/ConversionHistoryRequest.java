package com.minelsaygisever.fxtrackr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import java.time.LocalDate;

@Data
public class ConversionHistoryRequest {
    @Schema(description = "Filter by transaction ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String transactionId;

    @Schema(description = "Filter by conversion date (YYYY-MM-DD)", example = "2025-04-30")
    private LocalDate date;

    @AssertTrue(message = "Either 'transactionId' or 'date' must be provided")
    private boolean isAtLeastOneFilter() {
        return transactionId != null || date != null;
    }
}
