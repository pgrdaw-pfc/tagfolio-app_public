package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link ReportType} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface ReportTypeRepository extends JpaRepository<ReportType, Long> {
    /**
     * Finds a report type by its name.
     *
     * @param name The name of the report type.
     * @return An {@link Optional} containing the report type if found, or empty otherwise.
     */
    Optional<ReportType> findByName(String name);
}
