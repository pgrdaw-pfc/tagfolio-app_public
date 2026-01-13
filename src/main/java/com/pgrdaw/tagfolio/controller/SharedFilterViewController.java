package com.pgrdaw.tagfolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.SharedFilter;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.service.FilterExpressionEvaluator;
import com.pgrdaw.tagfolio.service.ImageService;
import com.pgrdaw.tagfolio.service.SharedFilterService;
import com.pgrdaw.tagfolio.service.util.ImageSortService;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for handling shared filter view requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
public class SharedFilterViewController {

    private final SharedFilterService sharedFilterService;
    private final ImageService imageService;
    private final FilterExpressionEvaluator filterExpressionEvaluator;
    private final ImageSortService imageSortService;
    private final MetadataService metadataService;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    /**
     * Constructs a new SharedFilterViewController.
     *
     * @param sharedFilterService     The shared filter service.
     * @param imageService            The image service.
     * @param filterExpressionEvaluator The filter expression evaluator.
     * @param imageSortService        The image sort service.
     * @param metadataService         The metadata service.
     * @param objectMapper            The object mapper for JSON processing.
     * @param batchSize               The batch size for image loading.
     */
    public SharedFilterViewController(SharedFilterService sharedFilterService,
                                      ImageService imageService,
                                      FilterExpressionEvaluator filterExpressionEvaluator,
                                      ImageSortService imageSortService,
                                      MetadataService metadataService,
                                      ObjectMapper objectMapper,
                                      @Value("${image.batch-size:50}") int batchSize) {
        this.sharedFilterService = sharedFilterService;
        this.imageService = imageService;
        this.filterExpressionEvaluator = filterExpressionEvaluator;
        this.imageSortService = imageSortService;
        this.metadataService = metadataService;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    /**
     * Displays the shared filter view.
     *
     * @param token     The shared filter token.
     * @param view      The view type (e.g., "grid").
     * @param sort      The sort field.
     * @param direction The sort direction.
     * @param model     The model to add attributes to.
     * @param request   The HTTP request.
     * @return The name of the view to render.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    @GetMapping("/shared/filter/{token}")
    @Transactional(readOnly = true)
    public String viewSharedFilter(@PathVariable String token,
                                   @RequestParam(value = "view", defaultValue = "grid") String view,
                                   @RequestParam(value = "sort", defaultValue = "Imported") String sort,
                                   @RequestParam(value = "direction", defaultValue = "desc") String direction,
                                   Model model,
                                   HttpServletRequest request) throws JsonProcessingException {
        Optional<SharedFilter> sharedFilterOptional = sharedFilterService.getSharedFilterByToken(token);

        if (sharedFilterOptional.isEmpty()) {
            return "error/404";
        }

        SharedFilter sharedFilter = sharedFilterOptional.get();
        Filter originalFilter = sharedFilter.getFilter();
        User filterCreator = originalFilter.getUser();
        List<Image> allCreatorImages = imageService.getImagesForUser(filterCreator);

        List<Map<String, String>> filterExpression = filterExpressionEvaluator.parseExpressionJson(originalFilter.getExpression());
        List<Long> matchingImageIds = filterExpressionEvaluator.evaluate(allCreatorImages, filterExpression);

        List<Image> images = imageService.findImagesByIds(matchingImageIds);

        images.sort(imageSortService.getImageComparator(sort, direction));
        
        List<Long> sortedImageIds = images.stream().map(Image::getId).collect(Collectors.toList());

        HttpSession session = request.getSession();
        session.setAttribute("sharedImageIds", matchingImageIds);
        session.setAttribute("sortedImageIds", sortedImageIds);
        session.setAttribute("sharedFilterToken", token);

        model.addAttribute("images", images);
        model.addAttribute("isSharedView", true);
        model.addAttribute("sharedFilterName", originalFilter.getName());
        model.addAttribute("sharedFilterToken", sharedFilter.getToken());
        model.addAttribute("currentView", view);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("sharedImageIds", matchingImageIds);
        model.addAttribute("sortableFields", imageSortService.getSortableFieldsMap());
        model.addAttribute("sortableFieldsJson", objectMapper.writeValueAsString(imageSortService.getSortableFieldsMap()));
        model.addAttribute("displayMetadataMap", metadataService.getDisplayMetadataMap());
        model.addAttribute("displayMetadataMapJson", objectMapper.writeValueAsString(metadataService.getDisplayMetadataMap()));
        model.addAttribute("batchSize", batchSize);
        model.addAttribute("displayMetadataKeys", metadataService.getDisplayMetadataKeys());

        return "filters/index";
    }
}
