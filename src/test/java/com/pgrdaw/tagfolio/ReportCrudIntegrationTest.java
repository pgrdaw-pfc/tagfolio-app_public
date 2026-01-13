package com.pgrdaw.tagfolio;

import com.pgrdaw.tagfolio.model.*;
import com.pgrdaw.tagfolio.repository.*;
import com.pgrdaw.tagfolio.service.ReportService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReportCrudIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ReportTypeRepository reportTypeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Long testUserId;
    private Long testImageId;
    private Long testReportTypeId;

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName(RoleType.USER).orElseThrow();
        User user = new User("testuser@tagfolio.com", passwordEncoder.encode("password"));
        user.addRole(userRole);
        userRepository.save(user);
        this.testUserId = user.getId();

        Image image = new Image(user);
        image.setOriginalFileName("test.jpg");
        image.setThumbnailFileName("thumb.jpg");
        image.setCreatedAt(LocalDateTime.now());
        image.setImportedAt(LocalDateTime.now());
        imageRepository.save(image);
        this.testImageId = image.getId();

        ReportType reportType = reportTypeRepository.findByName("compact").orElseThrow();
        this.testReportTypeId = reportType.getId();

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reportRepository.deleteAll();
        imageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateReport() {
        Report report = reportService.generateReport("Test Report", List.of(testImageId), testReportTypeId);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(report.getId());
        Report foundReport = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals("Test Report", foundReport.getName());
        assertEquals(testReportTypeId, foundReport.getReportType().getId());
        assertEquals(testUserId, foundReport.getUser().getId());
        assertEquals(1, foundReport.getReportImages().size());
        assertEquals(testImageId, foundReport.getReportImages().get(0).getImage().getId());
    }

    @Test
    void testReadReport() {
        Report report = reportService.generateReport("Test Report", List.of(testImageId), testReportTypeId);
        entityManager.flush();
        entityManager.clear();

        Report foundReport = reportService.getReportById(report.getId());
        assertEquals("Test Report", foundReport.getName());
    }

    @Test
    void testUpdateReport() {
        Report report = reportService.generateReport("Original Name", List.of(testImageId), testReportTypeId);
        entityManager.flush();
        entityManager.clear();

        reportService.updateReportName(report.getId(), "Updated Name");
        entityManager.flush();
        entityManager.clear();

        Report updatedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals("Updated Name", updatedReport.getName());
    }

    @Test
    void testDeleteReport() {
        Report report = reportService.generateReport("To Be Deleted", List.of(testImageId), testReportTypeId);
        Long reportId = report.getId();
        entityManager.flush();
        entityManager.clear();

        assertTrue(reportRepository.existsById(reportId));
        User currentUser = userRepository.findById(testUserId).orElseThrow();
        reportService.deleteReports(List.of(reportId), currentUser);
        entityManager.flush();
        entityManager.clear();

        assertFalse(reportRepository.existsById(reportId));
    }

    @Test
    void testGenerateSharedReportHtml() {
        Report report = reportService.generateReport("Shared Report", List.of(testImageId), testReportTypeId);
        entityManager.flush();
        entityManager.clear();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String html = reportService.generateSharedReportHtml(report.getId(), request, response);

        assertNotNull(html);
        assertTrue(html.contains("Shared Report"));
    }
}
