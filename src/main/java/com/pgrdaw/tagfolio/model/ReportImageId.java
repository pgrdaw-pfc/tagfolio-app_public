package com.pgrdaw.tagfolio.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents the composite primary key for the {@link ReportImage} entity.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportImageId implements Serializable {

    private Long reportId;
    private Long imageId;
}
