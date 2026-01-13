package com.pgrdaw.tagfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tag that can be associated with an image.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    private Set<Image> images = new HashSet<>();

    /**
     * Constructs a new Tag with the specified name.
     * The name is converted to lowercase.
     *
     * @param name The name of the tag.
     */
    public Tag(String name) {
        this.name = name.toLowerCase();
    }
}
