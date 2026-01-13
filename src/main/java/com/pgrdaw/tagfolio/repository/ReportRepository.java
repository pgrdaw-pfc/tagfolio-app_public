package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Report;
import com.pgrdaw.tagfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Report} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Finds a list of reports by user, fetching all details.
     *
     * @param user The user.
     * @return A list of reports.
     */
    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.reportType rt " +
            "JOIN FETCH r.user u " +
            "LEFT JOIN FETCH r.reportImages ri " +
            "LEFT JOIN FETCH ri.image img " +
            "WHERE r.user = :user " +
            "ORDER BY r.id DESC")
    List<Report> findByUser(User user);

    /**
     * Finds all reports, fetching all details.
     *
     * @return A list of all reports.
     */
    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.reportType rt " +
            "JOIN FETCH r.user u " +
            "LEFT JOIN FETCH r.reportImages ri " +
            "LEFT JOIN FETCH ri.image img " +
            "ORDER BY r.id DESC")
    List<Report> findAllWithDetails();

    /**
     * Finds a report by its ID, fetching all details.
     *
     * @param id The ID of the report.
     * @return An {@link Optional} containing the report if found, or empty otherwise.
     */
    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.reportType rt " +
            "LEFT JOIN FETCH r.reportImages ri " +
            "LEFT JOIN FETCH ri.image i " +
            "LEFT JOIN FETCH i.tags t " +
            "WHERE r.id = :id")
    Optional<Report> findByIdWithAllDetails(@Param("id") Long id);
}
