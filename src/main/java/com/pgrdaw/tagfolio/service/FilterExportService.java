package com.pgrdaw.tagfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.FilterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * A service for exporting images based on a set of filters.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class FilterExportService {

    private final FilterRepository filterRepository;
    private final ImageService imageService;
    private final FilterExpressionEvaluator filterExpressionEvaluator;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new FilterExportService.
     *
     * @param filterRepository        The filter repository.
     * @param imageService            The image service.
     * @param filterExpressionEvaluator The filter expression evaluator.
     * @param objectMapper            The object mapper for JSON processing.
     */
    public FilterExportService(FilterRepository filterRepository,
                               ImageService imageService,
                               FilterExpressionEvaluator filterExpressionEvaluator,
                               ObjectMapper objectMapper) {
        this.filterRepository = filterRepository;
        this.imageService = imageService;
        this.filterExpressionEvaluator = filterExpressionEvaluator;
        this.objectMapper = objectMapper;
    }

    /**
     * Exports images matching a list of filters to a zip file.
     *
     * @param filterIds       The IDs of the filters to export.
     * @param user            The user performing the export.
     * @param zipOutputStream The zip output stream to write to.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional(readOnly = true)
    public void exportFilters(List<Long> filterIds, User user, ZipOutputStream zipOutputStream) throws IOException {
        List<Image> allUserImages = imageService.getImagesForUser(user);
        Set<Long> allMatchingImageIds = new HashSet<>();

        for (Long filterId : filterIds) {
            Filter filter = filterRepository.findById(filterId).orElse(null);
            if (filter != null && (filter.getUser().equals(user) || user.isAdmin())) {
                try {
                    List<Map<String, String>> expression = objectMapper.readValue(filter.getExpression(), new TypeReference<>() {});
                    allMatchingImageIds.addAll(filterExpressionEvaluator.evaluate(allUserImages, expression));
                } catch (IOException e) {
                    throw new IOException("Error evaluating filter expression for filter ID: " + filterId, e);
                }
            }
        }

        imageService.zipImages(new ArrayList<>(allMatchingImageIds), zipOutputStream);
    }
}
