package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Report;
import com.pgrdaw.tagfolio.model.ReportImage;
import com.pgrdaw.tagfolio.model.ReportImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ReportImage} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface ReportImageRepository extends JpaRepository<ReportImage, ReportImageId> {
    /**
     * Finds a list of report images by report.
     *
     * @param report The report.
     * @return A list of report images.
     */
    List<ReportImage> findByReport(Report report);
}
