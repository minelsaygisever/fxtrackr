package com.minelsaygisever.fxtrackr.annotation;

import java.lang.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;


/**
 * Meta-annotation combining request parameter validation and OpenAPI metadata
 * for currency codes (three letters, case-insensitive).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Parameter(
        in          = ParameterIn.QUERY,
        description = "Currency code (3 letters), case-insensitive",
        required    = true,
        schema      = @Schema(type = "string", pattern = "^[A-Za-z]{3}$")
)
@Pattern(
        regexp = "^[A-Za-z]{3}$",
        message = "Currency code must be three letters"
)
public @interface CurrencyCodeParam {
    String message() default "Currency code must be three letters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
