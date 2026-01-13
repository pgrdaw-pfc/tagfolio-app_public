package com.pgrdaw.tagfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a summary response for an image, containing its ID and thumbnail URL.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageSummaryResponse {
    private Long id;
    private String thumbnailUrl;
}
