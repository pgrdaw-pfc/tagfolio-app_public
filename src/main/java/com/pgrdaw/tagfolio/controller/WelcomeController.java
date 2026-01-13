package com.pgrdaw.tagfolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.service.UserService;
import com.pgrdaw.tagfolio.service.util.ImageSortService;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import com.pgrdaw.tagfolio.service.util.SeedingStatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling the welcome page and other top-level pages.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
public class WelcomeController {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);

    private final UserService userService;
    @SuppressWarnings("FieldCanBeLocal")
    private final ImageSortService imageSortService;
    private final SeedingStatusService seedingStatusService;
    private final int batchSize;
    private final Environment environment;
    private final Map<String, String> sortableFields;
    private final ObjectMapper objectMapper;
    private final MetadataService metadataService;

    /**
     * Constructs a new WelcomeController.
     *
     * @param userService          The user service.
     * @param imageSortService     The image sort service.
     * @param seedingStatusService The seeding status service.
     * @param metadataService      The metadata service.
     * @param environment          The application environment.
     * @param objectMapper         The object mapper for JSON processing.
     * @param batchSize            The batch size for image loading.
     * @param sortableFields       A map of sortable fields.
     */
    @Autowired
    public WelcomeController(UserService userService,
                             ImageSortService imageSortService,
                             SeedingStatusService seedingStatusService,
                             MetadataService metadataService,
                             Environment environment,
                             ObjectMapper objectMapper,
                             @Value("${image.batch-size:50}") int batchSize,
                             @Value("#{${app.sortable-fields}}") Map<String, String> sortableFields) {
        this.userService = userService;
        this.imageSortService = imageSortService;
        this.seedingStatusService = seedingStatusService;
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.sortableFields = sortableFields;
        this.metadataService = metadataService;
    }

    /**
     * Displays the main page.
     *
     * @param model              The model to add attributes to.
     * @param authentication     The current authentication object.
     * @param view               The view type (e.g., "grid").
     * @param sort               The sort field.
     * @param direction          The sort direction.
     * @param request            The HTTP request.
     * @param redirectAttributes The redirect attributes.
     * @return The name of the view to render.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    @GetMapping("/")
    public String index(Model model, Authentication authentication,
                        @RequestParam(value = "view", defaultValue = "grid") String view,
                        @RequestParam(value = "sort", defaultValue = "Imported") String sort,
                        @RequestParam(value = "direction", defaultValue = "desc") String direction,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) throws JsonProcessingException {

        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("seed") && !seedingStatusService.isSeedingComplete()) {
            return "seeding";
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return "welcome";
        }
        User user = userService.findByEmail(authentication.getName()).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Your user account could not be found. Please log in again.");
            return "redirect:/login";
        }

        model.addAttribute("images", Collections.emptyList());
        model.addAttribute("batchSize", batchSize);
        model.addAttribute("currentView", view);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("sortableFieldsJson", objectMapper.writeValueAsString(sortableFields));
        model.addAttribute("sortableFields", sortableFields);
        model.addAttribute("displayMetadataMap", metadataService.getDisplayMetadataMap());
        model.addAttribute("displayMetadataMapJson", objectMapper.writeValueAsString(metadataService.getDisplayMetadataMap()));
        return "home/index";
    }

    /**
     * Displays the seeding status page.
     *
     * @return The name of the seeding view, or a redirect to the home page.
     */
    @GetMapping("/seeding")
    public String seeding() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (!activeProfiles.contains("seed") || seedingStatusService.isSeedingComplete()) {
            return "redirect:/";
        }
        return "seeding";
    }

    /**
     * Displays the license page.
     *
     * @return The name of the license view.
     */
    @GetMapping("/license")
    public String license() {
        return "license";
    }
}
