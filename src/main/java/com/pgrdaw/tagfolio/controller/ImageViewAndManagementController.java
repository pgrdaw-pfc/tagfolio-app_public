package com.pgrdaw.tagfolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.SharedFilter;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.TagRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.ImageSecurityService;
import com.pgrdaw.tagfolio.service.ImageService;
import com.pgrdaw.tagfolio.service.FilterExpressionEvaluator;
import com.pgrdaw.tagfolio.service.SharedFilterService;
import com.pgrdaw.tagfolio.service.util.ImageSortService;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for handling image view and management requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/images")
public class ImageViewAndManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ImageViewAndManagementController.class);

    @Getter
    @Setter
    public static class FilterRequest {
        private List<Map<String, String>> expression;
        private List<Long> baseImageIds;
        private String sort;
        private String direction;
    }

    private final int batchSize;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ImageService imageService;
    private final ImageSecurityService imageSecurityService;
    private final ObjectMapper objectMapper;
    private final FilterExpressionEvaluator filterExpressionEvaluator;
    private final MetadataService metadataService;
    private final ImageSortService imageSortService;
    private final Map<String, String> sortableFields;
    private final SharedFilterService sharedFilterService;

    /**
     * Constructs a new ImageViewAndManagementController.
     *
     * @param imageRepository         The image repository.
     * @param userRepository          The user repository.
     * @param tagRepository           The tag repository.
     * @param imageService            The image service.
     * @param imageSecurityService    The image security service.
     * @param objectMapper            The object mapper for JSON processing.
     * @param filterExpressionEvaluator The filter expression evaluator.
     * @param metadataService         The metadata service.
     * @param imageSortService        The image sort service.
     * @param sharedFilterService     The shared filter service.
     * @param batchSize               The batch size for image loading.
     * @param sortableFields          A map of sortable fields.
     */
    @Autowired
    public ImageViewAndManagementController(ImageRepository imageRepository,
                                            UserRepository userRepository,
                                            TagRepository tagRepository,
                                            ImageService imageService,
                                            ImageSecurityService imageSecurityService,
                                            ObjectMapper objectMapper,
                                            FilterExpressionEvaluator filterExpressionEvaluator,
                                            MetadataService metadataService,
                                            ImageSortService imageSortService,
                                            SharedFilterService sharedFilterService,
                                            @Value("${image.batch-size:50}") int batchSize,
                                            @Value("#{${app.sortable-fields}}") Map<String, String> sortableFields) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.imageService = imageService;
        this.imageSecurityService = imageSecurityService;
        this.objectMapper = objectMapper;
        this.filterExpressionEvaluator = filterExpressionEvaluator;
        this.metadataService = metadataService;
        this.imageSortService = imageSortService;
        this.batchSize = batchSize;
        this.sortableFields = sortableFields;
        this.sharedFilterService = sharedFilterService;
    }

    /**
     * Displays the details of a single image.
     *
     * @param id             The ID of the image.
     * @param model          The model to add attributes to.
     * @param authentication The current authentication object.
     * @param session        The HTTP session.
     * @return The name of the image detail view.
     * @throws IOException if an I/O error occurs.
     */
    @GetMapping("/{id}")
    @SuppressWarnings("SameReturnValue")
    public String show(@PathVariable Long id, Model model, Authentication authentication, HttpSession session) throws IOException {
        Image image = imageRepository.findByIdWithUser(id)
                .orElseThrow(() -> new NoSuchElementException("Image not found with id: " + id));

        User currentUser = null;
        String sharedFilterToken = null;
        List<Long> navigationImageIds = null;

        if (!(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)) {
            currentUser = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            if (!imageSecurityService.canRead(currentUser, image)) {
                throw new AccessDeniedException("You do not have permission to view this image.");
            }
            navigationImageIds = (List<Long>) session.getAttribute("sortedImageIds");
            if (navigationImageIds == null) {
                navigationImageIds = imageService.getImagesForUser(currentUser).stream().map(Image::getId).collect(Collectors.toList());
            }

        } else {
            List<Long> sharedImageIds = (List<Long>) session.getAttribute("sharedImageIds");
            if (sharedImageIds == null || !sharedImageIds.contains(id)) {
                throw new AccessDeniedException("You do not have permission to view this image.");
            }
            sharedFilterToken = (String) session.getAttribute("sharedFilterToken");
            navigationImageIds = (List<Long>) session.getAttribute("sortedImageIds");
        }

        Map<String, Object> rawMetadata = metadataService.getRawMetadata(image);
        String prettyPrintedExif = getPrettyPrintedExif(rawMetadata);

        List<Tag> sortedTags = getSortedTags(image);

        if (navigationImageIds != null) {
            int currentIndex = navigationImageIds.indexOf(id);
            if (currentIndex != -1) {
                if (currentIndex > 0) {
                    model.addAttribute("prevImageId", navigationImageIds.get(currentIndex - 1));
                }
                if (currentIndex < navigationImageIds.size() - 1) {
                    model.addAttribute("nextImageId", navigationImageIds.get(currentIndex + 1));
                }
            }
        }


        model.addAttribute("image", image);
        model.addAttribute("rawExiftoolData", prettyPrintedExif);
        model.addAttribute("sortedTags", sortedTags);
        model.addAttribute("displayMetadataMap", metadataService.getDisplayMetadataMap());
        model.addAttribute("displayMetadataMapJson", objectMapper.writeValueAsString(metadataService.getDisplayMetadataMap()));
        model.addAttribute("isImageViewPage", true);
        
        if (sharedFilterToken != null) {
            model.addAttribute("galleryUrl", "/shared/filter/" + sharedFilterToken);
        } else {
            model.addAttribute("galleryUrl", "/");
        }

        return "images/show";
    }

    /**
     * Serves the original image file.
     *
     * @param id             The ID of the image.
     * @param authentication The current authentication object.
     * @param session        The HTTP session.
     * @return A {@link ResponseEntity} with the image resource.
     * @throws IOException if an I/O error occurs.
     */
    @GetMapping("/original/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveOriginalImage(@PathVariable Long id, Authentication authentication, HttpSession session) throws IOException {
        Image image = imageRepository.findByIdWithUser(id)
                .orElseThrow(() -> new NoSuchElementException("Image not found with id: " + id));

        if (!(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)) {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            if (!imageSecurityService.canRead(user, image)) {
                throw new AccessDeniedException("You do not have permission to view this image.");
            }
        } else {
            List<Long> sharedImageIds = (List<Long>) session.getAttribute("sharedImageIds");
            if (sharedImageIds == null || !sharedImageIds.contains(id)) {
                throw new AccessDeniedException("You do not have permission to view this image.");
            }
        }

        Resource resource = imageService.loadImageAsResource(image.getOriginalFileName(), image.getUser().getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getOriginalFileName() + "\"")
                .body(resource);
    }

    private String getPrettyPrintedExif(Map<String, Object> rawMetadata) throws JsonProcessingException {
        ObjectMapper prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        return prettyMapper.writeValueAsString(rawMetadata);
    }

    private List<Tag> getSortedTags(Image image) {
        Set<Tag> imageTags = image.getTags();

        if (imageTags.isEmpty()) {
            return Collections.emptyList();
        }

        return imageTags.stream()
                .sorted(Comparator.comparing(Tag::getName))
                .collect(Collectors.toList());
    }

    /**
     * Gets a paginated list of images.
     *
     * @param page           The page number.
     * @param size           The page size.
     * @param sort           The sort field.
     * @param direction      The sort direction.
     * @param authentication The current authentication object.
     * @param session        The HTTP session.
     * @return A {@link ResponseEntity} with a list of images.
     */
    @GetMapping("/paginated")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<Image>> getPaginatedImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String size,
            @RequestParam(defaultValue = "Imported") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication,
            HttpSession session) {

        session.setAttribute("currentSort", sort);
        session.setAttribute("currentDirection", direction);

        List<Image> allImages;

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Long> sharedImageIds = (List<Long>) session.getAttribute("sharedImageIds");
            if (sharedImageIds == null || sharedImageIds.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            allImages = imageRepository.findAllById(sharedImageIds);
        } else {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            allImages = imageService.getImagesForUser(user);
        }

        allImages.sort(imageSortService.getImageComparator(sort, direction));
        
        session.setAttribute("sortedImageIds", allImages.stream().map(Image::getId).collect(Collectors.toList()));

        int effectiveSize = (size == null || "null".equalsIgnoreCase(size)) ? this.batchSize : Integer.parseInt(size);
        int start = page * effectiveSize;
        if (start >= allImages.size()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        int end = Math.min(start + effectiveSize, allImages.size());
        return ResponseEntity.ok(allImages.subList(start, end));
    }

    /**
     * Filters a list of images.
     *
     * @param filterRequest  The filter request.
     * @param authentication The current authentication object.
     * @param session        The HTTP session.
     * @return A {@link ResponseEntity} with a map of matching and non-matching image IDs.
     */
    @PostMapping("/filter")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, List<Long>>> filterImages(@RequestBody FilterRequest filterRequest, Authentication authentication, HttpSession session) {
        List<Image> imagesToFilter;
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Long> sharedImageIds = (List<Long>) session.getAttribute("sharedImageIds");
            if (sharedImageIds == null || sharedImageIds.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyMap());
            }
            imagesToFilter = imageRepository.findAllById(sharedImageIds);
        } else {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            imagesToFilter = (filterRequest.getBaseImageIds() != null && !filterRequest.getBaseImageIds().isEmpty())
                    ? imageRepository.findAllById(filterRequest.getBaseImageIds())
                    : imageService.getImagesForUser(user);
        }
        
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
             User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
             imagesToFilter = imagesToFilter.stream()
                     .filter(image -> imageSecurityService.canRead(user, image))
                     .collect(Collectors.toList());
        }

        List<Long> matchingImageIds = filterExpressionEvaluator.evaluate(imagesToFilter, filterRequest.getExpression());
        Set<Long> matchingImageIdsSet = new HashSet<>(matchingImageIds);
        List<Long> nonMatchingImageIds = imagesToFilter.stream()
                .map(Image::getId)
                .filter(id -> !matchingImageIdsSet.contains(id))
                .collect(Collectors.toList());

        List<Image> sortedMatchingImages = imageRepository.findAllById(matchingImageIds).stream()
                .sorted(imageSortService.getImageComparator(filterRequest.getSort(), filterRequest.getDirection()))
                .toList();
        List<Image> sortedNonMatchingImages = imageRepository.findAllById(nonMatchingImageIds).stream()
                .sorted(imageSortService.getImageComparator(filterRequest.getSort(), filterRequest.getDirection()))
                .toList();
                
        List<Long> sortedImageIds = new ArrayList<>();
        sortedImageIds.addAll(sortedMatchingImages.stream().map(Image::getId).collect(Collectors.toList()));
        sortedImageIds.addAll(sortedNonMatchingImages.stream().map(Image::getId).collect(Collectors.toList()));
        session.setAttribute("sortedImageIds", sortedImageIds);

        Map<String, List<Long>> response = new HashMap<>();
        response.put("matchingImageIds", sortedMatchingImages.stream().map(Image::getId).collect(Collectors.toList()));
        response.put("nonMatchingImageIds", sortedNonMatchingImages.stream().map(Image::getId).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a list of images by their IDs.
     *
     * @param imageIds       The list of image IDs.
     * @param sort           The sort field.
     * @param direction      The sort direction.
     * @param authentication The current authentication object.
     * @param session        The HTTP session.
     * @return A {@link ResponseEntity} with a list of images.
     */
    @PostMapping("/by-ids")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<Image>> getImagesByIds(@RequestBody List<Long> imageIds,
                                                      @RequestParam(defaultValue = "Imported") String sort,
                                                      @RequestParam(defaultValue = "desc") String direction,
                                                      Authentication authentication,
                                                      HttpSession session) {
        List<Image> images;
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Long> sharedImageIds = (List<Long>) session.getAttribute("sharedImageIds");
            if (sharedImageIds == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
            }
            List<Long> allowedImageIds = imageIds.stream().filter(sharedImageIds::contains).collect(Collectors.toList());
            images = imageRepository.findAllById(allowedImageIds);
        } else {
            User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
            images = imageRepository.findAllById(imageIds);
            for (Image image : images) {
                if (!imageSecurityService.canRead(user, image)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
                }
            }
        }
        images.sort(imageSortService.getImageComparator(sort, direction));
        session.setAttribute("sortedImageIds", images.stream().map(Image::getId).collect(Collectors.toList()));
        return ResponseEntity.ok(images);
    }

    /**
     * Updates an image.
     *
     * @param id             The ID of the image to update.
     * @param authentication The current authentication object.
     * @return A redirect to the image detail page.
     * @throws IOException if an I/O error occurs.
     */
    @PutMapping("/{id}")
    public String update(@PathVariable Long id, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Anonymous users cannot update images.");
        }
        Image image = imageRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        if (!imageSecurityService.canUpdate(user, image)) {
            throw new AccessDeniedException("You do not have permission to update this image.");
        }
        imageRepository.save(image);
        return "redirect:/images/" + id;
    }

    /**
     * Deletes multiple images.
     *
     * @param imageIds       The list of image IDs to delete.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    @DeleteMapping
    @ResponseBody
    public ResponseEntity<?> deleteMultipleImages(@RequestBody List<Long> imageIds, Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Anonymous users cannot delete images.");
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        for (Long id : imageIds) {
            Image image = imageRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
            if (!imageSecurityService.canDelete(user, image)) {
                throw new AccessDeniedException("You do not have permission to delete image with id: " + id);
            }
            imageService.deleteImage(image);
        }
        return ResponseEntity.ok(Map.of("message", "Images deleted successfully."));
    }

    /**
     * Regenerates thumbnails for all images of the authenticated user.
     *
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     * @throws IOException if an I/O error occurs.
     */
    @PostMapping("/regenerate-thumbnails")
    @ResponseBody
    public ResponseEntity<?> regenerateThumbnails(Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Anonymous users cannot regenerate thumbnails.");
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        imageService.regenerateThumbnailsForUser(user);
        return ResponseEntity.ok(Map.of("message", "Thumbnails regenerated successfully."));
    }
}
