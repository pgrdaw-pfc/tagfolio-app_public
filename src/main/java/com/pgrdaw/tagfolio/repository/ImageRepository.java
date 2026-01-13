package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Image} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * Finds a list of images by user ID.
     *
     * @param userId The ID of the user.
     * @return A list of images.
     */
    List<Image> findByUserId(Long userId);

    /**
     * Finds an image by its original file name and user.
     *
     * @param originalFileName The original file name of the image.
     * @param user             The user who owns the image.
     * @return An {@link Optional} containing the image if found, or empty otherwise.
     */
    Optional<Image> findByOriginalFileNameAndUser(String originalFileName, User user);

    /**
     * Finds all images by their original file name and user.
     *
     * @param originalFileName The original file name of the image.
     * @param user             The user who owns the image.
     * @return A list of images found.
     */
    List<Image> findAllByOriginalFileNameAndUser(String originalFileName, User user);

    /**
     * Finds an image by its ID, fetching the user and tags eagerly.
     *
     * @param id The ID of the image.
     * @return An {@link Optional} containing the image if found, or empty otherwise.
     */
    @Query("SELECT i FROM Image i JOIN FETCH i.user LEFT JOIN FETCH i.tags WHERE i.id = :id")
    Optional<Image> findByIdWithUser(@Param("id") Long id);

    /**
     * Counts the number of images associated with a tag.
     *
     * @param tag The tag.
     * @return The number of images associated with the tag.
     */
    long countByTags(Tag tag);

    /**
     * Checks if any image contains the given tag.
     *
     * @param tag The tag.
     * @return True if any image contains the tag, false otherwise.
     */
    boolean existsByTagsContaining(Tag tag);

    /**
     * Finds a list of images by tag.
     *
     * @param tag The tag.
     * @return A list of images.
     */
    List<Image> findByTags(Tag tag);
}
