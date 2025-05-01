package com.minelsaygisever.fxtrackr.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.minelsaygisever.fxtrackr.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 Bad Request when currency code is unsupported.
     */
    @ExceptionHandler(CurrencyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyNotFound(CurrencyNotFoundException ex) {
        ErrorResponse err = new ErrorResponse(
                "INVALID_CURRENCY",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    /**
     * 502 Bad Gateway on external API failures.
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException ex) {
        ErrorResponse err = new ErrorResponse(
                "EXTERNAL_API_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(err);
    }

    /**
     * 500 Internal Server Error for uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse err = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    /**
     * 400 Bad Request for parameter validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> String.format("%s", fieldError.getDefaultMessage()))
                .findFirst()
                .orElse("Invalid parameter");

        ErrorResponse err = new ErrorResponse(
                "INVALID_PARAMETER_FORMAT",
                msg,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Invalid parameter");

        ErrorResponse err = new ErrorResponse(
                "INVALID_PARAMETER_FORMAT",
                msg,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        String msg = "Missing required parameter: " + name;
        ErrorResponse err = new ErrorResponse("MISSING_PARAMETER", msg, LocalDateTime.now());
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(InvalidAmountException ex) {
        ErrorResponse err = new ErrorResponse(
                "INVALID_AMOUNT",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(FilterParameterException.class)
    public ResponseEntity<ErrorResponse> handleFilterParam(FilterParameterException ex) {
        ErrorResponse err = new ErrorResponse(
                "MISSING_FILTER",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
        String message = "Malformed JSON request";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException invalidEx = (InvalidFormatException) cause;
            if (invalidEx.getTargetType().equals(LocalDate.class)) {
                message = "Invalid date format, expected YYYY-MM-DD";
            } else {
                message = "Invalid value '" + invalidEx.getValue()
                        + "' for type " + invalidEx.getTargetType().getSimpleName();
            }
        }
        ErrorResponse err = new ErrorResponse(
                "INVALID_REQUEST",
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(BulkProcessingException.class)
    public ResponseEntity<ErrorResponse> handleBulkProcessing(BulkProcessingException ex) {
        ErrorResponse err = new ErrorResponse(
                "BULK_PROCESSING_ERROR",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    @ExceptionHandler(InvalidCsvHeaderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCsvHeader(InvalidCsvHeaderException ex) {
        ErrorResponse err = new ErrorResponse(
                "INVALID_CSV_HEADER",
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(err);
    }
}
