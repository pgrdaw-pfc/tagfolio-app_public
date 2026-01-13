package com.pgrdaw.tagfolio.service;

import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.RoleRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A service for managing users.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new UserService.
     *
     * @param userRepository  The user repository.
     * @param roleRepository  The role repository.
     * @param passwordEncoder The password encoder.
     */
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes the password for a user.
     *
     * @param username        The username of the user.
     * @param currentPassword The user's current password.
     * @param newPassword     The new password.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws IllegalArgumentException  if the current password is incorrect.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the user if found, or empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Gets the currently authenticated user.
     *
     * @param authentication The authentication object.
     * @return The current user, or null if not authenticated.
     * @throws NoSuchElementException if the authenticated user is not found in the repository.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String userIdentifier = authentication.getName();
        return userRepository.findByEmail(userIdentifier)
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found in repository: " + userIdentifier));
    }

    /**
     * Registers a new user.
     *
     * @param email    The user's email address.
     * @param password The user's password.
     * @param roleType The user's role type.
     * @return The newly created user.
     */
    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public User registerUser(String email, String password, RoleType roleType) {
        User newUser = new User(email, passwordEncoder.encode(password));
        roleRepository.findByName(roleType).ifPresent(newUser::addRole);
        return userRepository.save(newUser);
    }

    /**
     * Finds all users, sorted by ID in descending order.
     *
     * @return A list of all users.
     */
    @Transactional(readOnly = true)
    public List<User> findAllUsersSortedByIdDesc() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Creates a new user.
     *
     * @param email    The user's email address.
     * @param password The user's password.
     * @param roleType The user's role type.
     * @return The newly created user.
     * @throws IllegalArgumentException if a user with the same email already exists.
     */
    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public User createUser(String email, String password, RoleType roleType) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists.");
        }
        String encodedPassword = (password != null && !password.isEmpty()) ? passwordEncoder.encode(password) : passwordEncoder.encode(email);
        User newUser = new User(email, encodedPassword);
        roleRepository.findByName(roleType).ifPresent(newUser::addRole);
        return userRepository.save(newUser);
    }

    /**
     * Updates an existing user.
     *
     * @param id       The ID of the user to update.
     * @param email    The new email address.
     * @param roleType The new role type.
     * @return The updated user.
     * @throws UsernameNotFoundException if the user is not found.
     * @throws IllegalArgumentException  if a user with the new email already exists.
     */
    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public User updateUser(Long id, String email, RoleType roleType) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists.");
        }

        user.setEmail(email);
        user.getRoles().clear();
        roleRepository.findByName(roleType).ifPresent(user::addRole);
        return userRepository.save(user);
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id The ID of the user to delete.
     * @throws UsernameNotFoundException if the user is not found.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
