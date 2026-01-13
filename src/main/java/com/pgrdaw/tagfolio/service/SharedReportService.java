package com.pgrdaw.tagfolio.service;

import com.pgrdaw.tagfolio.model.Report;
import com.pgrdaw.tagfolio.model.SharedReport;
import com.pgrdaw.tagfolio.repository.SharedReportRepository;
import com.pgrdaw.tagfolio.service.util.HashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A service for managing shared reports.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class SharedReportService {

    private final SharedReportRepository sharedReportRepository;
    private final HashService hashService;
    private final String appBaseUrl;

    /**
     * Constructs a new SharedReportService.
     *
     * @param sharedReportRepository The shared report repository.
     * @param hashService            The hash service.
     * @param appBaseUrl             The base URL of the application.
     */
    public SharedReportService(SharedReportRepository sharedReportRepository,
                               HashService hashService,
                               @Value("${app.base-url}") String appBaseUrl) {
        this.sharedReportRepository = sharedReportRepository;
        this.hashService = hashService;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Creates a shareable link for a report.
     *
     * @param report The report to share.
     * @return The shareable link URL.
     */
    @Transactional
    public String createShareableLink(Report report) {
        Optional<SharedReport> existingSharedReportByReportId = sharedReportRepository.findByReportId(report.getId());
        if (existingSharedReportByReportId.isPresent()) {
            return getShareableLinkUrl(existingSharedReportByReportId.get().getToken());
        }

        String reportContent = report.getReportImages().stream()
                .map(ri -> ri.getImage().getId().toString())
                .collect(Collectors.joining(",")) + ":" + report.getReportType().getName();
        String contentHash = hashService.calculateSha256Hash(reportContent);

        Optional<SharedReport> existingSharedReportByContent = sharedReportRepository.findByContentHash(contentHash);

        if (existingSharedReportByContent.isPresent()) {
            return getShareableLinkUrl(existingSharedReportByContent.get().getToken());
        } else {
            String token = UUID.randomUUID().toString();
            SharedReport sharedReport = new SharedReport(token, contentHash, report, LocalDateTime.now());
            sharedReportRepository.save(sharedReport);
            return getShareableLinkUrl(token);
        }
    }

    /**
     * Retrieves a SharedReport by its unique token.
     *
     * @param token The unique token of the shared report.
     * @return An Optional containing the SharedReport if found, empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<SharedReport> getSharedReportByToken(String token) {
        return sharedReportRepository.findByToken(token);
    }

    /**
     * Retrieves a SharedReport by its report ID.
     *
     * @param reportId The ID of the report.
     * @return An Optional containing the SharedReport if found, empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<SharedReport> getSharedReportByReportId(Long reportId) {
        return sharedReportRepository.findByReportId(reportId);
    }

    /**
     * Constructs the full shareable URL for a given report token.
     *
     * @param token The unique token of the shared report.
     * @return The full shareable URL.
     */
    public String getShareableLinkUrl(String token) {
        return appBaseUrl + "/shared/report/" + token;
    }
}
