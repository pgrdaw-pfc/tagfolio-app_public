package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.ImageSecurityService;
import com.pgrdaw.tagfolio.service.ImageService;
import com.pgrdaw.tagfolio.service.SharedFilterService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Controller for handling image download requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/images")
public class ImageDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(ImageDownloadController.class);

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ImageSecurityService imageSecurityService;
    private final SharedFilterService sharedFilterService;

    /**
     * Constructs a new ImageDownloadController.
     *
     * @param imageRepository      The image repository.
     * @param userRepository       The user repository.
     * @param imageService         The image service.
     * @param imageSecurityService The image security service.
     * @param sharedFilterService  The shared filter service.
     */
    @Autowired
    public ImageDownloadController(ImageRepository imageRepository,
                                   UserRepository userRepository,
                                   ImageService imageService,
                                   ImageSecurityService imageSecurityService,
                                   SharedFilterService sharedFilterService) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.imageService = imageService;
        this.imageSecurityService = imageSecurityService;
        this.sharedFilterService = sharedFilterService;
    }

    /**
     * Downloads a zip file of images.
     *
     * @param ids            The IDs of the images to download.
     * @param token          The shared filter token, if any.
     * @param authentication The current authentication object.
     * @param response       The HTTP response.
     * @return A {@link ResponseEntity} with a streaming body of the zip file.
     */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("ids") List<Long> ids,
                                                          @RequestParam(value = "token", required = false) String token,
                                                          Authentication authentication,
                                                          HttpServletResponse response) {

        boolean isAnonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;

        if (!isAnonymous) {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            for (Long id : ids) {
                Image image = imageRepository.findByIdWithUser(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
                if (!imageSecurityService.canRead(user, image)) {
                    throw new AccessDeniedException("You do not have permission to download image with id: " + id);
                }
            }
        } else {
            if (token == null || token.isEmpty()) {
                logger.warn("Anonymous user attempted to export images without a shared filter token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            for (Long id : ids) {
                if (!sharedFilterService.isImageInSharedFilter(id, token)) {
                    logger.warn("Anonymous user attempted to export image {} not part of shared filter with token {}.", id, token);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tagfolio-images.zip\"");

        StreamingResponseBody stream = out -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
                imageService.zipImages(ids, zipOutputStream);
            } catch (IOException e) {
                logger.error("Error while zipping images for download", e);
            }
        };

        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    /**
     * Serves a thumbnail image.
     *
     * @param id             The ID of the image.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with the thumbnail resource.
     */
    @GetMapping("/thumbnail/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveThumbnailImage(@PathVariable Long id, Authentication authentication) {
        Image image = imageRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));

        if (!(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)) {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            if (!imageSecurityService.canRead(user, image)) {
                throw new AccessDeniedException("You do not have permission to view this image's thumbnail.");
            }
        }

        return serveImageResource(id, image.getThumbnailFileName(), true, image.getUser().getId());
    }

    private ResponseEntity<Resource> serveImageResource(Long id, String fileName, boolean isThumbnail, Long userId) {
        try {
            Resource resource = isThumbnail ? imageService.loadThumbnailAsResource(fileName, userId) : imageService.loadImageAsResource(fileName, userId);

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                logger.error("{} file not found or not readable: {}", isThumbnail ? "Thumbnail" : "Original", resource.getURI());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, (isThumbnail ? "Thumbnail" : "Original image") + " not found for image id: " + id);
            }
        } catch (IOException e) {
            logger.error("Error serving {} for image id {}: {}", isThumbnail ? "thumbnail" : "original image", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error serving " + (isThumbnail ? "thumbnail" : "original image") + " for image id: " + id, e);
        }
    }
}
