package com.satellite.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Typed, immutable error response returned by the API.
 * Replaces raw HashMap — provides compile-time safety for error fields.
 *
 * A Java record automatically generates the constructor, getters,
 * equals(), hashCode(), and toString().
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> messages
) {

    /**
     * Factory for single-message errors (e.g., 404 Not Found).
     */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, null);
    }

    /**
     * Factory for multi-message errors (e.g., 400 Validation Failed).
     */
    public static ErrorResponse of(int status, String error, List<String> messages) {
        return new ErrorResponse(LocalDateTime.now(), status, error, null, messages);
    }
}
