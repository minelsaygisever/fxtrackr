package com.minelsaygisever.fxtrackr.annotation;

import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "Convert currency amount",
        description = "Converts a specific amount from one currency to another and returns the converted amount along with a transaction ID."
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Currency converted successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = CurrencyConversionResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid input data (e.g., currency code or amount format issue)",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "502",
                description = "Error calling external exchange-rate service",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class)
                )
        )
})
public @interface CurrencyConversionApi {
}
