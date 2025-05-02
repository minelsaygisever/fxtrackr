package com.minelsaygisever.fxtrackr.annotation;

import com.minelsaygisever.fxtrackr.dto.ConversionHistoryResponse;
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
@Tag(name = "Conversion History", description = "Endpoint to query past conversions")
@Operation(
        summary     = "Search conversion history",
        description = "Returns a paginated list of past currency conversions filtered by transactionId, date, or both."
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description  = "Conversion history retrieved successfully",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ConversionHistoryResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description  = "Missing or invalid filter parameters",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class),
                        examples  = @ExampleObject(
                                name  = "MissingFilter",
                                value = "{ \"code\": \"MISSING_FILTER\", \"message\": \"Either transactionId or date must be provided\", \"timestamp\": \"2025-05-02T16:00:00.000Z\" }"
                        )
                )
        ),
        @ApiResponse(
                responseCode  = "500",
                description   = "Unexpected server error",
                content       = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class),
                        examples  = @ExampleObject(
                                name  = "InternalError",
                                value = "{ \"code\": \"INTERNAL_ERROR\", \"message\": \"An unexpected error occurred.\", \"timestamp\": \"2025-05-02T16:01:00.000Z\" }"
                        )
                )
        )
})
public @interface SearchHistoryApi {
}
