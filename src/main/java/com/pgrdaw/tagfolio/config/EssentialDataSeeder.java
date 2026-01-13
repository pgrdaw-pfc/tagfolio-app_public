package com.pgrdaw.tagfolio.config;

import com.pgrdaw.tagfolio.model.Permission;
import com.pgrdaw.tagfolio.model.PermissionType;
import com.pgrdaw.tagfolio.model.ReportType;
import com.pgrdaw.tagfolio.model.Role;
import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.repository.PermissionRepository;
import com.pgrdaw.tagfolio.repository.ReportTypeRepository;
import com.pgrdaw.tagfolio.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * A command-line runner that seeds the database with essential data.
 * This seeder runs on every application startup.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Component
@Order(1)
public class EssentialDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EssentialDataSeeder.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ReportTypeRepository reportTypeRepository;

    /**
     * Constructs a new EssentialDataSeeder.
     *
     * @param roleRepository       The role repository.
     * @param permissionRepository The permission repository.
     * @param reportTypeRepository The report type repository.
     */
    public EssentialDataSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository, ReportTypeRepository reportTypeRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.reportTypeRepository = reportTypeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Running essential data seeder...");
        seedPermissions();
        seedRoles();
        seedReportTypes();
        logger.info("Essential data seeding complete.");
    }

    private void seedPermissions() {
        for (PermissionType type : PermissionType.values()) {
            permissionRepository.findByName(type)
                    .orElseGet(() -> permissionRepository.save(new Permission(type)));
        }
    }

    private void createOrUpdateRoleWithPermissions(RoleType roleType, PermissionType... permissionTypes) {
        Role role = roleRepository.findByName(roleType)
                .orElseGet(() -> roleRepository.save(new Role(roleType)));
        Set<Permission> permissions = new HashSet<>();
        for (PermissionType permissionType : permissionTypes) {
            permissionRepository.findByName(permissionType).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

    private void seedRoles() {
        createOrUpdateRoleWithPermissions(RoleType.ADMIN,
                PermissionType.READ_OWN_IMAGES,
                PermissionType.UPLOAD_IMAGES,
                PermissionType.DELETE_IMAGES,
                PermissionType.MANAGE_USERS,
                PermissionType.VIEW_ADMIN_DASHBOARD,
                PermissionType.READ_SHARED_IMAGES);

        createOrUpdateRoleWithPermissions(RoleType.USER,
                PermissionType.READ_OWN_IMAGES,
                PermissionType.UPLOAD_IMAGES,
                PermissionType.DELETE_IMAGES);

        createOrUpdateRoleWithPermissions(RoleType.ANONYMOUS,
                PermissionType.READ_SHARED_IMAGES);
    }

    private void seedReportTypes() {
        reportTypeRepository.findByName("compact")
                .orElseGet(() -> reportTypeRepository.save(new ReportType("compact")));
        reportTypeRepository.findByName("extended")
                .orElseGet(() -> reportTypeRepository.save(new ReportType("extended")));
    }
}
