/**
 * @file This file handles the behavior of the sidebar, including collapsible sections, tag management, and metadata display.
 * @author Pablo Gimeno Ramallo <pgrdaw@gmail.com>
 * @since 2026-01-01
 */

/**
 * Deletes the selected tags.
 */
window.handleDeleteTags = async function() {
    const tagNamesToDelete = Array.from(window.selectedTagElements).map(el => el.dataset.tagName);
    if (tagNamesToDelete.length === 0) {
        window.displayGlobalAlert('info', 'No tags selected for deletion.');
        return;
    }

    window.displayConfirmationAlert({
        message: `Are you sure you want to delete ${tagNamesToDelete.length} selected tag(s)? This will remove them from ALL images.`,
        onConfirm: async () => {
            try {
                const response = await fetch('/images/tags/delete', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getCsrfHeaders()
                    },
                    body: JSON.stringify(tagNamesToDelete)
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to delete tags.');
                }
                window.displayGlobalAlert('success', `${tagNamesToDelete.length} tag(s) deleted successfully.`);
                window.clearAllSelections();
                window.updateSidebarContent();
                window.fetchAllTags();
            } catch (error) {
                console.error('Error deleting tags:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while deleting tags.');
            }
        }
    });
};


document.addEventListener('appInitialized', function() {
    async function fetchAndDisplay(url, imageIds, renderer, targetDivId) {
        const targetDiv = document.getElementById(targetDivId);
        if (!targetDiv) {
            console.error(`Target div with ID ${targetDivId} not found.`);
            return;
        }
        try {
            let payload;
            if (url === '/images/tags') {
                payload = {
                    imageIds: imageIds,
                    baseImageIds: (window.isAnonymous && window.sharedImageIds && window.sharedImageIds.length > 0) ? window.sharedImageIds : null
                };
            } else {
                payload = imageIds;
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(`Network response was not ok: ${response.status} ${response.statusText} - ${errorData}`);
            }
            const data = await response.json();
            renderer(data, imageIds, targetDivId);
        } catch (error) {
            console.error('Error fetching data:', error);
            targetDiv.innerHTML = `<p>Error loading data: ${error.message}</p>`;
        }
    }

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
     * Recursively renders a value into a friendly HTML string.
     * @param {*} value - The value to render.
     * @returns {string} The HTML string.
     */
    function renderFriendly(value) {
        if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
            let list = '<ul style="list-style-type: none; padding-left: 15px; border-left: 1px solid #4e5254; margin-top: 5px;">';
            for (const prop in value) {
                if (Object.prototype.hasOwnProperty.call(value, prop)) {
                    list += `<li><strong style="color: #87ceeb;">${prop}:</strong> ${renderFriendly(value[prop])}</li>`;
                }
            }
            list += '</ul>';
            return list;
        } else if (Array.isArray(value)) {
            return `[${value.map(item => renderFriendly(item)).join(', ')}]`;
        } else {
            // Check if value looks like a date string
            if (typeof value === 'string' && !isNaN(Date.parse(value)) && value.length > 10) {
                return `<span style="color: #98c379;">${formatDate(value)}</span>`;
            }
            return `<span style="color: #98c379;">${JSON.stringify(value)}</span>`;
        }
    }

    /**
     * Renders Exif data into the target div.
     * @param {Array<Object>} data - The Exif data.
     * @param {Array<number>} imageIds - The IDs of the images.
     * @param {string} targetDivId - The ID of the target div.
     */
    function renderExifData(data, imageIds, targetDivId) {
        const targetDiv = document.getElementById(targetDivId);
        if (!targetDiv) return;

        let html = '';
        if (imageIds.length === 0) {
            html += '<p>No images available to display Exiftool data.</p>';
            targetDiv.innerHTML = html;
            return;
        }
        if (!data || data.length === 0) {
            html += '<p>No Exiftool data to display.</p>';
            targetDiv.innerHTML = html;
            return;
        }
        data.forEach((metadataSet, index) => {
            html += `<pre><code>${JSON.stringify(metadataSet, null, 2)}</code></pre>`;
            if (index < data.length - 1) {
                html += '<hr>';
            }
        });
        targetDiv.innerHTML = html;
    }

    /**
     * Renders metadata into the target div.
     * @param {Array<Object>} data - The metadata.
     * @param {Array<number>} imageIds - The IDs of the images.
     * @param {string} targetDivId - The ID of the target div.
     */
    function renderMetadata(data, imageIds, targetDivId) {
        const targetDiv = document.getElementById(targetDivId);
        if (!targetDiv) return;

        let contentHtml = '';
        if (imageIds.length === 0) {
            contentHtml += '<p>No images selected to display metadata.</p>';
            targetDiv.innerHTML = contentHtml;
            return;
        }
        if (!data || data.length === 0) {
            contentHtml += '<p>No metadata to display.</p>';
            targetDiv.innerHTML = contentHtml;
            return;
        }

        data.forEach((imageMetadata, index) => {
            let imageHtml = '';
            if (imageMetadata) {
                let currentImageMetadataHtml = '';
                for (const displayName in window.displayMetadataMap) {
                    if (window.displayMetadataMap.hasOwnProperty(displayName)) {
                        const exiftoolKeys = window.displayMetadataMap[displayName].split(',').map(key => key.trim());
                        let value = null;
                        for (const key of exiftoolKeys) {
                            if (imageMetadata.hasOwnProperty(key) && imageMetadata[key] !== null && imageMetadata[key] !== '') {
                                value = imageMetadata[key];
                                break;
                            }
                        }

                        if (value !== null && String(value).trim() !== '' && String(value).trim().toUpperCase() !== 'N/A') {
                            currentImageMetadataHtml += `<div><strong>${displayName}:</strong> ${renderFriendly(value)}</div>`;
                        }
                    }
                }
                if (currentImageMetadataHtml !== '') {
                    imageHtml = `<div>${currentImageMetadataHtml}</div>`;
                }
            }
            contentHtml += imageHtml;
            if (index < data.length - 1) {
                contentHtml += '<hr>';
            }
        });
        targetDiv.innerHTML = contentHtml || '<p>No metadata to display.</p>';
    }

    /**
     * Renders tags into the target div.
     * @param {Array<Object>} data - The tag data.
     * @param {Array<number>} imageIds - The IDs of the images.
     * @param {string} targetDivId - The ID of the target div.
     */
    function renderTags(data, imageIds, targetDivId) {
        const targetDiv = document.getElementById(targetDivId);
        if (!targetDiv) return;

        const placeholder = "Add new tag(s)...";
        let html = '<div class="sidebar-scroll-container"><ul class="badge-list">';

        const hasSelection = window.selectedImageElements && window.selectedImageElements.size > 0;
        if (hasSelection && !window.isAnonymous) {
            html += `
                <li class="badge-list-item">
                    <input type="text" id="new-tag-input" class="tag-input-new" placeholder="${placeholder}">
                </li>
            `;
        }

        if (data && data.length > 0) {
            data.forEach(tag => {
                html += `
                    <li class="badge-list-item">
                        <span class="badge" data-tag-id="${tag.id}" data-tag-name="${tag.name}" draggable="true">
                            <span class="badge-name">${tag.name}</span>
                            <span class="badge-counter">${tag.counter}</span>
                        </span>
                    </li>
                `;
            });
        } else if (imageIds.length === 0 && !hasSelection) {
            html += '<li class="badge-list-item"><span class="badge">no tags</span></li>';
        } else if (data.length === 0 && hasSelection) {
            html += '<li class="badge-list-item"><span class="badge">no tags for selected images</span></li>';
        }

        html += '</ul></div>';
        targetDiv.innerHTML = html;

        const currentlySelectedTagNames = new Set(Array.from(window.selectedTagElements).map(el => el.dataset.tagName));
        const lastSelectedTagName = window.lastTagId;

        window.selectedTagElements.clear();
        window.lastTagId = null;

        targetDiv.querySelectorAll('.badge').forEach(badge => {
            if (currentlySelectedTagNames.has(badge.dataset.tagName)) {
                badge.classList.add('selected');
                window.selectedTagElements.add(badge);
                if (lastSelectedTagName === badge.dataset.tagName) {
                    window.lastTagId = badge.dataset.tagName;
                }
            }
            badge.addEventListener('click', window.handleTagClick);
            badge.addEventListener('dragstart', handleDragStart);
        });
        document.dispatchEvent(new CustomEvent('tagSelectionChanged', { detail: { selectedCount: window.selectedTagElements.size } }));


        const newTagInput = targetDiv.querySelector('.tag-input-new');
        if (newTagInput) {
            const autocompleteInputWrapper = document.createElement('div');
            autocompleteInputWrapper.className = 'autocomplete-input-wrapper';
            autocompleteInputWrapper.style.width = '100%';

            const autocompleteDropdown = document.createElement('div');
            autocompleteDropdown.className = 'autocomplete-dropdown';

            newTagInput.parentNode.insertBefore(autocompleteInputWrapper, newTagInput);
            autocompleteInputWrapper.appendChild(newTagInput);
            autocompleteInputWrapper.appendChild(autocompleteDropdown);

            let currentTagAutocompleteSelection = -1;

            function setAutocompleteVisible(visible) {
                const tagsSectionContent = document.getElementById('tags-section-content');
                if (visible) {
                    autocompleteDropdown.style.display = 'block';
                    if (tagsSectionContent) tagsSectionContent.style.overflowY = 'visible';
                } else {
                    autocompleteDropdown.style.display = 'none';
                    if (tagsSectionContent) tagsSectionContent.style.overflowY = 'auto';
                }
            }

            function updateTagAutocompleteSelection(newIndex) {
                const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');
                if (items.length > 0) {
                    if (currentTagAutocompleteSelection > -1) {
                        items[currentTagAutocompleteSelection].classList.remove('selected');
                    }

                    currentTagAutocompleteSelection = newIndex;
                    items[currentTagAutocompleteSelection].classList.add('selected');
                    items[currentTagAutocompleteSelection].scrollIntoView({ block: 'nearest' });
                }
            }

            function selectTagAutocompleteItem(item) {
                const tagName = item.textContent;
                handleTagAssociation(imageIds, [tagName]);
                newTagInput.value = '';
                setAutocompleteVisible(false);
                currentTagAutocompleteSelection = -1;
            }

            newTagInput.addEventListener('input', () => {
                const searchTerm = newTagInput.value.toLowerCase();
                autocompleteDropdown.innerHTML = '';
                currentTagAutocompleteSelection = -1;

                if (searchTerm.length === 0) {
                    setAutocompleteVisible(false);
                    return;
                }

                const matchingTags = allAvailableTags.filter(tag => tag.toLowerCase().includes(searchTerm));

                matchingTags.forEach(tag => {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-item';
                    item.textContent = tag;
                    item.addEventListener('mousedown', (event) => {
                        event.preventDefault();
                        selectTagAutocompleteItem(item);
                    });
                    autocompleteDropdown.appendChild(item);
                });

                if (matchingTags.length > 0) {
                    setAutocompleteVisible(true);
                } else {
                    setAutocompleteVisible(false);
                }
            });

            newTagInput.addEventListener('focus', () => {
                const event = new Event('input');
                newTagInput.dispatchEvent(event);
            });

            newTagInput.addEventListener('blur', () => {
                setTimeout(() => {
                    setAutocompleteVisible(false);
                    currentTagAutocompleteSelection = -1;
                }, 100);
            });

            newTagInput.addEventListener('keydown', (event) => {
                const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');

                if (event.key === 'ArrowDown') {
                    event.preventDefault();
                    if (items.length > 0) {
                        const newIndex = (currentTagAutocompleteSelection + 1) % items.length;
                        updateTagAutocompleteSelection(newIndex);
                        setAutocompleteVisible(true);
                    }
                } else if (event.key === 'ArrowUp') {
                    event.preventDefault();
                    if (items.length > 0) {
                        const newIndex = (currentTagAutocompleteSelection - 1 + items.length) % items.length;
                        updateTagAutocompleteSelection(newIndex);
                        setAutocompleteVisible(true);
                    }
                } else if (event.key === 'Enter') {
                    event.preventDefault();
                    if (currentTagAutocompleteSelection > -1 && items.length > 0) {
                        selectTagAutocompleteItem(items[currentTagAutocompleteSelection]);
                    } else {
                        const inputTags = newTagInput.value.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
                        if (inputTags.length > 0) {
                            handleTagAssociation(imageIds, inputTags);
                            newTagInput.value = '';
                            setAutocompleteVisible(false);
                        } else {
                            window.displayGlobalAlert('warning', 'Please enter at least one tag name.');
                        }
                    }
                }
            });
        }
    }

    /**
     * Associates tags with a list of images.
     * @param {Array<number>} imageIds - The IDs of the images.
     * @param {Array<string>} tagNames - The names of the tags to associate.
     */
    async function handleTagAssociation(imageIds, tagNames) {
        if (tagNames.length === 0 || imageIds.length === 0) return;
        const tagsToProcess = tagNames.map(name => name.trim()).filter(tag => tag.length > 0);
        if (tagsToProcess.length === 0) return;

        let successCount = 0;
        let errorCount = 0;
        for (const tagName of tagsToProcess) {
            try {
                const response = await fetch('/images/tags/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        ...getCsrfHeaders()
                    },
                    body: JSON.stringify({ imageIds: imageIds, tagName: tagName })
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || `Failed to add tag "${tagName}".`);
                }
                successCount++;
            } catch (error) {
                console.error(`Error adding tag "${tagName}":`, error);
                window.displayGlobalAlert('error', error.message || `Failed to add tag "${tagName}".`);
                errorCount++;
            }
        }
        if (successCount > 0) {
            window.displayGlobalAlert('success', `${successCount} tag(s) added successfully.`);
        }
        if (errorCount > 0) {
            window.displayGlobalAlert('warning', `${errorCount} tag(s) failed to add.`);
        }
        const newTagInput = document.querySelector('.tag-input-new');
        if (newTagInput) newTagInput.value = '';
        window.updateSidebarContent();
    }

    /**
     * Handles the drag start event for a tag.
     * @param {DragEvent} event - The drag event.
     */
    function handleDragStart(event) {
        const draggedTagName = event.target.dataset.tagName;
        const selectedTagNames = Array.from(window.selectedTagElements).map(el => el.dataset.tagName);

        if (window.selectedTagElements.size > 1 && selectedTagNames.includes(draggedTagName)) {
            event.dataTransfer.setData('application/x-tag-names', JSON.stringify(selectedTagNames));
            event.dataTransfer.effectAllowed = 'copy';
        } else {
            event.dataTransfer.setData('text/plain', draggedTagName);
            event.dataTransfer.effectAllowed = 'copy';
        }
    }

    /**
     * Updates the content of the sidebar.
     */
    window.updateSidebarContent = function(event) {
        if (window.selectionInProgress) {
            return;
        }

        let selectedImageIds = Array.from(window.selectedImageElements).map(item => item.dataset.id).filter(id => id !== null);
        const hasSelection = selectedImageIds.length > 0;

        const tagsSectionContent = document.getElementById('tags-section-content');
        const metadataSectionContent = document.getElementById('metadata-section-content');
        const exiftoolSectionContent = document.getElementById('exiftool-section-content');

        if (!window.isAnonymous) {
            if (hasSelection) {
                if (metadataSectionContent) {
                    fetchAndDisplay('/images/metadata', selectedImageIds, renderMetadata, 'metadata-section-content');
                }
                if (exiftoolSectionContent) {
                    fetchAndDisplay('/images/exif', selectedImageIds, renderExifData, 'exiftool-section-content');
                }
            } else {
                if (metadataSectionContent) metadataSectionContent.innerHTML = '<p>No image selected to display metadata.</p>';
                if (exiftoolSectionContent) exiftoolSectionContent.innerHTML = '<p>No image selected to display Exiftool data.</p>';
            }
        } else {
            if (metadataSectionContent) metadataSectionContent.innerHTML = '<p>Metadata not available for anonymous users.</p>';
            if (exiftoolSectionContent) exiftoolSectionContent.innerHTML = '<p>Exiftool data not available for anonymous users.</p>';
        }

        if (tagsSectionContent) {
            let tagsImageIds;
            if (hasSelection) {
                tagsImageIds = selectedImageIds;
            } else {
                tagsImageIds = window.isFilterActive ? window.filteredMatchingIds : [];
            }
            fetchAndDisplay('/images/tags', tagsImageIds, renderTags, 'tags-section-content');
        }
    }

    document.addEventListener('filterApplied', () => {
        try {
            if (typeof requestAnimationFrame === 'function') {
                requestAnimationFrame(() => window.updateSidebarContent());
            } else {
                window.updateSidebarContent();
            }
        } catch (e) {
        }
    });

    const COLLAPSE_STATE_KEY = 'sidebarCollapseStates';
    const collapseToggles = document.querySelectorAll('.sidebar__collapse-toggle');
    const contentsSidebar = document.getElementById('contents-sidebar');
    const topBarReport = document.getElementById('top-bar-reports');
    const topBarFilter = document.getElementById('top-bar-filters');

    let collapseStates = JSON.parse(localStorage.getItem(COLLAPSE_STATE_KEY)) || {};

    /**
     * Updates the visibility of the contents sidebar.
     */
    function updateContentsSidebarVisibility() {
        if (!contentsSidebar) return;
        const visibleSections = contentsSidebar.querySelectorAll('.sidebar__section:not(.hidden)');
        if (visibleSections.length > 0) {
            contentsSidebar.classList.remove('hidden');
        } else {
            contentsSidebar.classList.add('hidden');
        }
    }

    /**
     * Saves the collapse state of a sidebar section to localStorage.
     * @param {string} sectionId - The ID of the section.
     * @param {boolean} isVisible - Whether the section is visible.
     */
    window.saveCollapseState = function(sectionId, isVisible) {
        collapseStates[sectionId] = isVisible;
        localStorage.setItem(COLLAPSE_STATE_KEY, JSON.stringify(collapseStates));
    }

    /**
     * Applies the collapse state to a sidebar section.
     * @param {string} sectionId - The ID of the section.
     * @param {boolean} isVisible - Whether the section is visible.
     */
    window.applyCollapseState = function(sectionId, isVisible) {
        const toggleButton = document.querySelector(`.sidebar__collapse-toggle[data-target-id="${sectionId}-section-content"]`);
    
        if (sectionId === 'reports' || sectionId === 'filters') {
            const topBar = document.getElementById(`top-bar-${sectionId}`);
            if (topBar && toggleButton) {
                topBar.classList.toggle('d-none', !isVisible);
                toggleButton.classList.toggle('active', isVisible);
            }
        } else {
            const sectionElement = document.querySelector(`.sidebar__section[data-section-id="${sectionId}"]`);
            if (sectionElement && toggleButton) {
                sectionElement.classList.toggle('hidden', !isVisible);
                toggleButton.classList.toggle('active', isVisible);
            }
        }
    
        document.dispatchEvent(new CustomEvent('sidebarCollapseChanged', {
            detail: { sectionId: sectionId, isVisible: isVisible }
        }));
    
        updateContentsSidebarVisibility();
    }

    document.querySelectorAll('.sidebar__collapse-toggle').forEach(button => {
        const contentId = button.dataset.targetId;
        const sectionId = contentId.replace('-section-content', '');
    
        if (window.isAnonymous && (sectionId === 'metadata' || sectionId === 'exiftool' || sectionId === 'reports')) {
            const section = document.querySelector(`.sidebar__section[data-section-id="${sectionId}"]`);
            if(section) section.style.display = 'none';
            button.style.display = 'none';
            return;
        }
    
        const defaultVisible = true;
        const isVisible = collapseStates.hasOwnProperty(sectionId) ? collapseStates[sectionId] : defaultVisible;
        window.applyCollapseState(sectionId, isVisible);
    });

    collapseToggles.forEach(button => {
        button.addEventListener('click', function() {
            const contentId = this.dataset.targetId;
            const sectionId = contentId.replace('-section-content', '');
            const isCurrentlyVisible = this.classList.contains('active');
            window.applyCollapseState(sectionId, !isCurrentlyVisible);
            window.saveCollapseState(sectionId, !isCurrentlyVisible);
        });
    });

    window.updateSidebarContent();

    document.addEventListener('imageSelectionChanged', window.updateSidebarContent);
});
