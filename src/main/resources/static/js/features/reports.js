/**
 * @file This file handles the behavior of the reports feature, including generating, deleting, and sharing reports.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

document.addEventListener('DOMContentLoaded', function() {
    const generateReportBtn = document.getElementById('generate-report-btn');
    if (generateReportBtn) {
        generateReportBtn.addEventListener('click', async function() {
            const imagesToAddToReport = Array.from(window.selectedImageElements).map(el => ({
                id: parseInt(el.dataset.id),
                url: el.querySelector('img').src
            }));

            window.clearAllSelections();

            if (typeof window.applyCollapseState === 'function') {
                window.applyCollapseState('reports', true);
            } else {
                console.error('window.applyCollapseState is not defined. Cannot uncollapse reports section.');
            }

            imagesToAddToReport.forEach(image => {
                window.addImageToReportGenerator(image.id, image.url);
            });
        });
    }
});

/**
 * Deletes the selected reports.
 */
window.handleDeleteReports = async function() {
    if (window.selectedReportElements.size === 0) {
        window.displayGlobalAlert('info', 'No reports selected for deletion.');
        return;
    }

    const reportIdsToDelete = Array.from(window.selectedReportElements).map(el => el.dataset.reportId);

    window.displayConfirmationAlert({
        message: `Are you sure you want to delete ${reportIdsToDelete.length} selected report(s)?`,
        onConfirm: async () => {
            try {
                const response = await fetch('/api/reports', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify(reportIdsToDelete)
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to delete reports.');
                }

                window.displayGlobalAlert('success', `${reportIdsToDelete.length} report(s) deleted successfully.`);
                document.dispatchEvent(new CustomEvent('reportsDeleted'));
            } catch (error) {
                console.error('Error deleting reports:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while deleting reports.');
            }
        }
    });
};

/**
 * Shares a report.
 * @param {number} reportId - The ID of the report.
 * @param {string} reportName - The name of the report.
 */
window.handleShareReport = async function(reportId, reportName) {
    try {
        const response = await fetch(`/api/reports/${reportId}/share`, {
            method: 'POST',
            headers: {
                'Accept': 'text/plain',
                ...window.getCsrfHeaders()
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to generate shareable link.');
        }

        const shareableUrl = await response.text();
        copyToClipboard(shareableUrl);
        window.displayGlobalAlert('success', `Shareable link for "${reportName}" copied to clipboard!`);
        
        await window.fetchAndRenderUserReports();

    } catch (error) {
        console.error('Error generating shareable link:', error);
        window.displayGlobalAlert('error', error.message || 'An error occurred while generating the shareable link.');
    }
};

/**
 * Copies text to the clipboard.
 * @param {string} text - The text to copy.
 */
function copyToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).catch(err => {
            console.error('Failed to copy text using clipboard API: ', err);
        });
    } else {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.left = '-9999px';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        try {
            document.execCommand('copy');
        } catch (err) {
            console.error('Failed to copy text (fallback): ', err);
        }
        document.body.removeChild(textarea);
    }
}

/**
 * Handles a double-click on a report item.
 * @param {Event} event - The double-click event.
 */
window.handleReportDoubleClick = async function(event) {
    event.preventDefault();

    const reportId = this.dataset.reportId;
    if (!reportId) {
        console.error('Report ID not found for double-clicked element.');
        return;
    }
    
    try {
        const response = await fetch(`/api/reports/${reportId}/details`, {
            headers: {
                'Accept': 'application/json',
                ...window.getCsrfHeaders()
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Failed to fetch report details.');
        }

        const reportDetails = await response.json();
        

        if (typeof window.applyCollapseState === 'function') {
            window.applyCollapseState('reports', true);
            
        } else {
            console.warn('window.applyCollapseState is not defined. Cannot ensure reports section is uncollapsed.');
        }

        if (typeof window.loadReportIntoGenerator === 'function') {
            window.loadReportIntoGenerator(reportDetails);
            
        } else {
            console.error('window.loadReportIntoGenerator is not defined. Cannot load report into generator bar.');
        }

    } catch (error) {
        console.error('Error loading report into generator:', error);
        window.displayGlobalAlert('error', error.message || 'An error occurred while loading the report.');
    }
};

/**
 * Initializes the report configuration UI.
 * @param {number} reportId - The ID of the report.
 * @param {boolean} isEditable - Whether the report is editable.
 */
window.initializeReportConfig = function(reportId, isEditable) {
    if (typeof Sortable === 'undefined') {
        console.error("Sortable.js is not loaded. Cannot initialize report configuration.");
        return;
    }

    if (isEditable) {
        const reportNameInput = document.getElementById('reportNameInput');
        const reportTypeSelect = document.getElementById('reportTypeSelect');
        const reportImagesContainer = document.getElementById('report-images-container');
        const saveChangesBtn = document.getElementById('saveChangesBtn');
        const shareReportBtn = document.getElementById('shareReportBtn');

        let currentImageOrder = [];
        let shareLink = null;

        if (reportImagesContainer) {
            currentImageOrder = Array.from(reportImagesContainer.children).map(item => item.dataset.id);

            new Sortable(reportImagesContainer, {
                animation: 150,
                ghostClass: 'sortable-ghost',
                onEnd: function (evt) {
                    currentImageOrder = Array.from(reportImagesContainer.children).map(item => item.dataset.id);
                },
            });
        } else {
            console.warn("reportImagesContainer not found. Sortable.js not initialized.");
        }


        async function saveChanges() {
            const newReportName = reportNameInput.value;
            const newReportTypeName = reportTypeSelect.value;
            const newImageOrder = Array.from(reportImagesContainer.children).map(item => item.dataset.id);

            let successCount = 0;
            let errorMessages = [];

            async function makePutRequest(url, body, contentType) {
                try {
                    const response = await fetch(url, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': contentType,
                            ...window.getCsrfHeaders()
                        },
                        body: body
                    });
                    if (!response.ok) {
                        const errorData = await response.json();
                        throw new Error(errorData.message || `Failed to update ${url.split('/').pop()}.`);
                    }
                    return true;
                } catch (error) {
                    errorMessages.push(`${url.split('/').pop()}: ${error.message}`);
                    console.error(`Error updating ${url.split('/').pop()}:`, error);
                    return false;
                }
            }

            if (await makePutRequest(`/api/reports/${reportId}/name`, newReportName, 'text/plain')) {
                successCount++;
            }

            if (await makePutRequest(`/api/reports/${reportId}/type`, newReportTypeName, 'text/plain')) {
                successCount++;
            }

            if (await makePutRequest(`/api/reports/${reportId}/images/reorder`, JSON.stringify(newImageOrder), 'application/json')) {
                successCount++;
            }

            if (successCount === 3) {
                window.displayGlobalAlert('success', 'All changes saved successfully!');
                return true;
            } else {
                window.displayGlobalAlert('error', 'Errors occurred during save:\n' + errorMessages.join('\n'));
                return false;
            }
        }

        async function shareReport() {
            if (shareLink) {
                copyToClipboard(shareLink);
                window.displayGlobalAlert('success', 'Shareable link copied to clipboard:\n' + shareLink);
                return;
            }

            const saved = await saveChanges();
            if (!saved) {
                return;
            }

            try {
                const response = await fetch(`/api/reports/${reportId}/share`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to generate shareable link.');
                }
                shareLink = await response.text();

                copyToClipboard(shareLink);
                window.displayGlobalAlert('success', 'Shareable link copied to clipboard:\n' + shareLink);
            } catch (error) {
                console.error('Error sharing report:', error);
                window.displayGlobalAlert('error', 'Error sharing report: ' + (error.message || 'Unknown error'));
            }
        }

        saveChangesBtn.addEventListener('click', saveChanges);
        shareReportBtn.addEventListener('click', shareReport);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const reportGeneratorBar = document.getElementById('top-bar-reports');
    if (!reportGeneratorBar) return;

    const reportImagesContainer = document.getElementById('report-images-container');
    const addSelectedImagesBtn = document.getElementById('add-selected-images-btn');
    const removeSelectedImagesBtn = document.getElementById('remove-selected-images-btn');
    const clearReportImagesBtn = document.getElementById('clear-report-images-btn');
    const saveReportBtn = document.getElementById('save-report-btn');
    const shareReportBtn = document.getElementById('share-report-btn');
    
    const reportNameInput = document.getElementById('report-name-input');
    const reportTypeSelect = document.getElementById('report-type-select');

    let currentUiImageIds = [];
    let selectedThumbnails = new Set();
    let lastSelectedThumbnailId = null;

    let lastCreatedReportId = null;
    let lastCreatedReportShareableLink = null;
    let currentReportName = null;
    let currentReportTypeId = null;
    let currentReportImageOrder = [];

    function formatDateTime(date) {
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const seconds = date.getSeconds().toString().padStart(2, '0');
        return `${year}-${month}-${day}-${hours}-${minutes}-${seconds}`;
    }

    function getDefaultReportName() {
        return formatDateTime(new Date());
    }

    function updateReportGeneratorButtonStates() {
        const hasImages = currentUiImageIds.length > 0;
        const hasSelection = selectedThumbnails.size > 0;
        if (saveReportBtn) saveReportBtn.disabled = !hasImages;
        if (shareReportBtn) shareReportBtn.disabled = !hasImages;
        if (removeSelectedImagesBtn) removeSelectedImagesBtn.disabled = !hasSelection;
    }

    function clearThumbnailSelections() {
        selectedThumbnails.forEach(el => el.classList.remove('selected'));
        selectedThumbnails.clear();
        lastSelectedThumbnailId = null;
        updateReportGeneratorButtonStates();
    }

    function handleThumbnailClick(event, thumbnail) {
        const thumbnailId = thumbnail.dataset.imageId;
        const isSelected = thumbnail.classList.contains('selected');

        if (event.shiftKey && lastSelectedThumbnailId) {
            const allThumbnails = Array.from(reportImagesContainer.children);
            const clickedIndex = allThumbnails.indexOf(thumbnail);
            const lastSelectedIndex = allThumbnails.findIndex(t => t.dataset.imageId === lastSelectedThumbnailId);

            if (clickedIndex !== -1 && lastSelectedIndex !== -1) {
                if (!event.ctrlKey && !event.metaKey) clearThumbnailSelections();
                const start = Math.min(clickedIndex, lastSelectedIndex);
                const end = Math.max(clickedIndex, lastSelectedIndex);
                for (let i = start; i <= end; i++) {
                    allThumbnails[i].classList.add('selected');
                    selectedThumbnails.add(allThumbnails[i]);
                }
            }
        } else if (event.ctrlKey || event.metaKey) {
            thumbnail.classList.toggle('selected');
            if (thumbnail.classList.contains('selected')) {
                selectedThumbnails.add(thumbnail);
                lastSelectedThumbnailId = thumbnailId;
            } else {
                selectedThumbnails.delete(thumbnail);
                lastSelectedThumbnailId = selectedThumbnails.size > 0 ? Array.from(selectedThumbnails)[selectedThumbnails.size - 1].dataset.imageId : null;
            }
        } else {
            const wasSelectedAndOnlyOne = isSelected && selectedThumbnails.size === 1;
            clearThumbnailSelections();
            if (!wasSelectedAndOnlyOne) {
                thumbnail.classList.add('selected');
                selectedThumbnails.add(thumbnail);
                lastSelectedThumbnailId = thumbnailId;
            }
        }
        updateReportGeneratorButtonStates();
    }

    function renderImageThumbnail(imageId, imageUrl) {
        if (currentUiImageIds.includes(imageId)) return;

        const thumbnailDiv = document.createElement('div');
        thumbnailDiv.classList.add('report-image-thumbnail');
        thumbnailDiv.dataset.imageId = imageId;
        thumbnailDiv.innerHTML = `<img src="${imageUrl}" alt="Image ${imageId}">`;

        thumbnailDiv.addEventListener('click', (event) => handleThumbnailClick(event, thumbnailDiv));

        reportImagesContainer.appendChild(thumbnailDiv);
        currentUiImageIds.push(imageId);
        updateReportGeneratorButtonStates();
    }

    function removeSelectedImages() {
        const idsToRemove = new Set();
        selectedThumbnails.forEach(thumbnail => {
            idsToRemove.add(parseInt(thumbnail.dataset.imageId));
            reportImagesContainer.removeChild(thumbnail);
        });
        currentUiImageIds = currentUiImageIds.filter(id => !idsToRemove.has(id));
        clearThumbnailSelections();
        updateReportGeneratorButtonStates();
    }

    if (reportImagesContainer) {
        new Sortable(reportImagesContainer, {
            animation: 150,
            ghostClass: 'sortable-ghost',
            onEnd: function () {
                currentUiImageIds = Array.from(reportImagesContainer.children)
                                         .map(thumbnail => parseInt(thumbnail.dataset.imageId));
                updateReportGeneratorButtonStates();
            }
        });

        reportImagesContainer.addEventListener('dragover', (event) => {
            event.preventDefault();
            reportImagesContainer.classList.add('drag-over');
        });

        reportImagesContainer.addEventListener('dragleave', () => {
            reportImagesContainer.classList.remove('drag-over');
        });

        reportImagesContainer.addEventListener('drop', async (event) => {
            event.preventDefault();
            reportImagesContainer.classList.remove('drag-over');
            clearThumbnailSelections();

            const reportId = event.dataTransfer.getData('application/x-tagfolio-report-id');
            if (reportId) {
                try {
                    const response = await fetch(`/api/reports/${reportId}/details`, {
                        headers: { 'Accept': 'application/json', ...window.getCsrfHeaders() }
                    });
                    if (!response.ok) throw new Error((await response.json()).message || 'Failed to fetch report details.');
                    const reportDetails = await response.json();
                    if (typeof window.loadReportIntoGenerator === 'function') {
                        window.loadReportIntoGenerator(reportDetails);
                    }
                } catch (error) {
                    console.error('Error loading dropped report:', error);
                    window.displayGlobalAlert('error', 'Failed to load the dropped report.');
                }
            } else {
                const imageId = event.dataTransfer.getData('text/plain');
                const imageUrl = event.dataTransfer.getData('text/uri-list');
                if (imageId && imageUrl) {
                    renderImageThumbnail(parseInt(imageId), imageUrl);
                }
            }
        });
    }

    async function populateReportTypes() {
        if (!reportTypeSelect) return;
        reportTypeSelect.innerHTML = '';
        try {
            const response = await fetch('/api/reports/types', {
                headers: { 'Accept': 'application/json', ...window.getCsrfHeaders() }
            });
            if (!response.ok) throw new Error('Failed to fetch report types.');
            const reportTypes = await response.json();
            reportTypes.forEach(type => {
                const option = document.createElement('option');
                option.value = type.id;
                option.textContent = type.name;
                reportTypeSelect.appendChild(option);
            });
        } catch (error) {
            console.error('Error fetching report types:', error);
            window.displayGlobalAlert('error', error.message || 'Failed to load report types.');
        }
    }

    function arraysEqual(a, b) {
        if (a === b) return true;
        if (a == null || b == null) return false;
        if (a.length !== b.length) return false;
        for (let i = 0; i < a.length; i++) {
            if (a[i] !== b[i]) return false;
        }
        return true;
    }

    function hasUnsavedChanges() {
        if (lastCreatedReportId === null) {
            return currentUiImageIds.length > 0;
        }
        const nameChanged = (reportNameInput.value.trim() || getDefaultReportName()) !== currentReportName;
        const typeChanged = parseInt(reportTypeSelect.value) !== currentReportTypeId;
        const imagesChanged = !arraysEqual(currentUiImageIds, currentReportImageOrder);
        return nameChanged || typeChanged || imagesChanged;
    }

    async function createNewReport() {
        let nameToUse = reportNameInput.value.trim() || getDefaultReportName();
        const reportTypeId = parseInt(reportTypeSelect.value);
        const imageIds = currentUiImageIds;

        if (!nameToUse || !reportTypeId || imageIds.length === 0) {
            window.displayGlobalAlert('warning', 'Please provide a name, select a type, and add images.');
            return false;
        }

        try {
            const response = await fetch('/api/reports/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...window.getCsrfHeaders() },
                body: JSON.stringify({ name: nameToUse, imageIds: imageIds, reportTypeId: reportTypeId })
            });

            if (!response.ok) throw new Error((await response.json()).message || 'Failed to create report.');

            const newReport = await response.json();
            window.displayGlobalAlert('success', `Report "${newReport.name}" created successfully!`);

            lastCreatedReportId = newReport.id;
            currentReportName = newReport.name;
            currentReportTypeId = reportTypeId;
            currentReportImageOrder = imageIds;
            lastCreatedReportShareableLink = null;
            clearThumbnailSelections();
            return true;
        } catch (error) {
            console.error('Error creating report:', error);
            window.displayGlobalAlert('error', error.message || 'An error occurred while creating the report.');
            return false;
        }
    }

    async function updateExistingReport() {
        let nameToUse = reportNameInput.value.trim() || getDefaultReportName();
        let changesMade = false;
        const reportTypeId = parseInt(reportTypeSelect.value);
        const imageIds = currentUiImageIds;

        try {
            if (nameToUse !== currentReportName) {
                const res = await fetch(`/api/reports/${lastCreatedReportId}/name`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'text/plain', ...window.getCsrfHeaders() },
                    body: nameToUse
                });
                if (!res.ok) throw new Error(await res.text() || 'Failed to update report name.');
                currentReportName = nameToUse;
                changesMade = true;
            }

            if (reportTypeId !== currentReportTypeId) {
                const res = await fetch(`/api/reports/${lastCreatedReportId}/type`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', ...window.getCsrfHeaders() },
                    body: JSON.stringify(reportTypeId)
                });
                if (!res.ok) throw new Error(await res.text() || 'Failed to update report type.');
                currentReportTypeId = reportTypeId;
                changesMade = true;
            }

            if (!arraysEqual(imageIds, currentReportImageOrder)) {
                const res = await fetch(`/api/reports/${lastCreatedReportId}/images/reorder`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', ...window.getCsrfHeaders() },
                    body: JSON.stringify(imageIds)
                });
                if (!res.ok) throw new Error((await res.json()).message || 'Failed to update image order.');
                currentReportImageOrder = imageIds;
                changesMade = true;
            }

            if (changesMade) {
                window.displayGlobalAlert('success', `Report "${nameToUse}" updated successfully!`);
                lastCreatedReportShareableLink = null;
            } else {
                window.displayGlobalAlert('info', 'No changes detected to save.');
            }
            clearThumbnailSelections();
            return true;
        } catch (error) {
            console.error('Error updating report:', error);
            window.displayGlobalAlert('error', error.message || 'An error occurred while updating the report.');
            return false;
        }
    }

    if (addSelectedImagesBtn) {
        addSelectedImagesBtn.addEventListener('click', () => {
            if (window.selectedImageElements && window.selectedImageElements.size > 0) {
                Array.from(window.selectedImageElements).forEach(imageElement => {
                    const imageId = parseInt(imageElement.dataset.id);
                    const imageUrl = imageElement.querySelector('img').src;
                    renderImageThumbnail(imageId, imageUrl);
                });
                window.clearImageSelections();
            } else {
                window.displayGlobalAlert('info', 'No images selected in the gallery to add to the report.');
            }
        });
    }

    if (removeSelectedImagesBtn) {
        removeSelectedImagesBtn.addEventListener('click', removeSelectedImages);
    }

    if (clearReportImagesBtn) {
        clearReportImagesBtn.addEventListener('click', () => {
            reportImagesContainer.innerHTML = '';
            currentUiImageIds = [];
            clearThumbnailSelections();
            lastCreatedReportId = null;
            lastCreatedReportShareableLink = null;
            if (reportNameInput) reportNameInput.value = '';
            if (reportTypeSelect && reportTypeSelect.options.length > 0) reportTypeSelect.selectedIndex = 0;
            currentReportName = null;
            currentReportTypeId = null;
            currentReportImageOrder = [];
            updateReportGeneratorButtonStates();
        });
    }

    if (saveReportBtn) {
        saveReportBtn.addEventListener('click', async () => {
            if (lastCreatedReportId === null) {
                await createNewReport();
            } else {
                await updateExistingReport();
            }
            updateReportGeneratorButtonStates();
            if (typeof window.fetchAndRenderUserReports === 'function') {
                await window.fetchAndRenderUserReports();
            }
        });
    }

    if (shareReportBtn) {
        shareReportBtn.addEventListener('click', async () => {
            if (hasUnsavedChanges()) {
                const saved = lastCreatedReportId === null ? await createNewReport() : await updateExistingReport();
                if (!saved) {
                    window.displayGlobalAlert('error', 'Could not save changes. Sharing aborted.');
                    return;
                }
            }
    
            let reportId = lastCreatedReportId;
            if (!reportId) {
                window.displayGlobalAlert('info', 'There is nothing to share.');
                return;
            }
    
            if (lastCreatedReportShareableLink) {
                copyToClipboard(lastCreatedReportShareableLink);
                window.displayGlobalAlert('success', 'Shareable link copied to clipboard!');
                return;
            }
    
            try {
                const response = await fetch(`/api/reports/${reportId}/share`, {
                    method: 'POST',
                    headers: { 'Accept': 'text/plain', ...window.getCsrfHeaders() }
                });
                if (!response.ok) throw new Error(await response.text() || 'Failed to generate shareable link.');
                const shareableLink = await response.text();
                lastCreatedReportShareableLink = shareableLink;
                updateReportGeneratorButtonStates();
                copyToClipboard(shareableLink);
                window.displayGlobalAlert('success', 'Shareable link copied to clipboard!');
                if (typeof window.fetchAndRenderUserReports === 'function') {
                    await window.fetchAndRenderUserReports();
                }
            } catch (error) {
                console.error('Error sharing report:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while sharing the report.');
            }
        });
    }

    document.addEventListener('click', (event) => {
        if (!reportGeneratorBar.contains(event.target)) {
            clearThumbnailSelections();
        }
    });

    window.loadReportIntoGenerator = (report) => {
        if (clearReportImagesBtn) clearReportImagesBtn.click();
        if (reportNameInput) reportNameInput.value = report.name;
        if (reportTypeSelect) reportTypeSelect.value = report.reportTypeId;
        
        report.images.forEach(image => renderImageThumbnail(image.id, image.thumbnailUrl));
        
        lastCreatedReportId = report.id;
        currentReportName = report.name;
        currentReportTypeId = report.reportTypeId;
        currentReportImageOrder = report.images.map(img => img.id);

        lastCreatedReportShareableLink = report.shareableLink;
        updateReportGeneratorButtonStates();
    };

    if (reportNameInput) {
        const autocompleteInputWrapper = reportNameInput.closest('.autocomplete-input-wrapper');
        const autocompleteDropdown = autocompleteInputWrapper.querySelector('.autocomplete-dropdown');
        const autocompleteArrow = autocompleteInputWrapper.querySelector('.autocomplete-arrow');
        let currentAutocompleteSelection = -1;

        function updateAutocompleteSelection(newIndex) {
            const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');
            if (items.length === 0) return;
            if (currentAutocompleteSelection > -1) items[currentAutocompleteSelection].classList.remove('selected');
            currentAutocompleteSelection = newIndex;
            items[currentAutocompleteSelection].classList.add('selected');
            items[currentAutocompleteSelection].scrollIntoView({ block: 'nearest' });
        }

        async function selectAutocompleteItem(item) {
            const reportId = item.dataset.reportId;
            if (reportId) {
                try {
                    const response = await fetch(`/api/reports/${reportId}/details`, {
                        headers: { 'Accept': 'application/json', ...window.getCsrfHeaders() }
                    });
                    if (!response.ok) throw new Error((await response.json()).message || 'Failed to fetch report details.');
                    const reportDetails = await response.json();
                    window.loadReportIntoGenerator(reportDetails);
                } catch (error) {
                    console.error('Error loading report:', error);
                    window.displayGlobalAlert('error', 'Failed to load the selected report.');
                }
            }
            reportNameInput.value = item.textContent;
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;
        }

        function showAllOptions() {
            autocompleteDropdown.innerHTML = '';
            currentAutocompleteSelection = -1;

            if (window.allSavedReports.length === 0) {
                const item = document.createElement('div');
                item.className = 'autocomplete-item not-implemented';
                item.textContent = 'No saved reports';
                autocompleteDropdown.appendChild(item);
            } else {
                window.allSavedReports.forEach(report => {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-item';
                    item.textContent = report.name;
                    item.dataset.reportId = report.id;
                    item.addEventListener('mousedown', (event) => {
                        event.preventDefault();
                        selectAutocompleteItem(item);
                    });
                    autocompleteDropdown.appendChild(item);
                });
            }
            autocompleteDropdown.style.display = 'block';
        }

        if (autocompleteArrow) {
            autocompleteArrow.addEventListener('click', (event) => {
                event.stopPropagation();
                if (autocompleteDropdown.style.display === 'block') {
                    autocompleteDropdown.style.display = 'none';
                } else {
                    showAllOptions();
                    reportNameInput.focus();
                }
            });
        }

        reportNameInput.addEventListener('input', () => {
            const searchTerm = reportNameInput.value.toLowerCase();
            autocompleteDropdown.innerHTML = '';
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;

            if (searchTerm.length === 0) return;

            const matchingReports = window.allSavedReports.filter(r => r.name.toLowerCase().includes(searchTerm));

            matchingReports.forEach(report => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.textContent = report.name;
                item.dataset.reportId = report.id;
                item.addEventListener('mousedown', (event) => {
                    event.preventDefault();
                    selectAutocompleteItem(item);
                });
                autocompleteDropdown.appendChild(item);
            });

            if (matchingReports.length > 0) {
                autocompleteDropdown.style.display = 'block';
            }
        });

        reportNameInput.addEventListener('focus', () => {
            if (reportNameInput.value.trim().length > 0) {
                const event = new Event('input');
                reportNameInput.dispatchEvent(event);
            }
        });

        reportNameInput.addEventListener('blur', () => {
            setTimeout(() => {
                autocompleteDropdown.style.display = 'none';
                currentAutocompleteSelection = -1;
            }, 100);
        });

        reportNameInput.addEventListener('keydown', (event) => {
            const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                if (items.length > 0) {
                    const newIndex = (currentAutocompleteSelection + 1) % items.length;
                    updateAutocompleteSelection(newIndex);
                    autocompleteDropdown.style.display = 'block';
                } else {
                    showAllOptions();
                }
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                if (items.length > 0) {
                    const newIndex = (currentAutocompleteSelection - 1 + items.length) % items.length;
                    updateAutocompleteSelection(newIndex);
                    autocompleteDropdown.style.display = 'block';
                }
            } else if (event.key === 'Enter') {
                if (currentAutocompleteSelection > -1 && items.length > 0) {
                    event.preventDefault();
                    selectAutocompleteItem(items[currentAutocompleteSelection]);
                }
            }
        });
    }

    populateReportTypes();
    updateReportGeneratorButtonStates();
});

document.addEventListener('appInitialized', function() {
    const reportList = document.querySelector('#top-bar-reports #report-list');
    window.allSavedReports = [];

    window.fetchAndRenderUserReports = async function() {
        if (!reportList) {
            return;
        }

        if (window.isAnonymous) {
            reportList.innerHTML = '<li class="badge-list-item"><span class="badge">Login to view your reports.</span></li>';
            return;
        }

        try {
            const response = await fetch('/api/reports');
            if (!response.ok) {
                console.error('Failed to fetch user reports:', response.status, response.statusText);
                throw new Error('Failed to fetch user reports.');
            }
            const userReports = await response.json();
            
            window.allSavedReports = userReports.map(r => ({ id: r.id, name: r.name }));

            const currentlySelectedReportIds = new Set(Array.from(window.selectedReportElements).map(el => el.dataset.reportId));
            const lastSelectedReportIdValue = window.lastSelectedReportId;

            reportList.innerHTML = '';
            window.selectedReportElements.clear();
            window.lastSelectedReportId = null;

            if (userReports.length === 0) {
                reportList.innerHTML = '<li class="badge-list-item"><span class="badge">no reports</span></li>';
                return;
            }

            userReports.forEach(report => {
                const listItem = document.createElement('li');
                listItem.className = 'badge-list-item';

                const reportBadge = document.createElement('span');
                reportBadge.className = 'badge';
                reportBadge.dataset.reportId = report.id;
                reportBadge.draggable = true;

                const nameSpan = document.createElement('span');
                nameSpan.className = 'badge-name';
                nameSpan.textContent = report.name;

                if (report.isShared) {
                    const shareButton = document.createElement('button');
                    shareButton.className = 'badge-button';
                    shareButton.textContent = 'share';
                    shareButton.title = 'Copy shareable link';
                    shareButton.dataset.shareableLink = report.shareableLink;

                    shareButton.addEventListener('click', (event) => {
                        event.stopPropagation();
                        copyToClipboard(report.shareableLink);
                        window.displayGlobalAlert('success', `Shareable link for "${report.name}" copied to clipboard!`);
                    });
                    reportBadge.appendChild(shareButton);
                }
                reportBadge.appendChild(nameSpan);

                reportBadge.addEventListener('click', window.handleReportClick);
                reportBadge.addEventListener('dblclick', window.handleReportDoubleClick);

                reportBadge.addEventListener('dragstart', (event) => {
                    event.dataTransfer.setData('application/x-tagfolio-report-id', report.id);
                    event.dataTransfer.effectAllowed = 'copy';
                });

                listItem.appendChild(reportBadge);
                reportList.appendChild(listItem);

                if (currentlySelectedReportIds.has(report.id.toString())) {
                    reportBadge.classList.add('selected');
                    window.selectedReportElements.add(reportBadge);
                    if (lastSelectedReportIdValue === report.id.toString()) {
                        window.lastSelectedReportId = reportBadge.dataset.reportId;
                    }
                }
            });

        } catch (error) {
            console.error('Error fetching user reports:', error);
            window.displayGlobalAlert('error', error.message || 'An error occurred while loading user reports.');
        } finally {
            document.dispatchEvent(new CustomEvent('reportSelectionChanged', { detail: { selectedCount: window.selectedReportElements.size } }));
        }
    }

    window.fetchAndRenderUserReports();

    document.addEventListener('reportsDeleted', window.fetchAndRenderUserReports);
});
