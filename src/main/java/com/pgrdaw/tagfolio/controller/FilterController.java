package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.dto.FilterResponse;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.service.FilterService;
import com.pgrdaw.tagfolio.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling filter-related API requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api/filters")
public class FilterController {

    private final UserService userService;
    private final FilterService filterService;

    @Getter
    @Setter
    public static class EvaluationRequest {
        private List<Map<String, String>> expression;
        private String sharedFilterToken;
    }

    /**
     * Constructs a new FilterController.
     *
     * @param userService   The user service.
     * @param filterService The filter service.
     */
    public FilterController(UserService userService,
                            FilterService filterService) {
        this.userService = userService;
        this.filterService = filterService;
    }

    /**
     * Saves a filter.
     *
     * @param payload        The request payload containing the filter name and expression.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the saved filter response.
     */
    @PostMapping("/save")
    public ResponseEntity<FilterResponse> saveFilter(@RequestBody Map<String, Object> payload,
                                                     Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String filterName = (String) payload.get("name");
        String expressionJson = (String) payload.get("expression");

        Filter savedFilter = filterService.saveFilter(filterName, expressionJson, user);
        return ResponseEntity.ok(new FilterResponse(savedFilter.getId().toString(), savedFilter.getName(), savedFilter.getExpression(), false, null));
    }

    /**
     * Gets the saved filters for the authenticated user.
     *
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of filter responses.
     */
    @GetMapping
    public ResponseEntity<List<FilterResponse>> getSavedFilters(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(filterService.getSavedFilters(user));
    }

    /**
     * Evaluates a filter expression.
     *
     * @param request        The evaluation request.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of matching image IDs.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<List<Long>> evaluateFilter(@RequestBody EvaluationRequest request,
                                                     Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        List<Long> matchingImageIds = filterService.evaluateFilter(request.getExpression(), request.getSharedFilterToken(), user);
        return ResponseEntity.ok(matchingImageIds);
    }

    /**
     * Generates a shareable link for a filter.
     *
     * @param filterId       The ID of the filter.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the shareable link.
     */
    @PostMapping("/{filterId}/share")
    public ResponseEntity<String> generateShareableLink(@PathVariable Long filterId,
                                                        Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required to share filters.");
        }
        String shareableLink = filterService.generateShareableLink(filterId, user);
        return ResponseEntity.ok(shareableLink);
    }

    /**
     * Creates a new filter and generates a shareable link.
     *
     * @param payload        The request payload containing the filter name and expression.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the shareable link.
     */
    @PostMapping("/share-or-create")
    public ResponseEntity<String> shareOrCreateFilter(@RequestBody Map<String, Object> payload,
                                                      Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required to share filters.");
        }

        String filterName = (String) payload.get("name");
        String expressionJson = (String) payload.get("expression");

        Filter filter = filterService.saveFilter(filterName, expressionJson, user);

        String shareableLink = filterService.generateShareableLink(filter.getId(), user);
        return ResponseEntity.ok(shareableLink);
    }

    /**
     * Deletes a list of filters.
     *
     * @param filterIds      The list of filter IDs to delete.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     */
    @Transactional
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFilters(@RequestBody List<Long> filterIds,
                                           Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required to delete filters.");
        }
        filterService.deleteFilters(filterIds, user);
        return ResponseEntity.ok("Filters deleted successfully.");
    }
}
