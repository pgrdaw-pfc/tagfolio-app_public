package com.pgrdaw.tagfolio.controller;

import com.pgrdaw.tagfolio.model.RoleType;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling authentication-related requests.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Constructs a new AuthController.
     *
     * @param userRepository The user repository.
     * @param userService    The user service.
     */
    @Autowired
    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Displays the login page.
     *
     * @param model          The model to add attributes to.
     * @param authentication The current authentication object.
     * @param request        The HTTP request.
     * @return The name of the login view, or a redirect to the home page if already authenticated.
     */
    @GetMapping("/login")
    public String login(Model model, Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        model.addAttribute("pageTitle", "Login - Tagfolio");
        model.addAttribute("currentUri", request.getRequestURI());
        return "auth/login";
    }

    /**
     * Displays the registration form.
     *
     * @param model The model to add attributes to.
     * @return The name of the registration view.
     */
    @GetMapping("/register")
    @SuppressWarnings("SameReturnValue")
    public String showRegistrationForm(Model model) {
        model.addAttribute("pageTitle", "Register - Tagfolio");
        return "auth/register";
    }

    /**
     * Registers a new user.
     *
     * @param email                The user's email address.
     * @param password             The user's password.
     * @param passwordConfirmation The password confirmation.
     * @param redirectAttributes   The redirect attributes.
     * @param request              The HTTP request.
     * @return A redirect to the appropriate page.
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam("password_confirmation") String passwordConfirmation,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request) {

        if (!password.equals(passwordConfirmation)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "A user with that email already exists.");
            return "redirect:/register";
        }

        userService.registerUser(email, password, RoleType.USER);

        try {
            request.login(email, password);
        } catch (ServletException e) {
            redirectAttributes.addFlashAttribute("error", "Registration successful, but automatic login failed. Please log in manually.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("success", "User registered successfully!");
        return "redirect:/";
    }
}
