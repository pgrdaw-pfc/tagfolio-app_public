/**
 * @file This file handles image-related actions such as deletion and tag removal.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * Deletes the selected images.
 */
window.handleDeleteImages = async function() {
    const imageIdsToDelete = Array.from(window.selectedImageElements).map(el => el.dataset.id);
    if (imageIdsToDelete.length === 0) {
        window.displayGlobalAlert('info', 'No images selected for deletion.');
        return;
    }

    window.displayConfirmationAlert({
        message: `Are you sure you want to delete ${imageIdsToDelete.length} selected image(s)?`,
        onConfirm: async () => {
            try {
                const response = await fetch('/images', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify(imageIdsToDelete)
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to delete images.');
                }
                window.displayGlobalAlert('success', `${imageIdsToDelete.length} image(s) deleted successfully.`);
                location.reload();
            } catch (error) {
                console.error('Error deleting images:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while deleting images.');
            }
        }
    });
};

/**
 * Removes selected tags from the selected images.
 */
window.handleRemoveTagsFromImages = async function() {
    const imageIds = Array.from(window.selectedImageElements).map(el => el.dataset.id);
    const tagNames = Array.from(window.selectedTagElements).map(el => el.dataset.tagName);

    if (imageIds.length === 0 || tagNames.length === 0) {
        window.displayGlobalAlert('info', 'Select both images and tags to remove.');
        return;
    }

    window.displayConfirmationAlert({
        message: `Are you sure you want to remove ${tagNames.length} tag(s) from ${imageIds.length} image(s)?`,
        onConfirm: async () => {
            try {
                const response = await fetch('/images/tags/removeSelected', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify({ imageIds: imageIds, tagNames: tagNames })
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to remove tags from images.');
                }
                window.displayGlobalAlert('success', `Tags removed from ${imageIds.length} image(s) successfully.`);
                location.reload();
            } catch (error) {
                console.error('Error removing tags from images:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while removing tags from images.');
            }
        }
    });
};

/**
 * Updates the global `allImageItems` array based on the current view.
 */
window.updateAllImageItems = function() {
    const imageGrid = document.getElementById('image-grid-main');
    if (imageGrid) {
        window.allImageItems = Array.from(imageGrid.querySelectorAll('.image-card'));
    } else {
        window.allImageItems = [];
    }
};
