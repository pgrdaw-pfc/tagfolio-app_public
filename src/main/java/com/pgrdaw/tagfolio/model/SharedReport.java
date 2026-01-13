package com.pgrdaw.tagfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a shared report, allowing public access to a user's report.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "shared_reports")
@Getter
@Setter
@NoArgsConstructor
public class SharedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, unique = true)
    private String contentHash;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "id", nullable = false, unique = true)
    private Report report;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    /**
     * Constructs a new SharedReport.
     *
     * @param token        The unique token for the shareable link.
     * @param contentHash  The SHA-256 hash of the report's content.
     * @param report       The report being shared.
     * @param creationDate The timestamp when the shared report was created.
     */
    public SharedReport(String token, String contentHash, Report report, LocalDateTime creationDate) {
        this.token = token;
        this.contentHash = contentHash;
        this.report = report;
        this.creationDate = creationDate;
    }
}
