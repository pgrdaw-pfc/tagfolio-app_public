/**
 * @file This file handles the behavior of the navigation bar, including the hamburger menu and menu item actions.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

document.addEventListener('DOMContentLoaded', function() {
    const hamburgerMenu = document.getElementById('hamburger-menu');
    const navbarMenuGroup = document.querySelector('.navbar-menu-group');

    if (hamburgerMenu && navbarMenuGroup) {
        hamburgerMenu.addEventListener('click', function(event) {
            event.stopPropagation();
            navbarMenuGroup.classList.toggle('is-open');
        });
    }

    document.addEventListener('click', function(event) {
        if (navbarMenuGroup && !navbarMenuGroup.contains(event.target) && !hamburgerMenu.contains(event.target)) {
            navbarMenuGroup.classList.remove('is-open');
        }
    });
    
    const deleteSelectedBtn = document.getElementById('delete-selected-btn');
    const exportImagesBtn = document.getElementById('export-images-btn');
    const updateDatabaseBtn = document.getElementById('update-database-btn');
    const updateThumbnailsBtn = document.getElementById('update-thumbnails-btn');
    const zoomInBtn = document.getElementById('zoom-in-btn');
    const zoomOutBtn = document.getElementById('zoom-out-btn');

    const ZOOM_STORAGE_KEY = 'tagfolioImageZoom';
    const root = document.documentElement;

    const zoomStep = parseFloat(getComputedStyle(root).getPropertyValue('--image-scale-step')) || 0.1;
    const minZoom = parseFloat(getComputedStyle(root).getPropertyValue('--image-scale-min')) || 0.5;
    const maxZoom = parseFloat(getComputedStyle(root).getPropertyValue('--image-scale-max')) || 2.0;

    let currentZoom;

    /**
     * Updates the CSS variable for image zoom and saves it to localStorage.
     */
    function updateZoom() {
        root.style.setProperty('--image-scale', currentZoom.toFixed(2));
        localStorage.setItem(ZOOM_STORAGE_KEY, currentZoom.toFixed(2));
    }

    const storedZoom = localStorage.getItem(ZOOM_STORAGE_KEY);
    if (storedZoom) {
        currentZoom = parseFloat(storedZoom);
        if (currentZoom < minZoom) currentZoom = minZoom;
        if (currentZoom > maxZoom) currentZoom = maxZoom;
    } else {
        currentZoom = parseFloat(getComputedStyle(root).getPropertyValue('--image-scale')) || 1.0;
    }
    updateZoom();

    if (zoomInBtn) {
        zoomInBtn.addEventListener('click', (event) => {
            event.preventDefault();
            if (currentZoom < maxZoom) {
                currentZoom = Math.min(maxZoom, currentZoom + zoomStep);
                updateZoom();
            }
        });
    }

    if (zoomOutBtn) {
        zoomOutBtn.addEventListener('click', (event) => {
            event.preventDefault();
            if (currentZoom > minZoom) {
                currentZoom = Math.max(minZoom, currentZoom - zoomStep);
                updateZoom();
            }
        });
    }

    /**
     * Updates the state of the global delete button based on the current selection.
     */
    function updateGlobalDeleteButtonState() {
        let text = 'Delete';
        let disabled = true;

        const imagesSelected = window.selectedImageElements.size > 0;
        const tagsSelected = window.selectedTagElements.size > 0;
        const filtersSelected = window.selectedSavedFilterElements.size > 0;
        const reportsSelected = window.selectedReportElements.size > 0;

        if (reportsSelected) {
            text = 'Delete Selected Reports';
            disabled = false;
        } else if (imagesSelected && tagsSelected) {
            text = 'Remove Tags from Images';
            disabled = false;
        } else if (imagesSelected) {
            text = 'Delete Selected Images';
            disabled = false;
        } else if (tagsSelected) {
            text = 'Delete Selected Tags';
            disabled = false;
        } else if (filtersSelected) {
            text = 'Delete Selected Filters';
            disabled = false;
        }

        if (deleteSelectedBtn) {
            deleteSelectedBtn.textContent = text;
            deleteSelectedBtn.classList.toggle('disabled', disabled);
        }
    }

    /**
     * Updates the state of the export images button based on the current selection.
     */
    function updateExportImagesButtonState() {
        const imagesSelected = window.selectedImageElements.size > 0;
        if (exportImagesBtn) {
            exportImagesBtn.classList.toggle('disabled', !imagesSelected);
        }
    }

    document.addEventListener('imageSelectionChanged', () => {
        updateGlobalDeleteButtonState();
        updateExportImagesButtonState();
    });
    document.addEventListener('tagSelectionChanged', updateGlobalDeleteButtonState);
    document.addEventListener('filterSelectionChanged', updateGlobalDeleteButtonState);
    document.addEventListener('reportSelectionChanged', updateGlobalDeleteButtonState);

    if (deleteSelectedBtn) {
        deleteSelectedBtn.addEventListener('click', (event) => {
            event.preventDefault();
            if (!deleteSelectedBtn.classList.contains('disabled')) {
                const imagesSelected = window.selectedImageElements.size > 0;
                const tagsSelected = window.selectedTagElements.size > 0;
                const filtersSelected = window.selectedSavedFilterElements.size > 0;
                const reportsSelected = window.selectedReportElements.size > 0;

                if (reportsSelected) {
                    window.handleDeleteReports();
                } else if (imagesSelected && tagsSelected) {
                    window.handleRemoveTagsFromImages();
                } else if (imagesSelected) {
                    window.handleDeleteImages();
                } else if (tagsSelected) {
                    window.handleDeleteTags();
                } else if (filtersSelected) {
                    window.handleDeleteFilters();
                }
            }
        });
    }

    if (exportImagesBtn) {
        exportImagesBtn.addEventListener('click', (event) => {
            event.preventDefault();
            if (!exportImagesBtn.classList.contains('disabled')) {
                const imageIds = Array.from(window.selectedImageElements).map(el => el.dataset.id);
                if (imageIds.length > 0) {
                    let url = `/images/export?ids=${imageIds.join(',')}`;
                    if (window.isAnonymous && window.sharedFilterToken) {
                        url += `&token=${window.sharedFilterToken}`;
                    }
                    window.location.href = url;
                }
            }
        });
    }

    if (updateDatabaseBtn) {
        updateDatabaseBtn.addEventListener('click', async (event) => {
            event.preventDefault();
            
            window.displayConfirmationAlert({
                message: 'Are you sure you want to update the database from the originals folder? This process might take a while.',
                confirmText: 'Update',
                onConfirm: async () => {
                    const uploadOverlay = document.getElementById('upload-overlay');
                    if (uploadOverlay) {
                        uploadOverlay.classList.add('visible');
                    }
                    try {
                        const response = await fetch('/images/sync-database', {
                            method: 'POST',
                            headers: {
                                'Accept': 'application/json',
                                ...window.getCsrfHeaders()
                            }
                        });

                        if (!response.ok) {
                            const errorData = await response.json();
                            throw new Error(errorData.message || 'Failed to update database.');
                        }

                        const data = await response.json();
                        let message = `Database update complete.\nUploaded: ${data.uploaded.length}\nSkipped: ${data.skipped.length}\nConflicted: ${data.conflicted.length}`;
                        
                        if (data.conflicted.length > 0) {
                            message += '\nCheck console for conflict details.';
                            console.warn('Conflicts:', data.conflicted);
                        }

                        window.displayGlobalAlert('success', message);
                        
                        if (data.uploaded.length > 0 || data.conflicted.length > 0) {
                             setTimeout(() => window.location.reload(), 2000);
                        }

                    } catch (error) {
                        console.error('Error updating database:', error);
                        window.displayGlobalAlert('error', error.message || 'An error occurred while updating the database.');
                    } finally {
                        if (uploadOverlay) {
                            uploadOverlay.classList.remove('visible');
                        }
                    }
                }
            });
        });
    }

    if (updateThumbnailsBtn) {
        updateThumbnailsBtn.addEventListener('click', async (event) => {
            event.preventDefault();
            
            window.displayConfirmationAlert({
                message: 'Are you sure you want to regenerate all thumbnails? This process might take a while.',
                confirmText: 'Regenerate',
                onConfirm: async () => {
                    const uploadOverlay = document.getElementById('upload-overlay');
                    if (uploadOverlay) {
                        uploadOverlay.classList.add('visible');
                    }
                    try {
                        const response = await fetch('/images/regenerate-thumbnails', {
                            method: 'POST',
                            headers: {
                                'Accept': 'application/json',
                                ...window.getCsrfHeaders()
                            }
                        });

                        if (!response.ok) {
                            const errorData = await response.json();
                            throw new Error(errorData.message || 'Failed to regenerate thumbnails.');
                        }

                        const data = await response.json();
                        window.displayGlobalAlert('success', data.message);
                        
                        setTimeout(() => window.location.reload(), 2000);

                    } catch (error) {
                        console.error('Error regenerating thumbnails:', error);
                        window.displayGlobalAlert('error', error.message || 'An error occurred while regenerating thumbnails.');
                    } finally {
                        if (uploadOverlay) {
                            uploadOverlay.classList.remove('visible');
                        }
                    }
                }
            });
        });
    }

    updateGlobalDeleteButtonState();
    updateExportImagesButtonState();
});
