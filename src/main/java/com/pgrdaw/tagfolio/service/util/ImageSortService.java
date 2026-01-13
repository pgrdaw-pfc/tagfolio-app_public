package com.pgrdaw.tagfolio.service.util;

import com.pgrdaw.tagfolio.model.Image;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * A service for sorting images based on various fields.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class ImageSortService {

    private final Map<String, String> sortableFields;
    private Map<String, Function<Image, ? extends Comparable>> fieldExtractors;

    /**
     * Constructs a new ImageSortService.
     *
     * @param sortableFields A map of sortable field names to their corresponding ExifTool keys.
     */
    public ImageSortService(@Value("#{${app.sortable-fields}}") Map<String, String> sortableFields) {
        this.sortableFields = sortableFields;
    }

    /**
     * Initializes the field extractors map.
     */
    @PostConstruct
    public void init() {
        fieldExtractors = new HashMap<>();
        fieldExtractors.put("Filename", Image::getOriginalFileName);
        fieldExtractors.put("Created", Image::getCreatedAt);
        fieldExtractors.put("Modified", Image::getModifiedAt);
        fieldExtractors.put("Rating", Image::getRating);
        fieldExtractors.put("default", Image::getImportedAt);
    }

    /**
     * Gets a list of sortable field names.
     *
     * @return A list of sortable field names.
     */
    public List<String> getSortableFields() {
        return new ArrayList<>(sortableFields.keySet());
    }

    /**
     * Gets the map of sortable fields.
     *
     * @return An unmodifiable map of sortable fields.
     */
    public Map<String, String> getSortableFieldsMap() {
        return Collections.unmodifiableMap(sortableFields);
    }

    /**
     * Gets a comparator for sorting images.
     *
     * @param sort      The field to sort by.
     * @param direction The sort direction ("asc" or "desc").
     * @return A comparator for sorting images.
     */
    @SuppressWarnings("unchecked")
    public Comparator<Image> getImageComparator(String sort, String direction) {
        Function<Image, ? extends Comparable> extractor = fieldExtractors.getOrDefault(sort, fieldExtractors.get("default"));

        return (img1, img2) -> {
            Comparable val1 = extractor.apply(img1);
            Comparable val2 = extractor.apply(img2);

            Comparator<Comparable> nonNullValueComparator = (v1, v2) -> {
                if (v1 instanceof String) {
                    return ((String) v1).compareToIgnoreCase((String) v2);
                } else if (v1 instanceof LocalDateTime) {
                    return v1.compareTo((LocalDateTime) v2);
                } else if (v1 instanceof Integer) {
                    return v1.compareTo((Integer) v2);
                } else {
                    return v1.compareTo(v2);
                }
            };

            Comparator<Comparable> effectiveNonNullComparator = "desc".equalsIgnoreCase(direction) ?
                    nonNullValueComparator.reversed() :
                    nonNullValueComparator;

            return Comparator.nullsLast(effectiveNonNullComparator).compare(val1, val2);
        };
    }
}
