/**
 * @file This file defines global variables used throughout the application.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * The CSRF token for security purposes.
 * @type {string|null}
 */
window.csrfToken = null;

/**
 * The CSRF header name.
 * @type {string|null}
 */
window.csrfHeader = null;

/**
 * The current URI of the page.
 * @type {string}
 */
window.currentUri = '';

/**
 * A success message from the server.
 * @type {string|null}
 */
window.serverSuccess = null;

/**
 * An informational message from the server.
 * @type {string|null}
 */
window.serverInfo = null;

/**
 * A warning message from the server.
 * @type {string|null}
 */
window.serverWarning = null;

/**
 * An error message from the server.
 * @type {string|null}
 */
window.serverError = null;

/**
 * An array of all image items in the gallery.
 * @type {Array<HTMLElement>}
 */
window.allImageItems = [];

/**
 * A flag indicating if a selection is in progress.
 * @type {boolean}
 */
window.selectionInProgress = false;

/**
 * The current page number for pagination.
 * @type {number}
 */
window.currentPage = 0;

/**
 * A flag indicating if images are currently being loaded.
 * @type {boolean}
 */
window.isLoading = false;

/**
 * A flag indicating if all images have been loaded.
 * @type {boolean}
 */
window.allImagesLoaded = false;

/**
 * A flag indicating if the application has been initialized.
 * @type {boolean}
 */
window.appHasInitialized = false;

/**
 * A flag indicating if a filter is currently active.
 * @type {boolean}
 */
window.isFilterActive = false;

/**
 * An array of image IDs that match the current filter.
 * @type {Array<number>}
 */
window.filteredMatchingIds = [];

/**
 * An array of image IDs that do not match the current filter.
 * @type {Array<number>}
 */
window.filteredNonMatchingIds = [];

/**
 * The current sort field for the gallery.
 * @type {string}
 */
window.sortField = window.currentSort || 'XMP-xmp:CreateDate';

/**
 * The current sort direction for the gallery.
 * @type {string}
 */
window.sortDirection = window.currentDirection || 'desc';

/**
 * The number of images to load per batch.
 * @type {number}
 */
window.batchSize = window.batchSize || 50;
