package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.SharedReport;
import com.pgrdaw.tagfolio.service.ReportService;
import com.pgrdaw.tagfolio.service.SharedReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Controller for handling shared report view requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/shared/report")
public class SharedReportViewController {

    private final SharedReportService sharedReportService;
    private final ReportService reportService;

    /**
     * Constructs a new SharedReportViewController.
     *
     * @param sharedReportService The shared report service.
     * @param reportService       The report service.
     */
    public SharedReportViewController(SharedReportService sharedReportService, ReportService reportService) {
        this.sharedReportService = sharedReportService;
        this.reportService = reportService;
    }

    /**
     * Displays the shared report.
     *
     * @param token    The shared report token.
     * @param request  The HTTP request.
     * @param response The HTTP response.
     * @return The HTML content of the shared report.
     * @throws ResponseStatusException if the shared report is not found.
     */
    @GetMapping("/{token}")
    @ResponseBody
    public String getSharedReport(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) {
        Optional<SharedReport> sharedReportOptional = sharedReportService.getSharedReportByToken(token);
        if (sharedReportOptional.isPresent()) {
            SharedReport sharedReport = sharedReportOptional.get();
            return reportService.generateSharedReportHtml(sharedReport.getReport().getId(), request, response);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared report not found");
        }
    }
}
