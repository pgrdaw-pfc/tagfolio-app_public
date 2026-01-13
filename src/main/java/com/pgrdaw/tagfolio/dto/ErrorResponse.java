package com.pgrdaw.tagfolio.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a generic error response.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Getter
@Setter
public class ErrorResponse {
    private String message;

    /**
     * Constructs a new ErrorResponse with the specified message.
     *
     * @param message The error message.
     */
    public ErrorResponse(String message) {
        this.message = message;
    }
}
