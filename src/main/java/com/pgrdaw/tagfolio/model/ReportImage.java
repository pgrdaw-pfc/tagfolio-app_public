package com.pgrdaw.tagfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the association between a report and an image, including a sorting order.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "report_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportImage {

    @EmbeddedId
    private ReportImageId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reportId")
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("imageId")
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(name = "sorting_order")
    private Integer sortingOrder;

    /**
     * Constructs a new ReportImage with the specified report, image, and sorting order.
     *
     * @param report       The report.
     * @param image        The image.
     * @param sortingOrder The sorting order of the image within the report.
     */
    public ReportImage(Report report, Image image, Integer sortingOrder) {
        this.id = new ReportImageId(report.getId(), image.getId());
        this.report = report;
        this.image = image;
        this.sortingOrder = sortingOrder;
    }
}
