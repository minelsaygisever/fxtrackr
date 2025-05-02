package com.minelsaygisever.fxtrackr.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.minelsaygisever.fxtrackr.dto.ExchangeRateResponse;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Meta-annotation for documenting the /exchange-rate endpoint.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag(name = "Exchange Rate", description = "Endpoint to retrieve current FX rates")
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
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name  = "InvalidCode",
                                value = "{ \"code\": \"INVALID_PARAMETER_FORMAT\", \"message\": \"Currency code must be three letters\", \"timestamp\": \"2025-05-02T10:15:30.000Z\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description  = "Unexpected server error",
                content = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name  = "InternalError",
                                value = "{ \"code\": \"INTERNAL_ERROR\", \"message\": \"An unexpected error occurred.\", \"timestamp\": \"2025-05-02T10:17:00.000Z\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "502",
                description = "Error calling external exchange-rate service",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name  = "ExternalApiDown",
                                value = "{ \"code\": \"EXTERNAL_API_ERROR\", \"message\": \"Failed to call Fixer API\", \"timestamp\": \"2025-05-02T10:16:00.000Z\" }"
                        )
                )
        )
})
public @interface ExchangeRateApi {
}
