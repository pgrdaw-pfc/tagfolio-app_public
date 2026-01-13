package com.pgrdaw.tagfolio.service;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Report;
import com.pgrdaw.tagfolio.model.ReportImage;
import com.pgrdaw.tagfolio.model.ReportType;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.ReportRepository;
import com.pgrdaw.tagfolio.repository.ReportTypeRepository;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A service for managing reports.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ImageRepository imageRepository;
    private final ReportTypeRepository reportTypeRepository;
    private final TemplateEngine templateEngine;
    private final UserService userService;
    private final MetadataService metadataService;

    /**
     * Constructs a new ReportService.
     *
     * @param reportRepository   The report repository.
     * @param imageRepository    The image repository.
     * @param reportTypeRepository The report type repository.
     * @param templateEngine     The Thymeleaf template engine.
     * @param userService        The user service.
     * @param metadataService    The metadata service.
     */
    @Autowired
    public ReportService(ReportRepository reportRepository,
                         ImageRepository imageRepository,
                         ReportTypeRepository reportTypeRepository,
                         TemplateEngine templateEngine,
                         UserService userService,
                         MetadataService metadataService) {
        this.reportRepository = reportRepository;
        this.imageRepository = imageRepository;
        this.reportTypeRepository = reportTypeRepository;
        this.templateEngine = templateEngine;
        this.userService = userService;
        this.metadataService = metadataService;
    }

    /**
     * Generates a new report.
     *
     * @param name         The name of the report.
     * @param imageIds     The IDs of the images to include in the report.
     * @param reportTypeId The ID of the report type.
     * @return The newly generated report.
     * @throws IllegalArgumentException if no authenticated user is found or the report type is invalid.
     */
    @Transactional
    public Report generateReport(String name, List<Long> imageIds, Long reportTypeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalArgumentException("No authenticated user found to create the report.");
        }

        List<Image> images = imageRepository.findAllById(imageIds);
        Map<Long, Image> imageMap = images.stream()
                .collect(Collectors.toMap(Image::getId, Function.identity()));

        if (images.size() != imageIds.size()) {
            System.err.println("Warning: Some image IDs not found for report generation.");
        }

        ReportType selectedReportType = reportTypeRepository.findById(reportTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Report type with ID '" + reportTypeId + "' not found."));

        Report report = new Report();
        report.setName(name);
        report.setReportType(selectedReportType);
        report.setCreationDate(LocalDateTime.now());
        report.setUser(currentUser);

        List<ReportImage> reportImages = new ArrayList<>();
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            Image image = imageMap.get(imageId);
            if (image != null) {
                ReportImage reportImage = new ReportImage(report, image, i);
                reportImages.add(reportImage);
            }
        }
        report.setReportImages(reportImages);

        return reportRepository.save(report);
    }

    /**
     * Retrieves a report by its ID.
     *
     * @param id The ID of the report.
     * @return The report.
     * @throws IllegalArgumentException if the report is not found.
     */
    @Transactional(readOnly = true)
    public Report getReportById(Long id) {
        Report report = reportRepository.findByIdWithAllDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + id));

        List<ReportImage> uniqueReportImages = new ArrayList<>(new LinkedHashSet<>(report.getReportImages()));
        uniqueReportImages.sort(Comparator.comparing(ReportImage::getSortingOrder));
        
        report.getReportImages().clear();
        report.getReportImages().addAll(uniqueReportImages);

        return report;
    }

    private Report getReportAndVerifyOwnership(Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser == null) {
            throw new IllegalArgumentException("No authenticated user found.");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + reportId));

        if (!report.getUser().equals(currentUser) && !currentUser.isAdmin()) {
            throw new AccessDeniedException("You do not have permission to modify this report.");
        }
        return report;
    }

    /**
     * Updates the name of a report.
     *
     * @param reportId The ID of the report to update.
     * @param newName  The new name for the report.
     */
    @Transactional
    public void updateReportName(Long reportId, String newName) {
        Report report = getReportAndVerifyOwnership(reportId);
        report.setName(newName);
        reportRepository.save(report);
    }

    /**
     * Updates the type of a report.
     *
     * @param reportId      The ID of the report to update.
     * @param newReportTypeId The ID of the new report type.
     * @throws IllegalArgumentException if the new report type is not found.
     */
    @Transactional
    public void updateReportType(Long reportId, Long newReportTypeId) {
        Report report = getReportAndVerifyOwnership(reportId);
        ReportType newReportType = reportTypeRepository.findById(newReportTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Report type with ID '" + newReportTypeId + "' not found."));

        report.setReportType(newReportType);
        reportRepository.save(report);
    }

    /**
     * Updates the order of images within a report.
     *
     * @param reportId      The ID of the report to update.
     * @param newImageOrder The new order of image IDs.
     * @throws IllegalArgumentException if an image in the new order is not found.
     * @throws AccessDeniedException    if the user does not have permission to add an image to the report.
     */
    @Transactional
    public void updateImageOrder(Long reportId, List<Long> newImageOrder) {
        Report report = getReportAndVerifyOwnership(reportId);
        User currentUser = userService.getCurrentUser(SecurityContextHolder.getContext().getAuthentication());

        List<ReportImage> managedReportImages = report.getReportImages();

        Map<Long, ReportImage> existingReportImageMap = managedReportImages.stream()
                .collect(Collectors.toMap(ri -> ri.getImage().getId(), ri -> ri));

        List<ReportImage> reportImagesToKeep = new ArrayList<>();

        for (int i = 0; i < newImageOrder.size(); i++) {
            Long imageId = newImageOrder.get(i);
            ReportImage reportImage = existingReportImageMap.get(imageId);

            if (reportImage != null) {
                reportImage.setSortingOrder(i);
                reportImagesToKeep.add(reportImage);
            } else {
                Image image = imageRepository.findById(imageId)
                        .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));
                if (!image.getUser().equals(currentUser) && !currentUser.isAdmin()) {
                    throw new AccessDeniedException("You do not have permission to add image with ID: " + imageId + " to this report.");
                }
                ReportImage newReportImage = new ReportImage(report, image, i);
                reportImagesToKeep.add(newReportImage);
            }
        }

        managedReportImages.clear();
        managedReportImages.addAll(reportImagesToKeep);

        reportRepository.save(report);
    }

    /**
     * Retrieves all available report types.
     *
     * @return A list of {@link ReportType} objects.
     */
    @Transactional(readOnly = true)
    public List<ReportType> getAllReportTypes() {
        return reportTypeRepository.findAll();
    }

    private WebContext createWebContext(HttpServletRequest request, HttpServletResponse response, Report report) {
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        final WebContext context = new WebContext(application.buildExchange(request, response));
        
        context.setVariable("report", report);
        context.setVariable("json", new com.fasterxml.jackson.databind.ObjectMapper());
        context.setVariable("metadataService", metadataService);
        context.setVariable("isEditable", false);
        
        return context;
    }

    /**
     * Generates the HTML content for a shared report.
     *
     * @param reportId The ID of the report.
     * @param request  The HTTP servlet request.
     * @param response The HTTP servlet response.
     * @return The HTML content of the shared report.
     */
    @Transactional(readOnly = true)
    public String generateSharedReportHtml(Long reportId, HttpServletRequest request, HttpServletResponse response) {
        Report report = getReportById(reportId);
        
        List<Long> sharedImageIds = report.getReportImages().stream()
                .map(ri -> ri.getImage().getId())
                .collect(Collectors.toList());
        request.getSession().setAttribute("sharedImageIds", sharedImageIds);

        WebContext context = createWebContext(request, response, report);

        String templateName = "reports/" + report.getReportType().getName().toLowerCase();
        return templateEngine.process(templateName, context);
    }

    /**
     * Retrieves a list of reports for a given user.
     *
     * @param user The user.
     * @return A list of reports.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsByUser(User user) {
        List<Report> reports;
        if (user.isAdmin()) {
            reports = reportRepository.findAllWithDetails();
        } else {
            reports = reportRepository.findByUser(user);
        }
        Set<Report> uniqueReports = new LinkedHashSet<>(reports);
        return new ArrayList<>(uniqueReports);
    }

    /**
     * Deletes a list of reports.
     *
     * @param reportIds   The IDs of the reports to delete.
     * @param currentUser The current user performing the action.
     * @throws IllegalArgumentException if no reports are found with the given IDs.
     * @throws AccessDeniedException    if the user does not have permission to delete a report.
     */
    @Transactional
    public void deleteReports(List<Long> reportIds, User currentUser) {
        List<Report> reportsToDelete = reportRepository.findAllById(reportIds);

        if (reportsToDelete.isEmpty()) {
            throw new IllegalArgumentException("No reports found with the given IDs.");
        }

        for (Report report : reportsToDelete) {
            if (!report.getUser().equals(currentUser) && !currentUser.isAdmin()) {
                throw new AccessDeniedException("You do not have permission to delete report with ID: " + report.getId());
            }
        }
        reportRepository.deleteAll(reportsToDelete);
    }
}
