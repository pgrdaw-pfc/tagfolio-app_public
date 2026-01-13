/**
 * @file This file contains utility functions for making API requests, including handling CSRF tokens.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * Gets the CSRF headers for fetch requests.
 *
 * @returns {Object} An object containing the CSRF header and token, or an empty object if not available.
 */
function getCsrfHeaders() {
    if (window.csrfToken && window.csrfHeader) {
        return {[window.csrfHeader]: window.csrfToken};
    }
    return {};
}
