package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.RoleRepository;
import com.pgrdaw.tagfolio.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for handling user management requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    /**
     * Constructs a new UserController.
     *
     * @param userService    The user service.
     * @param roleRepository The role repository.
     */
    @Autowired
    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    /**
     * Displays the list of users.
     *
     * @param model The model to add attributes to.
     * @return The name of the user list view.
     */
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAllUsersSortedByIdDesc();
        model.addAttribute("users", users);
        model.addAttribute("roleTypes", Arrays.asList(RoleType.ADMIN, RoleType.USER));
        model.addAttribute("isUserManagementPage", true);
        return "users/list";
    }

    /**
     * Creates a new user inline.
     *
     * @param email              The user's email address.
     * @param roleType           The user's role type.
     * @param redirectAttributes The redirect attributes.
     * @return A redirect to the user list page.
     */
    @PostMapping("/inline-create")
    public String inlineCreateUser(@RequestParam String email,
                                   @RequestParam RoleType roleType,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(email, email, roleType);
            redirectAttributes.addFlashAttribute("globalSuccessMessage", "User created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    /**
     * Updates a user inline.
     *
     * @param id                 The ID of the user to update.
     * @param email              The new email address.
     * @param roleType           The new role type.
     * @param redirectAttributes The redirect attributes.
     * @return A redirect to the user list page.
     */
    @PostMapping("/inline-update/{id}")
    public String inlineUpdateUser(@PathVariable Long id,
                                   @RequestParam String email,
                                   @RequestParam RoleType roleType,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, email, roleType);
            redirectAttributes.addFlashAttribute("globalSuccessMessage", "User updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    /**
     * Deletes a user.
     *
     * @param id                 The ID of the user to delete.
     * @param redirectAttributes The redirect attributes.
     * @return A redirect to the user list page.
     */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("globalSuccessMessage", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/users";
    }
}
