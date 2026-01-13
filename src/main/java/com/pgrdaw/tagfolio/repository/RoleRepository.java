package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Role;
import com.pgrdaw.tagfolio.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for {@link Role} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
    /**
     * Finds a role by its name.
     *
     * @param name The name of the role.
     * @return An {@link Optional} containing the role if found, or empty otherwise.
     */
    Optional<Role> findByName(RoleType name);
}
