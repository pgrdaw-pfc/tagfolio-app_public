package com.pgrdaw.tagfolio.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.ImageSecurityService;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling image metadata requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/images")
public class ImageMetadataController {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ImageSecurityService imageSecurityService;
    private final ObjectMapper objectMapper;
    @SuppressWarnings("unused")
    private final MetadataService metadataService;
    private final List<String> displayMetadataKeys;

    /**
     * Constructs a new ImageMetadataController.
     *
     * @param imageRepository      The image repository.
     * @param userRepository       The user repository.
     * @param imageSecurityService The image security service.
     * @param objectMapper         The object mapper for JSON processing.
     * @param metadataService      The metadata service.
     */
    @Autowired
    public ImageMetadataController(ImageRepository imageRepository,
                                   UserRepository userRepository,
                                   ImageSecurityService imageSecurityService,
                                   ObjectMapper objectMapper,
                                   MetadataService metadataService) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.imageSecurityService = imageSecurityService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.displayMetadataKeys = metadataService.getDisplayMetadataKeys();
    }

    /**
     * Gets the metadata for a single image.
     *
     * @param id             The ID of the image.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the image metadata.
     * @throws IOException if an I/O error occurs.
     */
    @GetMapping("/{id}/metadata")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageMetadata(@PathVariable Long id, Authentication authentication) throws IOException {
        Image image = imageRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.ok(getFilteredMetadata(image));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        if (!imageSecurityService.canRead(user, image)) {
            throw new AccessDeniedException("You do not have permission to view this image's metadata.");
        }

        return ResponseEntity.ok(getFilteredMetadata(image));
    }

    /**
     * Gets the metadata for a list of selected images.
     *
     * @param imageIds       The list of image IDs.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of image metadata.
     * @throws IOException if an I/O error occurs.
     */
    @PostMapping("/metadata")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSelectedImagesMetadata(@RequestBody List<Long> imageIds, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Map<String, Object>> allMetadata = new ArrayList<>();
            for (Long id : imageIds) {
                Image image = imageRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
                allMetadata.add(getFilteredMetadata(image));
            }
            return ResponseEntity.ok(allMetadata);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        List<Map<String, Object>> allMetadata = new ArrayList<>();
        for (Long id : imageIds) {
            Image image = imageRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));
            if (!imageSecurityService.canRead(user, image)) {
                throw new AccessDeniedException("You do not have permission to view this image's metadata.");
            }
            allMetadata.add(getFilteredMetadata(image));
        }
        return ResponseEntity.ok(allMetadata);
    }

    /**
     * Gets the Exif data for a list of selected images.
     *
     * @param imageIds       The list of image IDs.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of Exif data.
     * @throws IOException if an I/O error occurs.
     */
    @PostMapping("/exif")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getExifDataForSelectedImages(@RequestBody List<Long> imageIds, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Map<String, Object>> allExifData = new ArrayList<>();
            for (Long id : imageIds) {
                Image image = imageRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
                if (image.getExiftool() != null && !image.getExiftool().isEmpty()) {
                    Map<String, Object> rawExifData = objectMapper.readValue(image.getExiftool(), new TypeReference<>() {
                    });
                    allExifData.add(rawExifData);
                }
            }
            return ResponseEntity.ok(allExifData);
        }

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        List<Map<String, Object>> allExifData = new ArrayList<>();
        for (Long id : imageIds) {
            Image image = imageRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));
            if (!imageSecurityService.canRead(user, image)) {
                throw new AccessDeniedException("You do not have permission to view this image's EXIF data.");
            }
            if (image.getExiftool() != null && !image.getExiftool().isEmpty()) {
                Map<String, Object> rawExifData = objectMapper.readValue(image.getExiftool(), new TypeReference<>() {
                });
                allExifData.add(rawExifData);
            }
        }
        return ResponseEntity.ok(allExifData);
    }

    private Map<String, Object> filterMetadataForDisplay(Map<String, Object> rawMetadata) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : displayMetadataKeys) {
            if (rawMetadata.containsKey(key)) {
                Object value = rawMetadata.get(key);
                filtered.put(key, String.valueOf(value));
            }
        }
        return filtered;
    }

    private Map<String, Object> getFilteredMetadata(Image image) throws IOException {
        Map<String, Object> rawMetadata;
        if (image.getExiftool() != null && !image.getExiftool().isEmpty()) {
            rawMetadata = objectMapper.readValue(image.getExiftool(), new TypeReference<>() {
            });
        } else {
            rawMetadata = Collections.emptyMap();
        }
        return filterMetadataForDisplay(rawMetadata);
    }
}
