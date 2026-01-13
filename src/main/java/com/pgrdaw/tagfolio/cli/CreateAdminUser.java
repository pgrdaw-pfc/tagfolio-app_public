package com.pgrdaw.tagfolio.cli;

import com.pgrdaw.tagfolio.model.Role;
import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.RoleRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Scanner;

/**
 * A command-line tool to create an administrator user.
 * This tool is only active when the "create-admin" profile is enabled.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Component
@Profile("create-admin")
public class CreateAdminUser implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CreateAdminUser.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new CreateAdminUser command-line runner.
     *
     * @param userRepository  The repository for user data.
     * @param roleRepository  The repository for role data.
     * @param passwordEncoder The password encoder.
     */
    public CreateAdminUser(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Executes the command-line runner to create an admin user.
     *
     * @param args Command-line arguments.
     */
    @Override
    @Transactional
    public void run(String... args) {
        logger.info("--- Admin User Creation Tool ---");

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter admin email: ");
            String email = scanner.nextLine();

            if (userRepository.findByEmail(email).isPresent()) {
                logger.error("Error: A user with the email '{}' already exists.", email);
                return;
            }

            System.out.print("Enter admin password: ");
            String password = scanner.nextLine();
            
            Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseGet(() -> {
                    logger.info("ADMIN role not found, creating it...");
                    return roleRepository.save(new Role(RoleType.ADMIN));
                });

            User adminUser = new User(email, passwordEncoder.encode(password));
            adminUser.addRole(adminRole);

            userRepository.save(adminUser);

            logger.info("✅ Successfully created admin user: {}", email);
        }
    }
}
