package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.TagWithCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for {@link Tag} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    /**
     * Finds a list of tags by their names.
     *
     * @param names The set of tag names.
     * @return A list of tags.
     */
    List<Tag> findByNameIn(Set<String> names);

    /**
     * Finds a tag by its name.
     *
     * @param name The name of the tag.
     * @return An {@link Optional} containing the tag if found, or empty otherwise.
     */
    Optional<Tag> findByName(String name);

    /**
     * Finds all tags with their usage counters for a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of {@link TagWithCounter} objects.
     */
    @Query("SELECT new com.pgrdaw.tagfolio.model.TagWithCounter(t, COUNT(i)) FROM Image i JOIN i.tags t WHERE i.user.id = :userId GROUP BY t.id, t.name, t.createdAt, t.updatedAt ORDER BY COUNT(i) DESC")
    List<TagWithCounter> findTagsWithCountersByUserId(@Param("userId") Long userId);

    /**
     * Finds all tags with their usage counters.
     *
     * @return A list of {@link TagWithCounter} objects.
     */
    @Query("SELECT new com.pgrdaw.tagfolio.model.TagWithCounter(t, COUNT(i)) FROM Image i JOIN i.tags t GROUP BY t.id, t.name, t.createdAt, t.updatedAt ORDER BY COUNT(i) DESC")
    List<TagWithCounter> findAllTagsWithCounters();
}
