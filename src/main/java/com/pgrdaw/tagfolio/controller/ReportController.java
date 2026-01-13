package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.dto.ImageSummaryResponse;
import com.pgrdaw.tagfolio.dto.ReportDetailsResponse;
import com.pgrdaw.tagfolio.dto.ReportGenerationRequest;
import com.pgrdaw.tagfolio.dto.ReportResponse;
import com.pgrdaw.tagfolio.model.Report;
import com.pgrdaw.tagfolio.model.ReportType;
import com.pgrdaw.tagfolio.model.SharedReport;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ReportTypeRepository;
import com.pgrdaw.tagfolio.service.ReportService;
import com.pgrdaw.tagfolio.service.SharedReportService;
import com.pgrdaw.tagfolio.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for handling report-related API requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("isAuthenticated()")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;
    private final UserService userService;
    private final SharedReportService sharedReportService;
    private final ReportTypeRepository reportTypeRepository;

    /**
     * Constructs a new ReportController.
     *
     * @param reportService        The report service.
     * @param userService          The user service.
     * @param sharedReportService  The shared report service.
     * @param reportTypeRepository The report type repository.
     */
    @Autowired
    public ReportController(ReportService reportService,
                            UserService userService,
                            SharedReportService sharedReportService,
                            ReportTypeRepository reportTypeRepository) {
        this.reportService = reportService;
        this.userService = userService;
        this.sharedReportService = sharedReportService;
        this.reportTypeRepository = reportTypeRepository;
    }

    /**
     * Gets the reports for the authenticated user.
     *
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of report responses.
     */
    @GetMapping
    public ResponseEntity<List<ReportResponse>> getUserReports(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Report> reports = reportService.getReportsByUser(currentUser);

        List<ReportResponse> reportResponses = reports.stream()
                .map(report -> {
                    Optional<SharedReport> sharedReportOptional = sharedReportService.getSharedReportByReportId(report.getId());
                    boolean isShared = sharedReportOptional.isPresent();
                    String shareableLink = isShared ? sharedReportService.getShareableLinkUrl(sharedReportOptional.get().getToken()) : null;
                    return new ReportResponse(report.getId(), report.getName(), shareableLink, isShared);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(reportResponses);
    }

    /**
     * Gets the details of a report.
     *
     * @param reportId       The ID of the report.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the report details.
     */
    @GetMapping("/{reportId}/details")
    public ResponseEntity<ReportDetailsResponse> getReportDetails(@PathVariable Long reportId, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Report report = reportService.getReportById(reportId);
            if (!report.getUser().equals(currentUser) && !currentUser.isAdmin()) {
                throw new AccessDeniedException("You do not have permission to view this report.");
            }

            Optional<SharedReport> sharedReportOptional = sharedReportService.getSharedReportByReportId(report.getId());
            boolean isShared = sharedReportOptional.isPresent();
            String shareableLink = isShared ? sharedReportService.getShareableLinkUrl(sharedReportOptional.get().getToken()) : null;

            List<ImageSummaryResponse> images = report.getReportImages().stream()
                    .map(reportImage -> new ImageSummaryResponse(reportImage.getImage().getId(), "/images/thumbnail/" + reportImage.getImage().getId()))
                    .collect(Collectors.toList());

            ReportDetailsResponse response = new ReportDetailsResponse(
                    report.getId(),
                    report.getName(),
                    report.getReportType().getId(),
                    images,
                    shareableLink,
                    isShared
            );
            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates a new report.
     *
     * @param request        The report generation request.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the generated report response.
     */
    @PostMapping("/generate")
    public ResponseEntity<ReportResponse> generateReport(@RequestBody ReportGenerationRequest request,
                                                         Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Report report = reportService.generateReport(request.getName(), request.getImageIds(), request.getReportTypeId());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReportResponse(report.getId(), report.getName(), null, false));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ReportResponse(null, e.getMessage(), null, false));
        }
    }

    /**
     * Gets all report types.
     *
     * @return A {@link ResponseEntity} with a list of report types.
     */
    @GetMapping("/types")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ReportType>> getAllReportTypes() {
        List<ReportType> reportTypes = reportService.getAllReportTypes();
        return ResponseEntity.ok(reportTypes);
    }

    /**
     * Shares a report.
     *
     * @param reportId       The ID of the report to share.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the shareable link.
     */
    @PostMapping("/{reportId}/share")
    public ResponseEntity<String> shareReport(@PathVariable Long reportId, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Report report = reportService.getReportById(reportId);
        if (!report.getUser().equals(currentUser) && !currentUser.isAdmin()) {
            throw new AccessDeniedException("You do not have permission to share this report.");
        }
        String shareableUrl = sharedReportService.createShareableLink(report);
        return ResponseEntity.ok(shareableUrl);
    }

    /**
     * Updates the name of a report.
     *
     * @param reportId The ID of the report to update.
     * @param newName  The new name for the report.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PutMapping("/{reportId}/name")
    public ResponseEntity<?> updateReportName(@PathVariable Long reportId, @RequestBody String newName) {
        reportService.updateReportName(reportId, newName);
        return ResponseEntity.ok().body("Report name updated successfully.");
    }

    /**
     * Updates the type of a report.
     *
     * @param reportId        The ID of the report to update.
     * @param newReportTypeId The ID of the new report type.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PutMapping("/{reportId}/type")
    public ResponseEntity<?> updateReportType(@PathVariable Long reportId, @RequestBody Long newReportTypeId) {
        ReportType reportType = reportTypeRepository.findById(newReportTypeId)
                .orElseThrow(() -> new NoSuchElementException("Report type with ID '" + newReportTypeId + "' not found."));
        reportService.updateReportType(reportId, reportType.getId());
        return ResponseEntity.ok().body("Report type updated successfully.");
    }

    /**
     * Reorders the images in a report.
     *
     * @param reportId      The ID of the report to update.
     * @param newImageOrder The new order of image IDs.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PutMapping("/{reportId}/images/reorder")
    public ResponseEntity<?> reorderReportImages(@PathVariable Long reportId, @RequestBody List<Long> newImageOrder) {
        reportService.updateImageOrder(reportId, newImageOrder);
        return ResponseEntity.ok().body("Image order updated successfully.");
    }

    /**
     * Deletes a list of reports.
     *
     * @param reportIds      The list of report IDs to delete.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteReports(@RequestBody List<Long> reportIds, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        reportService.deleteReports(reportIds, currentUser);
        return ResponseEntity.ok().build();
    }
}
