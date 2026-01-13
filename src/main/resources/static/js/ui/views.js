/**
 * @file This file handles the view switching logic for the image gallery.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

document.addEventListener('DOMContentLoaded', function() {
    const imageGrid = document.getElementById('image-grid-main');
    const gridViewBtn = document.getElementById('grid-view-btn');
    const listViewBtn = document.getElementById('list-view-btn');

    /**
     * Sets the view type for the image gallery.
     *
     * @param {string} viewType - The type of view to set ('grid' or 'list').
     */
    function setView(viewType) {
        if (!imageGrid) return;

        if (viewType === 'list') {
            imageGrid.classList.add('list-view');
        } else {
            imageGrid.classList.remove('list-view');
        }
        const url = new URL(window.location);
        url.searchParams.set('view', viewType);
        window.history.pushState({}, '', url);

        if (imageGrid) imageGrid.innerHTML = '';
        window.currentPage = 0;
        window.allImagesLoaded = false;
    }

    const urlParams = new URLSearchParams(window.location.search);
    const initialView = urlParams.get('view') || 'grid';
    setView(initialView);

    if (gridViewBtn) {
        gridViewBtn.addEventListener('click', (event) => {
            event.preventDefault();
            setView('grid');
            window.loadImagesUntilScrollable();
        });
    }

    if (listViewBtn) {
        listViewBtn.addEventListener('click', (event) => {
            event.preventDefault();
            setView('list');
            window.loadImagesUntilScrollable();
        });
    }
});
