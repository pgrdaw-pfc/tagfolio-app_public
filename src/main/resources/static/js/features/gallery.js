/**
 * @file This file handles the image gallery behavior, including loading, sorting, and filtering images.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * Formats a date string to "yyyy-mm-dd hh:mm:ss" (24h).
 * @param {string} dateString - The date string to format.
 * @returns {string} The formatted date string.
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * Creates an image card element.
 * @param {Object} image - The image object.
 * @param {boolean} isMatching - Whether the image is matching the current filter.
 * @returns {HTMLElement} The image card element.
 */
function createImageCard(image, isMatching) {
    const imageCard = document.createElement('div');
    imageCard.className = 'image-card';
    imageCard.dataset.id = image.id;
    if (isMatching) {
        imageCard.classList.add('matching');
    } else {
        imageCard.classList.add('not-matching');
    }

    let detailsHtml = '';
    for (const displayName of Object.keys(window.sortableFields)) {
        let value = 'N/A';

        switch (displayName) {
            case 'Filename':
                value = image.originalFileName;
                break;
            case 'Created':
                value = formatDate(image.createdAt);
                break;
            case 'Modified':
                value = formatDate(image.modifiedAt);
                break;
            case 'Imported':
                value = formatDate(image.importedAt);
                break;
            case 'Rating':
                value = image.rating || 'N/A';
                break;
        }

        detailsHtml += `
            <div class="list-item-sort-field">
                <strong>${displayName}:</strong> <span>${value}</span>
            </div>
        `;
    }

    imageCard.innerHTML = `
        <img src="/images/thumbnail/${image.id}" alt="${image.originalFileName}" loading="lazy">
        <div class="image-card-details">
            ${detailsHtml}
        </div>
    `;
    return imageCard;
}

/**
 * Fetches and renders a batch of images.
 */
async function fetchAndRenderImages() {
    
    if (window.isLoading || window.allImagesLoaded) {
        return;
    }
    window.isLoading = true;

    try {
        let images = [];
        let matchingIdsSet = new Set(window.filteredMatchingIds.map(String));

        if (window.isFilterActive) {
            const allFilteredIds = [...window.filteredMatchingIds, ...window.filteredNonMatchingIds];
            const start = window.currentPage * window.batchSize;
            const end = start + window.batchSize;
            const batchIds = allFilteredIds.slice(start, end);

            if (batchIds.length > 0) {
                const response = await fetch(`/images/by-ids?sort=${window.sortField}&direction=${window.sortDirection}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify(batchIds)
                });
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                images = await response.json();

                const orderMap = new Map(batchIds.map((id, index) => [String(id), index]));
                images.sort((a, b) => orderMap.get(String(a.id)) - orderMap.get(String(b.id)));
            } else {
                window.allImagesLoaded = true;
            }
        } else {
            const response = await fetch(`/images/paginated?page=${window.currentPage}&size=${window.batchSize}&sort=${window.sortField}&direction=${window.sortDirection}`, {
                headers: {
                    'Accept': 'application/json',
                    ...window.getCsrfHeaders()
                }
            });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            images = await response.json();
        }

        if (images.length === 0) {
            window.allImagesLoaded = true;
            return;
        }

        const imageGrid = document.getElementById('image-grid-main');
        if (imageGrid) {
            
            images.forEach(image => {
                const isMatching = !window.isFilterActive || matchingIdsSet.has(image.id.toString());
                const imageCard = createImageCard(image, isMatching);
                imageGrid.appendChild(imageCard);
            });
            
        }


        window.currentPage++;
    } catch (error) {
        console.error('Error fetching images:', error);
    } finally {
        window.isLoading = false;
        
    }
}

/**
 * Loads images until the page is scrollable and the grid is full.
 */
window.loadImagesUntilScrollable = async function() {
    
    const mainContentArea = document.getElementById('main-content-area');
    if (!mainContentArea) return;

    const imageGrid = document.getElementById('image-grid-main');
    if (!imageGrid) return;

    while (!window.allImagesLoaded) {
        
        await fetchAndRenderImages();

        const lastImage = imageGrid.lastElementChild;
        if (!lastImage) {
            break;
        }

        const lastImageRect = lastImage.getBoundingClientRect();
        const mainContentAreaRect = mainContentArea.getBoundingClientRect();

        if (lastImageRect.top >= mainContentAreaRect.bottom) {
            
            break;
        }
    }

    if (!window.appHasInitialized) {
        document.dispatchEvent(new CustomEvent('appInitialized'));
        window.appHasInitialized = true;
    }
    
};

/**
 * Updates the checkmarks for the sort buttons.
 */
function updateSortCheckmarks() {
    document.querySelectorAll('.sort-checkmark').forEach(span => {
        span.textContent = '';
    });

    const activeSortFieldSpan = document.querySelector(`.sort-checkmark[data-sort-field-check="${window.sortField}"]`);
    if (activeSortFieldSpan) {
        activeSortFieldSpan.textContent = '✓';
    }

    const activeSortDirectionSpan = document.querySelector(`.sort-checkmark[data-sort-direction-check="${window.sortDirection}"]`);
    if (activeSortDirectionSpan) {
        activeSortDirectionSpan.textContent = '✓';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const mainContentArea = document.getElementById('main-content-area');

    if (mainContentArea) {
        mainContentArea.addEventListener('scroll', () => {
            if (mainContentArea.scrollTop + mainContentArea.clientHeight >= mainContentArea.scrollHeight - 500) {
                
                fetchAndRenderImages();
            }
        });
    }

    const sortFieldButtons = document.querySelectorAll('.sort-field-btn');
    const sortDirectionButtons = document.querySelectorAll('.sort-direction-btn');

    /**
     * Applies the current sort settings to the gallery.
     */
    function applySort() {
        if (window.lastSuccessfullyAppliedFilterExpression && window.lastSuccessfullyAppliedFilterExpression.length > 0) {
            window.filterImages(window.lastSuccessfullyAppliedFilterExpression);
        } else {
            window.isFilterActive = false;
            window.filteredMatchingIds = [];
            window.filteredNonMatchingIds = [];

            const imageGrid = document.getElementById('image-grid-main');
            if (imageGrid) imageGrid.innerHTML = '';
            window.currentPage = 0;
            window.allImagesLoaded = false;
            window.loadImagesUntilScrollable();
        }

        const url = new URL(window.location);
        url.searchParams.set('sort', window.sortField);
        url.searchParams.set('direction', window.sortDirection);
        window.history.pushState({}, '', url);
        updateSortCheckmarks();
    }

    sortFieldButtons.forEach(button => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            window.sortField = button.dataset.sortField;
            applySort();
        });
    });

    sortDirectionButtons.forEach(button => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            window.sortDirection = button.dataset.sortDirection;
            applySort();
        });
    });

    updateSortCheckmarks();
});
