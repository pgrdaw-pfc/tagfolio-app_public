package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.model.TagWithCounter;
import com.pgrdaw.tagfolio.repository.FilterRepository;
import com.pgrdaw.tagfolio.repository.TagRepository;
import com.pgrdaw.tagfolio.service.FilterExportService;
import com.pgrdaw.tagfolio.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Controller for handling main view-related requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
public class ViewController {

    private final TagRepository tagRepository;
    private final UserService userService;
    private final FilterRepository filterRepository;
    private final FilterExportService filterExportService;
    private final int batchSize;

    /**
     * Constructs a new ViewController.
     *
     * @param tagRepository       The tag repository.
     * @param userService         The user service.
     * @param filterRepository    The filter repository.
     * @param filterExportService The filter export service.
     * @param batchSize           The batch size for image loading.
     */
    public ViewController(TagRepository tagRepository,
                            UserService userService,
                            FilterRepository filterRepository,
                            FilterExportService filterExportService,
                            @Value("${image.batch-size:50}") int batchSize) {
        this.tagRepository = tagRepository;
        this.userService = userService;
        this.filterRepository = filterRepository;
        this.filterExportService = filterExportService;
        this.batchSize = batchSize;
    }

    /**
     * Provides the CSRF token to the model.
     *
     * @param request The HTTP request.
     * @return The CSRF token.
     */
    @ModelAttribute("csrfToken")
    public String getCsrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return csrf != null ? csrf.getToken() : null;
    }

    /**
     * Provides the CSRF header name to the model.
     *
     * @param request The HTTP request.
     * @return The CSRF header name.
     */
    @ModelAttribute("csrfHeaderName")
    public String getCsrfHeaderName(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return csrf != null ? csrf.getHeaderName() : null;
    }

    /**
     * Displays the filters page.
     *
     * @param model          The model to add attributes to.
     * @param tags           A list of tags to filter by.
     * @param authentication The current authentication object.
     * @return The name of the filters view.
     */
    @GetMapping("/filters")
    @SuppressWarnings("SameReturnValue")
    public String filters(Model model, @RequestParam(value = "tag", required = false) List<String> tags, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));

        List<Filter> filters;
        if (user.isAdmin()) {
            filters = filterRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            filters = filterRepository.findByUserId(user.getId());
        }

        List<TagWithCounter> tagsWithCounters;
        if (user.isAdmin()) {
            tagsWithCounters = tagRepository.findAllTagsWithCounters();
        } else {
            tagsWithCounters = tagRepository.findTagsWithCountersByUserId(user.getId());
        }

        model.addAttribute("images", Collections.emptyList());
        model.addAttribute("tags", tagsWithCounters);
        model.addAttribute("filters", filters);
        model.addAttribute("batchSize", batchSize);

        long maxTagCount = tagsWithCounters.stream()
                .mapToLong(TagWithCounter::getCounter)
                .max()
                .orElse(0);
        model.addAttribute("maxTagCount", maxTagCount);

        if (tags != null && !tags.isEmpty()) {
            model.addAttribute("initialTags", tags);
        }
        return "filters/index";
    }

    /**
     * Exports images based on a set of filters.
     *
     * @param ids            The IDs of the filters to export.
     * @param authentication The current authentication object.
     * @param response       The HTTP response.
     * @return A {@link ResponseEntity} with a streaming body of the zip file.
     */
    @Transactional(readOnly = true)
    @GetMapping("/filters/export")
    public ResponseEntity<StreamingResponseBody> exportFilters(@RequestParam("ids") List<Long> ids,
                                                               Authentication authentication,
                                                               HttpServletResponse response) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tagfolio-images.zip\"");

        StreamingResponseBody stream = out -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
                filterExportService.exportFilters(ids, user, zipOutputStream);
            } catch (IOException e) {
            }
        };

        return new ResponseEntity<>(stream, HttpStatus.OK);
    }
}
