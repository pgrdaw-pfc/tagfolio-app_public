package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling user settings-related requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;

    /**
     * Constructs a new SettingsController.
     *
     * @param userService The user service.
     */
    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the change password form.
     *
     * @param model The model to add attributes to.
     * @return The name of the change password view.
     */
    @GetMapping("/change-password")
    @SuppressWarnings("SameReturnValue")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("isSettingsPage", true);
        return "settings/change-password";
    }

    /**
     * Changes the user's password.
     *
     * @param authentication          The current authentication object.
     * @param currentPassword         The user's current password.
     * @param newPassword             The new password.
     * @param newPasswordConfirmation The new password confirmation.
     * @param redirectAttributes      The redirect attributes.
     * @return A redirect to the appropriate page.
     */
    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestParam("current-password") String currentPassword,
                                 @RequestParam("new-password") String newPassword,
                                 @RequestParam("new-password-confirmation") String newPasswordConfirmation,
                                 RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(newPasswordConfirmation)) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", "New passwords do not match.");
            return "redirect:/settings/change-password";
        }

        String username = authentication.getName();
        try {
            userService.changePassword(username, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("globalSuccessMessage", "Password changed successfully.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", e.getMessage());
            return "redirect:/settings/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("globalErrorMessage", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/settings/change-password";
        }
    }
}
