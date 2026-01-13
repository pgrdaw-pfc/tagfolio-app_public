package com.pgrdaw.tagfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents an image in the application.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "tags", "reportImages"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String thumbnailFileName;

    @Lob
    @Column(name = "exiftool", columnDefinition = "CLOB")
    private String exiftool;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = false)
    private LocalDateTime importedAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "rating")
    private Integer rating;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "image_tag",
            joinColumns = @JoinColumn(name = "image_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReportImage> reportImages = new HashSet<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Constructs a new Image associated with the specified user.
     *
     * @param user The user who owns the image.
     */
    public Image(User user) {
        this.user = user;
    }

    /**
     * Retrieves the value of a specific field from the ExifTool JSON data.
     *
     * @param fieldName The name of the field to retrieve.
     * @return The value of the field as a String, or null if not found.
     */
    public String getExiftoolField(String fieldName) {
        if (exiftool == null || exiftool.isEmpty()) {
            return null;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(exiftool);
            JsonNode fieldNode = rootNode.get(fieldName);
            if (fieldNode != null && fieldNode.isTextual()) {
                return fieldNode.asText();
            } else if (fieldNode != null && fieldNode.isArray()) {
                List<String> values = new ArrayList<>();
                for (JsonNode node : fieldNode) {
                    if (node.isTextual()) {
                        values.add(node.asText());
                    }
                }
                return String.join(", ", values);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing exiftool JSON: " + e.getMessage());
        }
        return null;
    }
}
