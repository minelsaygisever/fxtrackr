package com.minelsaygisever.fxtrackr.annotation;

import com.minelsaygisever.fxtrackr.dto.CurrencyConversionResponse;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Tag(name = "Currency Conversion", description = "Endpoint to convert a single currency amount")
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
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples  = @ExampleObject(
                                name  = "ValidationFailure",
                                value = "{ \"code\": \"INVALID_PARAMETER_FORMAT\", \"message\": \"Amount must be greater than zero\", \"timestamp\": \"2025-05-02T15:00:00.000Z\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description  = "Unexpected server error",
                content = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class),
                        examples  = @ExampleObject(
                                name  = "InternalError",
                                value = "{ \"code\": \"INTERNAL_ERROR\", \"message\": \"An unexpected error occurred.\", \"timestamp\": \"2025-05-02T15:02:00.000Z\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode = "502",
                description = "Error calling external exchange-rate service",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples  = @ExampleObject(
                                name  = "ExternalApiError",
                                value = "{ \"code\": \"EXTERNAL_API_ERROR\", \"message\": \"Fixer API failed\", \"timestamp\": \"2025-05-02T15:01:00.000Z\" }"
                        )
                )
        )
})
public @interface CurrencyConversionApi {
}
