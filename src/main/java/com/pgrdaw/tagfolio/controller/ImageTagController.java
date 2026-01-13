package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.TagWithCounter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.TagRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.ImageSecurityService;
import com.pgrdaw.tagfolio.service.ImageService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for handling image tag-related requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/images")
public class ImageTagController {

    @Getter
    @Setter
    public static class TagsRequest {
        private List<Long> imageIds;
        private List<Long> baseImageIds;
    }

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ImageService imageService;
    private final ImageSecurityService imageSecurityService;

    /**
     * Constructs a new ImageTagController.
     *
     * @param imageRepository      The image repository.
     * @param userRepository       The user repository.
     * @param tagRepository        The tag repository.
     * @param imageService         The image service.
     * @param imageSecurityService The image security service.
     */
    @Autowired
    public ImageTagController(ImageRepository imageRepository,
                              UserRepository userRepository,
                              TagRepository tagRepository,
                              ImageService imageService,
                              ImageSecurityService imageSecurityService) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.imageService = imageService;
        this.imageSecurityService = imageSecurityService;
    }

    /**
     * Gets the tags for the selected images.
     *
     * @param tagsRequest    The request payload containing the image IDs.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a list of tags and their counts.
     */
    @PostMapping("/tags")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSelectedImagesTags(@RequestBody TagsRequest tagsRequest, Authentication authentication) {
        List<Long> imageIds = tagsRequest.getImageIds();
        List<Long> baseImageIds = tagsRequest.getBaseImageIds();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            List<Image> imagesToConsider;

            if (baseImageIds != null && !baseImageIds.isEmpty()) {
                if (imageIds != null && !imageIds.isEmpty()) {
                    imagesToConsider = imageRepository.findAllById(imageIds);
                } else {
                    imagesToConsider = imageRepository.findAllById(baseImageIds);
                }
            } else {
                return ResponseEntity.ok(Collections.emptyList());
            }

            return ResponseEntity.ok(calculateAndFormatTagCounts(imagesToConsider));
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in repository."));

        if (imageIds == null || imageIds.isEmpty()) {
            List<TagWithCounter> tagsWithCounters;
            if (user.isAdmin()) {
                tagsWithCounters = tagRepository.findAllTagsWithCounters();
            } else {
                tagsWithCounters = tagRepository.findTagsWithCountersByUserId(user.getId());
            }

            return ResponseEntity.ok(tagsWithCounters.stream()
                    .map(twc -> {
                        Map<String, Object> tagMap = new LinkedHashMap<>();
                        tagMap.put("name", twc.getTag().getName());
                        tagMap.put("counter", twc.getCounter());
                        return tagMap;
                    })
                    .sorted(Comparator
                            .comparing((Map<String, Object> map) -> (Long) map.get("counter"), Comparator.reverseOrder())
                            .thenComparing(map -> (String) map.get("name")))
                    .collect(Collectors.toList()));

        } else {
            List<Image> selectedImages = new ArrayList<>();
            for (Long id : imageIds) {
                Image image = imageRepository.findByIdWithUser(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
                if (!imageSecurityService.canRead(user, image)) {
                    throw new AccessDeniedException("You do not have permission to view tags for image with id: " + id);
                }
                selectedImages.add(image);
            }

            return ResponseEntity.ok(calculateAndFormatTagCounts(selectedImages));
        }
    }

    private List<Map<String, Object>> calculateAndFormatTagCounts(List<Image> images) {
        Map<Tag, Long> tagCounts = new HashMap<>();
        for (Image img : images) {
            for (Tag tag : img.getTags()) {
                tagCounts.merge(tag, 1L, Long::sum);
            }
        }

        return tagCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> tagMap = new LinkedHashMap<>();
                    tagMap.put("name", entry.getKey().getName());
                    tagMap.put("counter", entry.getValue());
                    return tagMap;
                })
                .sorted(Comparator
                        .comparing((Map<String, Object> map) -> (Long) map.get("counter"), Comparator.reverseOrder())
                        .thenComparing(map -> (String) map.get("name")))
                .collect(Collectors.toList());
    }

    /**
     * Adds a tag to a list of images.
     *
     * @param payload        The request payload containing the image IDs and tag name.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PostMapping("/tags/add")
    @ResponseBody
    public ResponseEntity<?> addTagToImages(@RequestBody Map<String, Object> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot add tags."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));

        List<?> rawImageIds = (List<?>) payload.get("imageIds");
        List<Long> imageIds = rawImageIds.stream()
                .map(id -> {
                    if (id instanceof String) {
                        return Long.parseLong((String) id);
                    } else if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    }
                    throw new IllegalArgumentException("Unexpected type for image ID: " + id.getClass());
                })
                .toList();

        String tagName = (String) payload.get("tagName");

        if (imageIds.isEmpty() || tagName == null || tagName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request parameters."));
        }

        for (Long id : imageIds) {
            Image image = imageRepository.findByIdWithUser(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + id));
            if (!imageSecurityService.canUpdate(user, image)) {
                throw new AccessDeniedException("You do not have permission to update image with id: " + id);
            }

            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        return tagRepository.save(newTag);
                    });

            image.getTags().add(tag);
            imageRepository.save(image);
        }

        return ResponseEntity.ok(Map.of("message", "Tag added to images successfully."));
    }

    /**
     * Removes selected tags from a list of images.
     *
     * @param payload        The request payload containing the image IDs and tag names.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     */
    @PostMapping("/tags/removeSelected")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> removeSelectedTagsFromImages(@RequestBody Map<String, Object> payload,
                                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot remove tags."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));

        List<?> rawImageIds = (List<?>) payload.get("imageIds");
        List<Long> imageIds = rawImageIds.stream()
                .map(id -> {
                    if (id instanceof String) {
                        return Long.parseLong((String) id);
                    } else if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    }
                    throw new IllegalArgumentException("Unexpected type for image ID: " + id.getClass());
                })
                .toList();

        List<String> tagNames = (List<String>) payload.get("tagNames");

        if (imageIds.isEmpty() || tagNames == null || tagNames.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request parameters."));
        }

        List<Tag> tagsToRemove = tagRepository.findByNameIn(new java.util.HashSet<>(tagNames));

        if (tagsToRemove.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No matching tags found to remove."));
        }

        for (Long imageId : imageIds) {
            Image image = imageRepository.findByIdWithUser(imageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found with id: " + imageId));

            if (!imageSecurityService.canUpdate(user, image)) {
                throw new AccessDeniedException("You do not have permission to update image with id: " + imageId);
            }

            tagsToRemove.forEach(image.getTags()::remove);
            imageRepository.save(image);
        }

        for (Tag tag : tagsToRemove) {
            boolean isTagUsed = imageRepository.existsByTagsContaining(tag);
            if (!isTagUsed) {
                tagRepository.delete(tag);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Tags removed from images successfully."));
    }

    /**
     * Deletes tags globally from all images.
     *
     * @param tagNames       The list of tag names to delete.
     * @param authentication The current authentication object.
     * @return A {@link ResponseEntity} with a success message.
     */
    @Transactional
    @DeleteMapping("/tags/delete")
    @ResponseBody
    public ResponseEntity<?> deleteTagsGlobally(@RequestBody List<String> tagNames, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Anonymous users cannot delete tags globally."));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
        imageService.deleteTagsGlobally(tagNames, user);
        return ResponseEntity.ok(Map.of("message", "Tags deleted successfully."));
    }
}
