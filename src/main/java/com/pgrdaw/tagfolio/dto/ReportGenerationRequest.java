package com.pgrdaw.tagfolio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a request to generate a report.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Getter
@Setter
public class ReportGenerationRequest {
    private String name;
    private List<Long> imageIds;
    private Long reportTypeId;
}
