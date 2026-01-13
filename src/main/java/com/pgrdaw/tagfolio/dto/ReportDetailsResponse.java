package com.pgrdaw.tagfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a detailed response for a report, including its images and sharing status.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailsResponse {
    private Long id;
    private String name;
    private Long reportTypeId;
    private List<ImageSummaryResponse> images;
    private String shareableLink;
    @JsonProperty("isShared")
    private boolean isShared;
}
