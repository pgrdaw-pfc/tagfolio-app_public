package com.pgrdaw.tagfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.dto.FilterResponse;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.FilterRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.FilterService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FilterCrudIntegrationTest {

    @Autowired
    private FilterService filterService;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User("testuser@tagfolio.com", passwordEncoder.encode("password"));
        userRepository.save(user);
        this.testUserId = user.getId();

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        filterRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createExpressionJson(String tag) throws JsonProcessingException {
        return objectMapper.writeValueAsString(List.of(Map.of("type", "tag", "value", tag)));
    }

    @Test
    void testCreateFilter() throws JsonProcessingException {
        User user = userRepository.findById(testUserId).orElseThrow();
        String expression = createExpressionJson("test-tag");

        Filter createdFilter = filterService.saveFilter("Test Filter", expression, user);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(createdFilter.getId());
        Filter foundFilter = filterRepository.findById(createdFilter.getId()).orElseThrow();
        assertEquals("Test Filter", foundFilter.getName());
        assertEquals(expression, foundFilter.getExpression());
        assertEquals(testUserId, foundFilter.getUser().getId());
    }

    @Test
    void testReadFilter() throws JsonProcessingException {
        User user = userRepository.findById(testUserId).orElseThrow();
        String expression = createExpressionJson("test-tag");
        filterService.saveFilter("Test Filter", expression, user);
        entityManager.flush();
        entityManager.clear();

        User currentUser = userRepository.findById(testUserId).orElseThrow();
        List<FilterResponse> filters = filterService.getSavedFilters(currentUser);
        assertEquals(1, filters.size());
        assertEquals("Test Filter", filters.get(0).getName());
    }

    @Test
    void testUpdateFilter() throws JsonProcessingException {
        User user = userRepository.findById(testUserId).orElseThrow();
        String expression = createExpressionJson("test-tag");
        Filter originalFilter = filterService.saveFilter("Original Name", expression, user);
        entityManager.flush();
        entityManager.clear();

        User currentUser = userRepository.findById(testUserId).orElseThrow();
        Filter updatedFilter = filterService.saveFilter("Updated Name", expression, currentUser);
        entityManager.flush();
        entityManager.clear();

        assertEquals(originalFilter.getId(), updatedFilter.getId());
        Filter foundFilter = filterRepository.findById(originalFilter.getId()).orElseThrow();
        assertEquals("Updated Name", foundFilter.getName());
    }

    @Test
    void testDeleteFilter() throws JsonProcessingException {
        User user = userRepository.findById(testUserId).orElseThrow();
        String expression = createExpressionJson("to-be-deleted");
        Filter filter = filterService.saveFilter("To Be Deleted", expression, user);
        Long filterId = filter.getId();
        entityManager.flush();
        entityManager.clear();

        assertTrue(filterRepository.existsById(filterId));
        User currentUser = userRepository.findById(testUserId).orElseThrow();
        filterService.deleteFilters(List.of(filterId), currentUser);
        entityManager.flush();
        entityManager.clear();

        assertFalse(filterRepository.existsById(filterId));
    }

    @Test
    void testShareFilter() throws JsonProcessingException {
        User user = userRepository.findById(testUserId).orElseThrow();
        String expression = createExpressionJson("shared-tag");
        Filter filter = filterService.saveFilter("Shared Filter", expression, user);
        entityManager.flush();
        entityManager.clear();

        User currentUser = userRepository.findById(testUserId).orElseThrow();
        String shareableLink = filterService.generateShareableLink(filter.getId(), currentUser);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(shareableLink);
        assertTrue(shareableLink.contains("/shared/filter/"));

        Filter sharedFilter = filterRepository.findById(filter.getId()).orElseThrow();
        assertNotNull(sharedFilter.getSharedFilter());
        assertNotNull(sharedFilter.getSharedFilter().getToken());
    }
}
