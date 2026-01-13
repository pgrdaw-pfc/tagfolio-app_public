/**
 * @file This file handles the selection logic for various items in the UI, including images, tags, filters, and reports.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

let selectedImageElements = new Set();
let lastSelectedImageId = null;

let selectedTagElements = new Set();
let lastSelectedTagId = null;

let selectedSavedFilterElements = new Set();
let lastSelectedSavedFilterId = null;

let selectedReportElements = new Set();
let lastSelectedReportId = null;

/**
 * Clears all image selections.
 */
function clearImageSelections() {
    const mainContentArea = document.getElementById('main-content-area');
    if (mainContentArea) {
        const allVisibleItems = Array.from(mainContentArea.querySelectorAll('.image-card, .list-item'));
        allVisibleItems.forEach(item => item.classList.remove('selected'));
    }
    selectedImageElements.clear();
    lastSelectedImageId = null;
    document.dispatchEvent(new CustomEvent('imageSelectionChanged', { detail: { selectedCount: 0 } }));
}

/**
 * Clears all tag selections.
 */
function clearTagSelections() {
    selectedTagElements.forEach(el => el.classList.remove('selected'));
    selectedTagElements.clear();
    lastSelectedTagId = null;
    document.dispatchEvent(new CustomEvent('tagSelectionChanged', { detail: { selectedCount: 0 } }));
}

/**
 * Clears all filter selections.
 */
function clearFilterSelections() {
    selectedSavedFilterElements.forEach(el => el.classList.remove('selected'));
    selectedSavedFilterElements.clear();
    lastSelectedSavedFilterId = null;
    document.dispatchEvent(new CustomEvent('filterSelectionChanged', { detail: { selectedCount: 0 } }));
}

/**
 * Clears all report selections.
 */
function clearReportSelections() {
    selectedReportElements.forEach(el => el.classList.remove('selected'));
    selectedReportElements.clear();
    lastSelectedReportId = null;
    document.dispatchEvent(new CustomEvent('reportSelectionChanged', { detail: { selectedCount: 0 } }));
}

/**
 * Clears all selections of any type.
 */
function clearAllSelections() {
    clearImageSelections();
    clearTagSelections();
    clearFilterSelections();
    clearReportSelections();
}

/**
 * Handles the selection logic for an item.
 * @param {Event} event - The click event.
 * @param {HTMLElement} item - The item being selected.
 * @param {Set<HTMLElement>} itemSet - The set of selected items.
 * @param {string|null} currentLastSelectedId - The ID of the last selected item.
 * @param {string} itemSelector - The CSS selector for the items.
 * @param {string} idAttribute - The data attribute containing the item's ID.
 * @param {boolean} [forceToggle=false] - Whether to force a toggle selection.
 * @returns {string|null} The ID of the updated last selected item.
 */
function handleItemSelection(event, item, itemSet, currentLastSelectedId, itemSelector, idAttribute, forceToggle = false) {
    const itemId = item.dataset[idAttribute];
    const isSelected = item.classList.contains('selected');

    let newLastSelectedId = currentLastSelectedId;

    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    const isSelectionActive = itemSet.size > 0;
    
    const shouldToggle = forceToggle || (isTouchDevice && isSelectionActive) || event.ctrlKey || event.metaKey;

    if (event.shiftKey && currentLastSelectedId && !isTouchDevice) {
        const allItems = Array.from(document.querySelectorAll(itemSelector));
        const clickedIndex = allItems.indexOf(item);

        const lastSelectedDomElement = allItems.find(el => el.dataset[idAttribute] === currentLastSelectedId);
        const lastSelectedIndex = allItems.indexOf(lastSelectedDomElement);

        if (clickedIndex !== -1 && lastSelectedIndex !== -1) {
            if (!event.ctrlKey && !event.metaKey) {
                itemSet.forEach(el => el.classList.remove('selected'));
                itemSet.clear();
            }
            const start = Math.min(clickedIndex, lastSelectedIndex);
            const end = Math.max(clickedIndex, lastSelectedIndex);
            for (let i = start; i <= end; i++) {
                allItems[i].classList.add('selected');
                itemSet.add(allItems[i]);
            }
        }
    } else if (shouldToggle) {
        item.classList.toggle('selected');
        if (item.classList.contains('selected')) {
            itemSet.add(item);
            newLastSelectedId = itemId;
        } else {
            itemSet.delete(item);
            newLastSelectedId = itemSet.size > 0 ? Array.from(itemSet).pop().dataset[idAttribute] : null;
        }
    } else {
        const wasSelectedAndOnlyOne = isSelected && itemSet.size === 1;
        itemSet.forEach(el => el.classList.remove('selected'));
        itemSet.clear();
        if (!wasSelectedAndOnlyOne) {
            item.classList.add('selected');
            itemSet.add(item);
            newLastSelectedId = itemId;
        } else {
            newLastSelectedId = null;
        }
    }
    return newLastSelectedId;
}

/**
 * Handles a click on an image.
 * @param {Event} event - The click event.
 * @param {HTMLElement} imageItem - The image item that was clicked.
 * @param {boolean} [forceToggle=false] - Whether to force a toggle selection.
 */
window.handleImageClick = function(event, imageItem, forceToggle = false) {
    clearFilterSelections();
    clearReportSelections();

    lastSelectedImageId = handleItemSelection(event, imageItem, selectedImageElements, lastSelectedImageId, '.image-card, .list-item', 'id', forceToggle);

    document.querySelectorAll('.image-card, .list-item').forEach(item => {
        if (selectedImageElements.has(item)) {
            item.classList.add('selected');
        } else {
            item.classList.remove('selected');
        }
    });

    document.dispatchEvent(new CustomEvent('imageSelectionChanged', { detail: { selectedCount: selectedImageElements.size } }));
};

/**
 * Handles a click on a tag.
 * @param {Event} event - The click event.
 * @param {boolean} [forceToggle=false] - Whether to force a toggle selection.
 */
window.handleTagClick = function(event, forceToggle = false) {
    clearFilterSelections();
    clearReportSelections();
    const tagElement = event.currentTarget;
    lastSelectedTagId = handleItemSelection(event, tagElement, selectedTagElements, lastSelectedTagId, '.badge:not([data-report-id])', 'tagName', forceToggle);
    document.dispatchEvent(new CustomEvent('tagSelectionChanged', { detail: { selectedCount: selectedTagElements.size } }));
};

/**
 * Handles a click on a filter.
 * @param {Event} event - The click event.
 * @param {boolean} [forceToggle=false] - Whether to force a toggle selection.
 */
window.handleFilterClick = function(event, forceToggle = false) {
    clearImageSelections();
    clearTagSelections();
    clearReportSelections();
    const filterElement = event.currentTarget;
    lastSelectedSavedFilterId = handleItemSelection(event, filterElement, selectedSavedFilterElements, lastSelectedSavedFilterId, '.badge', 'filterId', forceToggle);
    document.dispatchEvent(new CustomEvent('filterSelectionChanged', { detail: { selectedCount: selectedSavedFilterElements.size } }));
};

/**
 * Handles a click on a report.
 * @param {Event} event - The click event.
 * @param {boolean} [forceToggle=false] - Whether to force a toggle selection.
 */
window.handleReportClick = function(event, forceToggle = false) {
    clearImageSelections();
    clearTagSelections();
    clearFilterSelections();
    const reportElement = event.currentTarget;
    lastSelectedReportId = handleItemSelection(event, reportElement, selectedReportElements, lastSelectedReportId, '.badge[data-report-id]', 'reportId', forceToggle);
    document.dispatchEvent(new CustomEvent('reportSelectionChanged', { detail: { selectedCount: selectedReportElements.size } }));
};

/**
 * Handles a double-click on a report.
 * @param {Event} event - The double-click event.
 */
window.handleReportDoubleClick = function(event) {
    event.stopPropagation();
    const reportId = event.currentTarget.dataset.reportId;
    if (reportId) {
        window.open(`/reports/${reportId}`, '_blank');
    }
};

document.addEventListener('DOMContentLoaded', function() {
    const appLayout = document.querySelector('.app-layout');
    const imageDetailsSidebar = document.getElementById('imageDetailsSidebar');

    document.addEventListener('click', (event) => {
        const clickedImage = event.target.closest('.image-card, .list-item');
        const clickedTag = event.target.closest('.badge:not([data-report-id])');
        const clickedFilter = event.target.closest('.badge');
        const clickedReport = event.target.closest('.badge[data-report-id]');
        const clickedNavbar = event.target.closest('.navbar');
        const clickedSidebarTitle = event.target.closest('.sidebar__section-title');
        const clickedButtonsSidebar = event.target.closest('.buttons-sidebar');
        const isClickInsideTagInput = event.target.closest('#new-tag-input');
        const isClickInsideReportGenerator = event.target.closest('.feature-bar');

        if (!clickedImage && !clickedTag && !clickedFilter && !clickedReport && !clickedNavbar && !clickedSidebarTitle && !isClickInsideTagInput && !isClickInsideReportGenerator && !clickedButtonsSidebar) {
            const isClickInsideFilterBarInput = event.target.closest('.feature-bar');
            const isClickInsideImageDetailsSidebar = imageDetailsSidebar && imageDetailsSidebar.contains(event.target);

            if (!isClickInsideFilterBarInput && !isClickInsideImageDetailsSidebar) {
                clearAllSelections();
            }
        }
    });

    const mainContentArea = document.getElementById('main-content-area');
    if (mainContentArea) {
        let mouseDownItem = null;

        mainContentArea.addEventListener('mousedown', (event) => {
            if (event.button !== 0) return;
            const imageItem = event.target.closest('.image-card, .list-item');
            mouseDownItem = imageItem;
        });

        mainContentArea.addEventListener('mouseup', (event) => {
            const imageItem = event.target.closest('.image-card, .list-item');
            if (imageItem && imageItem === mouseDownItem) {
                window.handleImageClick(event, imageItem);
            }
        });

        mainContentArea.addEventListener('dblclick', (event) => {
            const imageItem = event.target.closest('.image-card, .list-item');
            if (imageItem) {
                const imageId = imageItem.dataset.id;
                if (imageId) {
                    window.location.href = `/images/${imageId}`;
                }
            }
        });

        document.addEventListener('mouseup', () => {
            mouseDownItem = null;
        });

        let longPressTimer;
        let longPressTriggered = false;
        const longPressDelay = 500;

        mainContentArea.addEventListener('touchstart', (e) => {
            const imageItem = e.target.closest('.image-card, .list-item');
            if (imageItem) {
                longPressTriggered = false;
                longPressTimer = setTimeout(() => {
                    longPressTriggered = true;
                    window.handleImageClick(e, imageItem, true);
                    
                    if (navigator.vibrate) navigator.vibrate(50);
                }, longPressDelay);
            }
        }, { passive: true });

        const cancelLongPress = () => {
            if (longPressTimer) {
                clearTimeout(longPressTimer);
                longPressTimer = null;
            }
        };

        mainContentArea.addEventListener('touchend', (e) => {
            cancelLongPress();
            if (longPressTriggered) {
                if (e.cancelable) e.preventDefault();
                longPressTriggered = false;
            }
        });

        mainContentArea.addEventListener('touchmove', () => {
             cancelLongPress();
             longPressTriggered = false; 
        });
        
        mainContentArea.addEventListener('touchcancel', cancelLongPress);

        mainContentArea.addEventListener('contextmenu', (e) => {
            if (longPressTriggered) {
                e.preventDefault();
            }
        });
    }
});

window.selectedImageElements = selectedImageElements;
window.selectedTagElements = selectedTagElements;
window.selectedSavedFilterElements = selectedSavedFilterElements;
window.selectedReportElements = selectedReportElements;

window.clearImageSelections = clearImageSelections;
window.clearTagSelections = clearTagSelections;
window.clearFilterSelections = clearFilterSelections;
window.clearReportSelections = clearReportSelections;
window.clearAllSelections = clearAllSelections;

/**
 * Dispatches an event to add an image to the report generator.
 * @param {number} imageId - The ID of the image.
 * @param {string} imageUrl - The URL of the image.
 */
window.addImageToReportGenerator = function(imageId, imageUrl) {
    document.dispatchEvent(new CustomEvent('addImageToReportGenerator', {
        detail: { imageId: imageId, imageUrl: imageUrl }
    }));
};

/**
 * Dispatches an event to remove an image from the report generator.
 * @param {number} imageId - The ID of the image.
 */
window.removeImageFromReportGenerator = function(imageId) {
    document.dispatchEvent(new CustomEvent('removeImageFromReportGenerator', {
        detail: { imageId: imageId }
    }));
};
