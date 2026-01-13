package com.pgrdaw.tagfolio.service;

/**
 * An exception thrown by the {@link FilterService}.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
public class FilterServiceException extends RuntimeException {
    /**
     * Constructs a new FilterServiceException with the specified detail message.
     *
     * @param message The detail message.
     */
    public FilterServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new FilterServiceException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause   The cause.
     */
    public FilterServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
