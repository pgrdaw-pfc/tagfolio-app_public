package com.pgrdaw.tagfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pgrdaw.tagfolio.model.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a response object for a report.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private String name;
    private String shareableLink;
    @JsonProperty("isShared")
    private boolean isShared;
    private Long reportTypeId;
    private List<Image> images;

    /**
     * Constructs a new ReportResponse.
     *
     * @param id            The report ID.
     * @param name          The report name.
     * @param shareableLink The shareable link for the report.
     * @param isShared      Whether the report is shared.
     */
    public ReportResponse(Long id, String name, String shareableLink, boolean isShared) {
        this.id = id;
        this.name = name;
        this.shareableLink = shareableLink;
        this.isShared = isShared;
    }
}
