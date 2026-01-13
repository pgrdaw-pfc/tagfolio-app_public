package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.SharedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link SharedFilter} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Repository
public interface SharedFilterRepository extends JpaRepository<SharedFilter, Long> {
    /**
     * Finds a shared filter by its token.
     *
     * @param token The token of the shared filter.
     * @return An {@link Optional} containing the shared filter if found, or empty otherwise.
     */
    Optional<SharedFilter> findByToken(String token);

    /**
     * Finds a shared filter by its content hash.
     *
     * @param contentHash The content hash of the shared filter.
     * @return An {@link Optional} containing the shared filter if found, or empty otherwise.
     */
    Optional<SharedFilter> findByContentHash(String contentHash);

    /**
     * Finds a shared filter by the ID of the original filter it shares.
     *
     * @param filterId The ID of the original filter.
     * @return An {@link Optional} containing the shared filter if found, or empty otherwise.
     */
    Optional<SharedFilter> findByFilterId(Long filterId);
}
