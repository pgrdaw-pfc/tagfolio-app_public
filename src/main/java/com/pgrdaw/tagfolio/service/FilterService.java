package com.pgrdaw.tagfolio.service;

import com.pgrdaw.tagfolio.dto.FilterResponse;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.SharedFilter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.FilterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A service for managing filters.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class FilterService {

    private final FilterRepository filterRepository;
    private final ImageService imageService;
    private final FilterExpressionEvaluator filterExpressionEvaluator;
    private final SharedFilterService sharedFilterService;

    /**
     * Constructs a new FilterService.
     *
     * @param filterRepository        The filter repository.
     * @param imageService            The image service.
     * @param filterExpressionEvaluator The filter expression evaluator.
     * @param sharedFilterService     The shared filter service.
     */
    public FilterService(FilterRepository filterRepository,
                         ImageService imageService,
                         FilterExpressionEvaluator filterExpressionEvaluator,
                         SharedFilterService sharedFilterService) {
        this.filterRepository = filterRepository;
        this.imageService = imageService;
        this.filterExpressionEvaluator = filterExpressionEvaluator;
        this.sharedFilterService = sharedFilterService;
    }

    /**
     * Saves a filter.
     *
     * @param filterName     The name of the filter.
     * @param expressionJson The filter expression as a JSON string.
     * @param user           The user who created the filter.
     * @return The saved filter.
     * @throws FilterServiceException if the filter name or expression is empty.
     */
    @Transactional
    public Filter saveFilter(String filterName, String expressionJson, User user) {
        if (filterName == null || filterName.trim().isEmpty()) {
            throw new FilterServiceException("Filter name is required.");
        }
        if (expressionJson == null || expressionJson.trim().isEmpty()) {
            throw new FilterServiceException("Filter expression is required.");
        }

        Optional<Filter> existingFilter = filterRepository.findByUserAndExpression(user, expressionJson);
        if (existingFilter.isPresent()) {
            Filter filter = existingFilter.get();
            if (!filter.getName().equals(filterName)) {
                filter.setName(filterName);
                return filterRepository.save(filter);
            }
            return filter;
        } else {
            Filter newFilter = new Filter(filterName, expressionJson, user);
            return filterRepository.save(newFilter);
        }
    }

    /**
     * Gets a list of saved filters for a user.
     *
     * @param user The user.
     * @return A list of filter responses.
     */
    @Transactional(readOnly = true)
    public List<FilterResponse> getSavedFilters(User user) {
        List<Filter> filters;
        if (user.isAdmin()) {
            filters = filterRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            filters = filterRepository.findByUserOrderByIdDesc(user);
        }

        return filters.stream()
                .map(filter -> {
                    Optional<SharedFilter> sharedFilterOptional = sharedFilterService.getSharedFilterByFilterId(filter.getId());
                    boolean isShared = sharedFilterOptional.isPresent();
                    String shareableLink = isShared ? sharedFilterService.getShareableLinkUrl(sharedFilterOptional.get().getToken()) : null;
                    return new FilterResponse(filter.getId().toString(), filter.getName(), filter.getExpression(), isShared, shareableLink);
                })
                .collect(Collectors.toList());
    }

    /**
     * Evaluates a filter expression.
     *
     * @param expression        The filter expression.
     * @param sharedFilterToken The shared filter token, if any.
     * @param user              The user performing the evaluation.
     * @return A list of matching image IDs.
     */
    @Transactional(readOnly = true)
    public List<Long> evaluateFilter(List<Map<String, String>> expression, String sharedFilterToken, User user) {
        List<Image> imagesToFilter;

        if (user != null) {
            imagesToFilter = imageService.getImagesForUser(user);
        } else if (sharedFilterToken != null) {
            Optional<SharedFilter> sharedFilterOptional = sharedFilterService.getSharedFilterByToken(sharedFilterToken);
            if (sharedFilterOptional.isEmpty()) {
                return List.of();
            }
            SharedFilter sharedFilter = sharedFilterOptional.get();
            User filterCreator = sharedFilter.getFilter().getUser();
            imagesToFilter = imageService.getImagesForUser(filterCreator);
        } else {
            return List.of();
        }

        return filterExpressionEvaluator.evaluate(imagesToFilter, expression);
    }

    /**
     * Generates a shareable link for a filter.
     *
     * @param filterId The ID of the filter.
     * @param user     The user generating the link.
     * @return The shareable link.
     * @throws FilterServiceException if the filter is not found or not owned by the user.
     */
    @Transactional
    public String generateShareableLink(Long filterId, User user) {
        Optional<Filter> filterOptional = filterRepository.findById(filterId);
        if (filterOptional.isEmpty()) {
            throw new FilterServiceException("Filter not found.");
        }
        
        Filter filter = filterOptional.get();
        if (!filter.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
            throw new FilterServiceException("Filter not found or not owned by user.");
        }
        return sharedFilterService.createShareableLink(filterId);
    }

    /**
     * Deletes a list of filters.
     *
     * @param filterIds The list of filter IDs to delete.
     * @param user      The user performing the action.
     * @throws FilterServiceException if a filter is not found or not owned by the user.
     */
    @Transactional
    public void deleteFilters(List<Long> filterIds, User user) {
        for (Long filterId : filterIds) {
            Optional<Filter> filterOptional = filterRepository.findById(filterId);
            if (filterOptional.isEmpty()) {
                throw new FilterServiceException("Filter not found: " + filterId);
            }
            
            Filter filter = filterOptional.get();
            if (!filter.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
                throw new FilterServiceException("Filter not found or not owned by user: " + filterId);
            }
            filterRepository.deleteById(filterId);
        }
    }
}
