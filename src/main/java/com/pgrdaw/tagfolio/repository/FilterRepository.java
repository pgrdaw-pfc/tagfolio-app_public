package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Filter;
import com.pgrdaw.tagfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Filter} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface FilterRepository extends JpaRepository<Filter, Long> {

    /**
     * Finds a list of filters by user ID.
     *
     * @param userId The ID of the user.
     * @return A list of filters.
     */
    List<Filter> findByUserId(Long userId);

    /**
     * Finds a list of filters by user.
     *
     * @param user The user.
     * @return A list of filters.
     */
    List<Filter> findByUser(User user);

    /**
     * Finds a list of filters by user, ordered by ID in descending order.
     *
     * @param user The user.
     * @return A list of filters.
     */
    List<Filter> findByUserOrderByIdDesc(User user);

    /**
     * Finds a filter by user and expression.
     *
     * @param user       The user.
     * @param expression The filter expression.
     * @return An {@link Optional} containing the filter if found, or empty otherwise.
     */
    Optional<Filter> findByUserAndExpression(User user, String expression);
}
