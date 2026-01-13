package com.pgrdaw.tagfolio.repository;

import com.pgrdaw.tagfolio.model.Permission;
import com.pgrdaw.tagfolio.model.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for {@link Permission} entities.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    /**
     * Finds a permission by its name.
     *
     * @param name The name of the permission.
     * @return An {@link Optional} containing the permission if found, or empty otherwise.
     */
    Optional<Permission> findByName(PermissionType name);
}
