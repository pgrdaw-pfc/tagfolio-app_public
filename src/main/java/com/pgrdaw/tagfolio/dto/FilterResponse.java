package com.pgrdaw.tagfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a response object for a filter.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Getter
@Setter
public class FilterResponse {
    private String id;
    private String name;
    private String expression;
    @JsonProperty("isShared")
    private boolean isShared;
    private String shareableLink;

    /**
     * Constructs a new FilterResponse.
     *
     * @param id            The filter ID.
     * @param name          The filter name.
     * @param expression    The filter expression.
     * @param isShared      Whether the filter is shared.
     * @param shareableLink The shareable link for the filter.
     */
    public FilterResponse(String id, String name, String expression, boolean isShared, String shareableLink) {
        this.id = id;
        this.name = name;
        this.expression = expression;
        this.isShared = isShared;
        this.shareableLink = shareableLink;
    }
}
