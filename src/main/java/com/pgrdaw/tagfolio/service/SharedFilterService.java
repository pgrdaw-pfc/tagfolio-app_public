package com.pgrdaw.tagfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.SharedFilter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.FilterRepository;
import com.pgrdaw.tagfolio.repository.SharedFilterRepository;
import com.pgrdaw.tagfolio.service.util.HashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A service for managing shared filters.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class SharedFilterService {

    private final SharedFilterRepository sharedFilterRepository;
    private final FilterRepository filterRepository;
    private final FilterExpressionEvaluator filterExpressionEvaluator;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;
    private final HashService hashService;
    private final String appBaseUrl;

    /**
     * Constructs a new SharedFilterService.
     *
     * @param sharedFilterRepository    The shared filter repository.
     * @param filterRepository          The filter repository.
     * @param filterExpressionEvaluator The filter expression evaluator.
     * @param imageService              The image service.
     * @param objectMapper              The object mapper for JSON processing.
     * @param hashService               The hash service.
     * @param appBaseUrl                The base URL of the application.
     */
    public SharedFilterService(SharedFilterRepository sharedFilterRepository,
                               FilterRepository filterRepository,
                               FilterExpressionEvaluator filterExpressionEvaluator,
                               ImageService imageService,
                               ObjectMapper objectMapper,
                               HashService hashService,
                               @Value("${app.base-url}") String appBaseUrl) {
        this.sharedFilterRepository = sharedFilterRepository;
        this.filterRepository = filterRepository;
        this.filterExpressionEvaluator = filterExpressionEvaluator;
        this.imageService = imageService;
        this.objectMapper = objectMapper;
        this.hashService = hashService;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Creates a shareable link for a filter.
     *
     * @param filterId The ID of the filter to share.
     * @return The shareable link URL.
     * @throws FilterServiceException if the filter is not found.
     */
    @Transactional
    public String createShareableLink(Long filterId) {
        Filter filter = filterRepository.findById(filterId)
                .orElseThrow(() -> new FilterServiceException("Filter not found with id: " + filterId));

        String filterExpressionJson = filter.getExpression();
        String contentHash = hashService.calculateSha256Hash(filterExpressionJson);

        Optional<SharedFilter> existingSharedFilter = sharedFilterRepository.findByContentHash(contentHash);

        if (existingSharedFilter.isPresent()) {
            return getShareableLinkUrl(existingSharedFilter.get().getToken());
        } else {
            String token = UUID.randomUUID().toString();
            SharedFilter sharedFilter = new SharedFilter(token, contentHash, filter, LocalDateTime.now());
            sharedFilterRepository.save(sharedFilter);
            return getShareableLinkUrl(token);
        }
    }

    /**
     * Retrieves a SharedFilter by its unique token.
     *
     * @param token The unique token of the shared filter.
     * @return An Optional containing the SharedFilter if found, empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<SharedFilter> getSharedFilterByToken(String token) {
        return sharedFilterRepository.findByToken(token);
    }

    /**
     * Retrieves a SharedFilter by its filter ID.
     *
     * @param filterId The ID of the filter.
     * @return An Optional containing the SharedFilter if found, empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<SharedFilter> getSharedFilterByFilterId(Long filterId) {
        return sharedFilterRepository.findByFilterId(filterId);
    }

    /**
     * Checks if an image is included in a shared filter.
     *
     * @param imageId The ID of the image.
     * @param token   The token of the shared filter.
     * @return True if the image is in the shared filter, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isImageInSharedFilter(Long imageId, String token) {
        Optional<SharedFilter> sharedFilterOptional = sharedFilterRepository.findByToken(token);
        if (sharedFilterOptional.isEmpty()) {
            return false;
        }

        SharedFilter sharedFilter = sharedFilterOptional.get();
        Filter originalFilter = sharedFilter.getFilter();
        User filterCreator = originalFilter.getUser();
        List<Image> allCreatorImages = imageService.getImagesForUser(filterCreator);

        try {
            List<Map<String, String>> filterExpression = filterExpressionEvaluator.parseExpressionJson(originalFilter.getExpression());
            List<Long> matchingImageIds = filterExpressionEvaluator.evaluate(allCreatorImages, filterExpression);
            return matchingImageIds.contains(imageId);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Constructs the full shareable URL for a given filter token.
     *
     * @param token The unique token of the shared filter.
     * @return The full shareable URL.
     */
    public String getShareableLinkUrl(String token) {
        return appBaseUrl + "/shared/filter/" + token;
    }
}
