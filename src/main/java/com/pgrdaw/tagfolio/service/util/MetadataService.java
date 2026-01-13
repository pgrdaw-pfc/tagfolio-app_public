package com.pgrdaw.tagfolio.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A service for handling image metadata.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private final Map<String, String> displayMetadataMap;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new MetadataService.
     *
     * @param displayMetadataMap A map of display names to ExifTool keys.
     * @param objectMapper       The object mapper for JSON processing.
     */
    public MetadataService(@Value("#{${image.exiftool.display-metadata-keys}}") Map<String, String> displayMetadataMap,
                           ObjectMapper objectMapper) {
        this.displayMetadataMap = displayMetadataMap;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets the map of display metadata keys.
     *
     * @return An unmodifiable map of display metadata keys.
     */
    public Map<String, String> getDisplayMetadataMap() {
        return Collections.unmodifiableMap(displayMetadataMap);
    }

    /**
     * Gets a list of all display metadata keys.
     *
     * @return A list of display metadata keys.
     */
    public List<String> getDisplayMetadataKeys() {
        return displayMetadataMap.values().stream()
                .flatMap(keys -> Arrays.stream(keys.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Gets the value of a metadata field by its display name.
     *
     * @param displayName The display name of the metadata field.
     * @param rawMetadata The raw metadata map.
     * @return The value of the metadata field, or null if not found.
     */
    public String getMetadataValue(String displayName, Map<String, Object> rawMetadata) {
        String exiftoolKeysString = displayMetadataMap.get(displayName);
        if (exiftoolKeysString == null || rawMetadata == null) {
            return null;
        }

        List<String> exiftoolKeys = Arrays.stream(exiftoolKeysString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String key : exiftoolKeys) {
            Object value = rawMetadata.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * Gets the raw metadata map from an image.
     *
     * @param image The image.
     * @return The raw metadata map.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    public Map<String, Object> getRawMetadata(Image image) throws JsonProcessingException {
        if (image.getExiftool() != null && !image.getExiftool().isEmpty()) {
            return objectMapper.readValue(image.getExiftool(), new TypeReference<>() {
            });
        }
        return Collections.emptyMap();
    }
}
