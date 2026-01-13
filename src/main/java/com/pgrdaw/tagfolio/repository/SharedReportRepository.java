package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.SharedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link SharedReport} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface SharedReportRepository extends JpaRepository<SharedReport, Long> {

    /**
     * Finds a shared report by its content hash.
     *
     * @param contentHash The content hash of the shared report.
     * @return An {@link Optional} containing the shared report if found, or empty otherwise.
     */
    Optional<SharedReport> findByContentHash(String contentHash);

    /**
     * Finds a shared report by the ID of the original report it shares.
     *
     * @param reportId The ID of the original report.
     * @return An {@link Optional} containing the shared report if found, or empty otherwise.
     */
    Optional<SharedReport> findByReportId(Long reportId);

    /**
     * Finds a shared report by its token.
     *
     * @param token The token of the shared report.
     * @return An {@link Optional} containing the shared report if found, or empty otherwise.
     */
    Optional<SharedReport> findByToken(String token);
}
