/**
 * @file This file handles the behavior of dropdown menus, such as the user profile dropdown.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

document.addEventListener('DOMContentLoaded', function() {
    const userProfileDropdown = document.querySelector('.user-profile-dropdown');
    const userProfileToggle = document.querySelector('.user-profile-toggle');

    if (userProfileDropdown && userProfileToggle) {
        userProfileToggle.addEventListener('click', function(event) {
            event.preventDefault();
            event.stopPropagation(); // Prevent document click from closing immediately
            userProfileDropdown.classList.toggle('active');
        });
    }

    // --- Click Outside to Close Menus ---
    document.addEventListener('click', function(event) {
        // Close user profile dropdown
        if (userProfileDropdown && !userProfileDropdown.contains(event.target)) {
            userProfileDropdown.classList.remove('active');
        }
    });
});
