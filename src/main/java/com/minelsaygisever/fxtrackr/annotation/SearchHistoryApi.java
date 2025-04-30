package com.minelsaygisever.fxtrackr.annotation;

import com.minelsaygisever.fxtrackr.dto.ConversionHistoryRequest;
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
        summary     = "Search conversion history",
        description = "Returns a paginated list of past currency conversions filtered by transactionId, date, or both."
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description  = "Conversion history retrieved successfully",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ConversionHistoryRequest.class)
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description  = "Missing or invalid filter parameters",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class)
                )
        )
})
public @interface SearchHistoryApi {
}
