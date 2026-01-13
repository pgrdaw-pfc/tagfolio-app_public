package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling image upload requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/images")
public class ImageUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadController.class);

    private final ImageService imageService;
    private final UserRepository userRepository;

    /**
     * Constructs a new ImageUploadController.
     *
     * @param imageService   The image service.
     * @param userRepository The user repository.
     */
    @Autowired
    public ImageUploadController(ImageService imageService, UserRepository userRepository) {
        this.imageService = imageService;
        this.userRepository = userRepository;
    }

    /**
     * Stores an image uploaded via a form.
     *
     * @param file               The uploaded image file.
     * @param authentication     The current authentication object.
     * @param redirectAttributes The redirect attributes.
     * @return A redirect to the appropriate page.
     */
    @PostMapping("/create")
    public String storeForm(@RequestParam("image") MultipartFile file,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            redirectAttributes.addFlashAttribute("error", "Anonymous users cannot upload images.");
            return "redirect:/login";
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        try {
            imageService.processAndSaveFile(file, user);
            redirectAttributes.addFlashAttribute("success", "Image uploaded and metadata synced!");
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Stores images uploaded via an AJAX request.
     *
     * @param files          An array of uploaded image files.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the upload results.
     */
    @PostMapping
    @ResponseBody
    public ResponseEntity<?> storeAjax(@RequestParam("images[]") MultipartFile[] files,
                                       Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot upload images."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));

        List<ImageService.UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                results.add(imageService.processAndSaveFile(file, user));
            } catch (IOException e) {
                logger.error("Failed to upload file via AJAX: {}", e.getMessage(), e);
            }
        }

        Map<String, List<?>> response = new HashMap<>();
        response.put("uploaded", results.stream().filter(r -> "UPLOADED".equals(r.getStatus())).map(ImageService.UploadResult::getImage).collect(Collectors.toList()));
        response.put("skipped", results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).map(ImageService.UploadResult::getImage).collect(Collectors.toList()));
        response.put("conflicted", results.stream().filter(r -> "CONFLICT".equals(r.getStatus())).map(ImageService.UploadResult::getConflict).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Overwrites an existing image.
     *
     * @param file           The new image file.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the result of the operation.
     */
    @PostMapping("/overwrite")
    @ResponseBody
    public ResponseEntity<?> overwrite(@RequestParam("image") MultipartFile file,
                                       Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot overwrite images."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));

        try {
            Image overwrittenImage = imageService.overwriteImage(file, user);
            return ResponseEntity.ok(Map.of("message", "Image overwritten successfully.", "image", overwrittenImage));
        } catch (IOException e) {
            logger.error("Failed to overwrite file via AJAX: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to overwrite image."));
        }
    }

    /**
     * Synchronizes the database with the user's image files.
     *
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the synchronization results.
     */
    @PostMapping("/sync-database")
    @ResponseBody
    public ResponseEntity<?> syncDatabase(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot sync the database."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));

        try {
            List<ImageService.UploadResult> results = imageService.syncUserImages(user);
            Map<String, List<?>> response = new HashMap<>();
            response.put("uploaded", results.stream().filter(r -> "UPLOADED".equals(r.getStatus())).map(ImageService.UploadResult::getImage).collect(Collectors.toList()));
            response.put("skipped", results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).map(ImageService.UploadResult::getImage).collect(Collectors.toList()));
            response.put("conflicted", results.stream().filter(r -> "CONFLICT".equals(r.getStatus())).map(ImageService.UploadResult::getConflict).collect(Collectors.toList()));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error during database sync for user {}: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }
}
