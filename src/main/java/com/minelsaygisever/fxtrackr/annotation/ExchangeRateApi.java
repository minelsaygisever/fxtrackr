package com.minelsaygisever.fxtrackr.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;

/**
 * Meta-annotation for documenting the /exchange-rate endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        summary = "Get current exchange rate",
        description = "Returns the latest exchange rate from one currency to another."
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Exchange rate retrieved successfully",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ExchangeRateResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid currency code format",
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
public @interface ExchangeRateApi {
}
