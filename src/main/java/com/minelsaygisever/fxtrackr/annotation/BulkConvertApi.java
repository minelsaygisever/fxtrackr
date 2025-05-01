package com.minelsaygisever.fxtrackr.annotation;

import com.minelsaygisever.fxtrackr.dto.BulkConversionResult;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
        summary     = "Bulk currency conversion",
        description = "Upload a CSV (headers: amount,from,to) to perform multiple conversions."
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description  = "Bulk conversion processed successfully",
                content      = @Content(
                        mediaType = "application/json",
                        array     = @ArraySchema(
                                schema = @Schema(implementation = BulkConversionResult.class)
                        )
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description  = "Invalid CSV format or data",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description  = "Bulk processing error",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ErrorResponse.class)
                )
        )
})
public @interface BulkConvertApi {
}
