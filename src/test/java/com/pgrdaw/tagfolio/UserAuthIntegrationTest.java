package com.pgrdaw.tagfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession; // Import MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles; // Import ActiveProfiles
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID; // Import UUID

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Ensures that tests are rolled back after execution
@ActiveProfiles("test") // Activate the "test" profile for this test class
public class UserAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String userEmail;
    private String userPassword;

    @BeforeEach
    void setUp() {
        // Generate a unique email for each test run
        userEmail = "testuser_" + UUID.randomUUID().toString() + "@example.com";
        userPassword = "password123";

        User user = new User(userEmail, passwordEncoder.encode(userPassword));
        userRepository.saveAndFlush(user);
    }

    @Test
    void testUserRegistrationSuccess() throws Exception {
        String newEmail = "newuser@example.com";
        String newPassword = "newpassword";

        mockMvc.perform(post("/register")
                        .param("email", newEmail)
                        .param("password", newPassword)
                        .param("password_confirmation", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "User registered successfully!"))
                .andExpect(redirectedUrl("/"));

        assertThat(userRepository.findByEmail(newEmail)).isPresent();
    }

    @Test
    void testUserRegistrationPasswordMismatch() throws Exception {
        String newEmail = "mismatch@example.com";
        String newPassword = "newpassword";
        String confirmPassword = "wrongpassword";

        mockMvc.perform(post("/register")
                        .param("email", newEmail)
                        .param("password", newPassword)
                        .param("password_confirmation", confirmPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Passwords do not match."))
                .andExpect(redirectedUrl("/register"));

        assertThat(userRepository.findByEmail(newEmail)).isNotPresent();
    }

    @Test
    void testUserLoginSuccess() throws Exception {
        mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail));
    }

    @Test
    void testUserLoginFailure() throws Exception {
        mockMvc.perform(formLogin("/login").user(userEmail).password("wrongpassword"))
                .andExpect(unauthenticated());
    }

    @Test
    void testUserLogout() throws Exception {
        // First, log in the user
        mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail));

        // Then, perform logout
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated());
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testUserChangePasswordSuccess() throws Exception {
        // Log in the user first
        MvcResult loginResult = mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail))
                .andReturn();

        String newPassword = "newSecurePassword";

        mockMvc.perform(post("/settings/change-password")
                        .session((MockHttpSession) loginResult.getRequest().getSession()) // Use the session from login
                        .param("current-password", userPassword)
                        .param("new-password", newPassword)
                        .param("new-password-confirmation", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("globalSuccessMessage", "Password changed successfully."))
                .andExpect(redirectedUrl("/"));

        // Verify password change by attempting to log in with the new password
        mockMvc.perform(formLogin("/login").user(userEmail).password(newPassword))
                .andExpect(authenticated().withUsername(userEmail));

        // Ensure old password no longer works
        mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(unauthenticated());
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testUserChangePasswordMismatch() throws Exception {
        // Log in the user first
        MvcResult loginResult = mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail))
                .andReturn();

        String newPassword = "newSecurePassword";
        String confirmNewPassword = "mismatchPassword";

        mockMvc.perform(post("/settings/change-password")
                        .session((MockHttpSession) loginResult.getRequest().getSession())
                        .param("current-password", userPassword)
                        .param("new-password", newPassword)
                        .param("new-password-confirmation", confirmNewPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("globalErrorMessage", "New passwords do not match."))
                .andExpect(redirectedUrl("/settings/change-password"));

        // Ensure password was not changed
        mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail));
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testUserChangePasswordIncorrectCurrentPassword() throws Exception {
        // Log in the user first
        MvcResult loginResult = mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail))
                .andReturn();

        String newPassword = "newSecurePassword";

        mockMvc.perform(post("/settings/change-password")
                        .session((MockHttpSession) loginResult.getRequest().getSession())
                        .param("current-password", "wrongCurrentPassword")
                        .param("new-password", newPassword)
                        .param("new-password-confirmation", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("globalErrorMessage", "Incorrect current password."))
                .andExpect(redirectedUrl("/settings/change-password"));

        // Ensure password was not changed
        mockMvc.perform(formLogin("/login").user(userEmail).password(userPassword))
                .andExpect(authenticated().withUsername(userEmail));
    }
}
