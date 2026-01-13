package com.pgrdaw.tagfolio;

import com.pgrdaw.tagfolio.model.Role;
import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.RoleRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class UserCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SuppressWarnings("FieldCanBeLocal")
    private User adminUser;
    @SuppressWarnings("FieldCanBeLocal")
    private User regularUser;

    @BeforeEach
    void setUp() {
        // Clear only user data before each test. Roles are seeded once and should persist.
        userRepository.deleteAll();

        // Fetch roles created by EssentialDataSeeder
        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseThrow(() -> new NoSuchElementException("ADMIN role not found. Check EssentialDataSeeder."));
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new NoSuchElementException("USER role not found. Check EssentialDataSeeder."));

        String adminEmail = "admin_" + UUID.randomUUID() + "@example.com";
        adminUser = new User(adminEmail, passwordEncoder.encode("password"));
        adminUser.addRole(adminRole);
        userRepository.save(adminUser);

        String userEmail = "user_" + UUID.randomUUID() + "@example.com";
        regularUser = new User(userEmail, passwordEncoder.encode("password"));
        regularUser.addRole(userRole);
        userRepository.save(regularUser);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("roleTypes"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminCanCreateUser() throws Exception {
        String newUserEmail = "newuser@example.com";
        mockMvc.perform(post("/users/inline-create")
                        .param("email", newUserEmail)
                        .param("roleType", "USER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertThat(userRepository.findByEmail(newUserEmail)).isPresent();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminCanUpdateUser() throws Exception {
        String updatedEmail = "updated@example.com";
        mockMvc.perform(post("/users/inline-update/" + regularUser.getId())
                        .param("email", updatedEmail)
                        .param("roleType", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(updatedEmail);
        assertThat(updatedUser.hasRole(RoleType.ADMIN)).isTrue();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminCanDeleteUser() throws Exception {
        mockMvc.perform(post("/users/delete/" + regularUser.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertThat(userRepository.findById(regularUser.getId())).isNotPresent();
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testUserCannotAccessUserListPage() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }
}
