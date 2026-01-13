/**
 * @file This file handles the filtering functionality of the application.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

window.filterExpression = [];
window.lastSuccessfullyAppliedFilterExpression = [];
window.FILTER_STORAGE_KEY = 'tagfolioFilterExpression';

/**
 * Deletes the selected filters.
 */
window.handleDeleteFilters = async function() {
    const filterIdsToDelete = Array.from(window.selectedSavedFilterElements).map(el => el.dataset.filterId);
    if (filterIdsToDelete.length === 0) {
        window.displayGlobalAlert('info', 'No filters selected for deletion.');
        return;
    }

    window.displayConfirmationAlert({
        message: `Are you sure you want to delete ${filterIdsToDelete.length} selected filter(s)?`,
        onConfirm: async () => {
            try {
                const response = await fetch('/api/filters/delete', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getCsrfHeaders()
                    },
                    body: JSON.stringify(filterIdsToDelete)
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to delete filters.');
                }
                window.displayGlobalAlert('success', `${filterIdsToDelete.length} filter(s) deleted successfully.`);
                await window.fetchAndRenderSavedFilters();
                document.dispatchEvent(new CustomEvent('filtersDeleted'));
            } catch (error) {
                console.error('Error deleting filters:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while deleting filters.');
            }
        }
    });
};

/**
 * Consolidates a filter expression for backend processing.
 * @param {Array<Object>} expression - The filter expression.
 * @returns {Array<Object>} The consolidated expression.
 */
function consolidateExpression(expression) {
    let tempExpression = JSON.parse(JSON.stringify(expression));

    for (let i = tempExpression.length - 2; i >= 0; i--) {
        const first = tempExpression[i];
        const second = tempExpression[i+1];

        if (first.type === 'comparator' && second.type === 'comparator') {
            const combinedValue = first.value + second.value;
            if (['>=', '<='].includes(combinedValue)) {
                const newComparator = { type: 'comparator', value: combinedValue };
                tempExpression.splice(i, 2, newComparator);
            }
        }
    }

    for (let i = tempExpression.length - 3; i >= 0; i--) {
        const first = tempExpression[i];
        const second = tempExpression[i+1];
        const third = tempExpression[i+2];

        if (first.type === 'comparator-field' && second.type === 'comparator' && third.type === 'value') {
            const newField = {
                type: 'field',
                field: first.value,
                op: second.value,
                rawValue: third.value,
                value: `${first.value}${second.value}${third.value}`
            };
            tempExpression.splice(i, 3, newField);
        }
    }
    return tempExpression;
}


/**
 * Generates a human-readable string from a filter expression.
 * @param {Array<Object>} expression - The filter expression.
 * @returns {string} The human-readable string.
 */
window.generateFilterExpressionString = function(expression) {
    const stringParts = expression.map(item => {
        if (item.type === 'tag') {
            let tagValue = item.value;
            tagValue = tagValue
                .replace(/ñ/g, 'n')
                .replace(/Ñ/g, 'N')
                .replace(/[áÁàÀäÄâÂ]/g, 'a')
                .replace(/[éÉèÈëËêÊ]/g, 'e')
                .replace(/[íÍìÌïÏîÎ]/g, 'i')
                .replace(/[óÓòÒöÖôÔ]/g, 'o')
                .replace(/[úÚùÙüÜûÛ]/g, 'u')
                .replace(/ç/g, 'c')
                .replace(/Ç/g, 'C');
            return tagValue.replace(/[^a-zA-Z0-9_]+/g, '_').toLowerCase().replace(/^_|_$/g, '');
        } else if (item.type === 'operator') {
            return item.value.toUpperCase();
        } else if (item.type === 'parenthesis') {
            return item.value;
        } else if (item.type === 'field') {
            return item.value;
        } else if (item.type === 'comparator') {
            return item.value;
        } else if (item.type === 'comparator-field') {
            return item.value;
        } else if (item.type === 'value') {
            return item.value;
        }
        return '';
    }).filter(part => part !== '');

    let result = stringParts.join('_');

    result = result.replace(/(AND|OR|NOT)/g, '_$1_');

    result = result.replace(/_\(_/g, '(');
    result = result.replace(/\)_/g, ')');
    result = result.replace(/_\)/g, ')');

    result = result.replace(/__+/g, '_');

    result = result.replace(/^_|_$/g, '');

    return result.trim();
};

/**
 * Updates the state of the export and share filter menu items.
 */
function updateExportFilterMenuState() {
    const exportFiltersBtn = document.getElementById('export-filters-btn');
    const shareFiltersBtn = document.getElementById('share-filters-btn');
    const shareFilterBarBtn = document.getElementById('share-filter-bar-btn');

    const filtersSelected = window.selectedSavedFilterElements.size > 0;
    const singleFilterSelected = window.selectedSavedFilterElements.size === 1;

    if (exportFiltersBtn) {
        exportFiltersBtn.classList.toggle('disabled', !filtersSelected);
    }
    if (shareFiltersBtn) {
        shareFiltersBtn.classList.toggle('disabled', !singleFilterSelected);
    }
    if (shareFilterBarBtn) {
        shareFilterBarBtn.classList.toggle('disabled', !filtersSelected);
    }
}

/**
 * Handles the drag start event for a saved filter.
 * @param {DragEvent} event - The drag event.
 */
function handleSavedFilterDragStart(event) {
    const draggedFilterName = event.target.dataset.filterName;
    const draggedFilterExpression = event.target.dataset.filterExpression;

    const selectedFilterNames = Array.from(window.selectedSavedFilterElements).map(el => el.dataset.filterName);
    const selectedFilterExpressions = Array.from(window.selectedSavedFilterElements).map(el => el.dataset.filterExpression);

    if (window.selectedSavedFilterElements.size > 1 && selectedFilterNames.includes(draggedFilterName)) {
        event.dataTransfer.setData('application/x-saved-filter-expressions', JSON.stringify(selectedFilterExpressions));
        event.dataTransfer.effectAllowed = 'copy';
    } else {
        event.dataTransfer.setData('text/plain', draggedFilterName);
        event.dataTransfer.setData('application/x-saved-filter-name', draggedFilterName);
        event.dataTransfer.setData('application/x-saved-filter-expression', draggedFilterExpression);
        event.dataTransfer.effectAllowed = 'copy';
    }
}

/**
 * Validates a filter expression.
 * @param {Array<Object>} expression - The filter expression.
 * @returns {boolean} True if the expression is valid, false otherwise.
 */
window.isValidFilterExpression = function(expression) {
    if (expression.length === 0) {
        return true;
    }
    const lastItem = expression[expression.length - 1];
    const invalidEndingTypes = ['operator', 'comparator', 'comparator-field'];
    if (invalidEndingTypes.includes(lastItem.type)) {
        return false;
    }
    return !(lastItem.type === 'parenthesis' && lastItem.value === '(');

};

/**
 * Checks if an item is a complete operand.
 * @param {Object} item - The item to check.
 * @returns {boolean} True if the item is a complete operand, false otherwise.
 */
window.isCompleteOperand = function(item) {
    if (!item) return false;
    return item.type === 'tag' || item.type === 'field' || (item.type === 'parenthesis' && item.value === ')');
};

/**
 * Renders the filter expression UI.
 */
window.renderFilterExpression = function() {
    const filterExpressionContainer = document.getElementById("filter-expression-container");
    const autocompleteInputWrapper = document.querySelector('#filter-expression-container .autocomplete-input-wrapper');
    const saveFilterBtn = document.getElementById("save-filter-btn");
    const shareFilterBarBtn = document.getElementById("share-filter-bar-btn");

    if (!filterExpressionContainer) return;

    filterExpressionContainer.innerHTML = '';

    window.filterExpression.forEach((item, index) => {
        const badge = document.createElement('span');
        badge.classList.add('filter-badge', item.type);
        badge.textContent = item.type === 'comparator-field' ? item.value.toUpperCase() : item.value;
        badge.dataset.index = index;
        badge.addEventListener('click', (event) => {
            event.stopPropagation();
            window.filterExpression.splice(index, 1);
            window.renderFilterExpression();
            window.processFilterExpressionChange();
        });
        filterExpressionContainer.appendChild(badge);
    });

    if (autocompleteInputWrapper) {
        filterExpressionContainer.appendChild(autocompleteInputWrapper);
    }

    const isDisabled = window.filterExpression.length === 0;
    if (saveFilterBtn) saveFilterBtn.disabled = isDisabled;
    if (shareFilterBarBtn) shareFilterBarBtn.disabled = isDisabled;
};

/**
 * Processes a change in the filter expression.
 */
window.processFilterExpressionChange = async function() {
    
    if (window.isValidFilterExpression(window.filterExpression)) {
        window.lastSuccessfullyAppliedFilterExpression = [...window.filterExpression];
        localStorage.setItem(window.FILTER_STORAGE_KEY, JSON.stringify(window.filterExpression));
        try {
            
            await window.filterImages(window.filterExpression);
        } catch (e) {}
        try {
            if (typeof requestAnimationFrame === 'function') {
                requestAnimationFrame(() => {
                    document.dispatchEvent(new CustomEvent('filterApplied'));
                    if (window.renderFilterExpression) window.renderFilterExpression();
                });
            } else {
                setTimeout(() => {
                    document.dispatchEvent(new CustomEvent('filterApplied'));
                    if (window.renderFilterExpression) window.renderFilterExpression();
                }, 0);
            }
        } catch (e) {
        }
    } else {
        window.filterImages(window.lastSuccessfullyAppliedFilterExpression);
        window.displayGlobalAlert('warning', 'Invalid filter expression. Applying last valid filter.');
        if (window.renderFilterExpression) window.renderFilterExpression();
    }
};

/**
 * Gets the drop index for a drag-and-drop operation.
 * @param {number} clientX - The clientX coordinate of the drop event.
 * @returns {number} The drop index.
 */
window.getDropIndex = function(clientX) {
    const filterExpressionContainer = document.getElementById("filter-expression-container");
    if (!filterExpressionContainer) return window.filterExpression.length;

    const badges = Array.from(filterExpressionContainer.querySelectorAll('.filter-badge'));
    for (let i = 0; i < badges.length; i++) {
        const badgeRect = badges[i].getBoundingClientRect();
        if (clientX < badgeRect.left + badgeRect.width / 2) {
            return i;
        }
    }
    return window.filterExpression.length;
};

/**
 * Adds a tag to the filter expression.
 * @param {string} tagName - The name of the tag to add.
 */
window.addTagToFilter = function(tagName) {
    if (window.filterExpression.length > 0 && window.isCompleteOperand(window.filterExpression[window.filterExpression.length - 1])) {
        window.filterExpression.push({ type: 'operator', value: 'AND' });
    }
    window.filterExpression.push({ type: 'tag', value: tagName });
    window.processFilterExpressionChange();
};

/**
 * Adds an operator to the filter expression.
 * @param {string} op - The operator to add.
 */
window.addOperatorToFilter = function(op) {
    window.filterExpression.push({ type: 'operator', value: op });
    window.processFilterExpressionChange();
};

/**
 * Adds a parenthesis to the filter expression.
 * @param {string} p - The parenthesis to add.
 */
window.addParenthesisToFilter = function(p) {
    window.filterExpression.push({ type: 'parenthesis', value: p });
    window.processFilterExpressionChange();
};

/**
 * Adds a comparator field to the filter expression.
 * @param {string} field - The comparator field to add.
 */
window.addComparatorFieldToFilter = function(field) {
    if (window.filterExpression.length > 0 && window.isCompleteOperand(window.filterExpression[window.filterExpression.length - 1])) {
        window.filterExpression.push({ type: 'operator', value: 'AND' });
    }
    window.filterExpression.push({ type: 'comparator-field', value: field });
    window.processFilterExpressionChange();
};

/**
 * Adds a comparator to the filter expression.
 * @param {string} symbol - The comparator symbol to add.
 */
window.addComparatorToFilter = function(symbol) {
    const lastItem = window.filterExpression.length > 0 ? window.filterExpression[window.filterExpression.length - 1] : null;
    if (lastItem && lastItem.type === 'comparator' && (lastItem.value === '>' || lastItem.value === '<') && symbol === '=') {
        lastItem.value += symbol;
    } else {
        if (lastItem && window.isCompleteOperand(lastItem)) {
            window.filterExpression.push({ type: 'operator', value: 'AND' });
        }
        window.filterExpression.push({ type: 'comparator', value: symbol });
    }
    window.processFilterExpressionChange();
};

/**
 * Clears the filter expression.
 */
window.clearFilter = function() {
    
    window.filterExpression = [];
    window.lastSuccessfullyAppliedFilterExpression = [];
    localStorage.removeItem(window.FILTER_STORAGE_KEY);
    const newFilterInput = document.getElementById('new-filter-input');
    if(newFilterInput) newFilterInput.value = '';
    const filterNameInput = document.getElementById('filter-name-input');
    if(filterNameInput) filterNameInput.value = '';
    
    window.processFilterExpressionChange();
};

/**
 * Parses a string into a filter expression.
 * @param {string} expressionString - The string to parse.
 * @returns {Array<Object>} The parsed filter expression.
 */
window.parseExpressionString = function(expressionString) {
    const parsed = [];
    const tokens = expressionString.match(/(?:AND|OR|NOT|\(|\)|"[^"]+"|'[^']+'|\S+)/gi);

    if (!tokens) return [];

    const allowedFields = ['rating', 'createdAt', 'updatedAt', 'fileModifiedAt', 'originalFileName'];

    tokens.forEach(token => {
        const upperToken = token.toUpperCase();
        if (upperToken === 'AND' || upperToken === 'OR' || upperToken === 'NOT') {
            parsed.push({ type: 'operator', value: upperToken });
        } else if (token === '(' || token === ')') {
            parsed.push({ type: 'parenthesis', value: token });
        } else {
            const m = token.match(/^([a-zA-Z][a-zA-Z0-9_]*)\s*([<>=])\s*(.+)$/);
            if (m) {
                const fieldRaw = m[1];
                const op = m[2];
                let rhs = m[3];
                rhs = rhs.replace(/^["']|["']$/g, '');
                const fieldCanonical = allowedFields.find(f => f.toLowerCase() === fieldRaw.toLowerCase());
                if (fieldCanonical) {
                    parsed.push({ type: 'field', field: fieldCanonical, op: op, rawValue: rhs, value: `${fieldCanonical}${op}${rhs}` });
                    return;
                }
            }
            const tagValue = token.replace(/^["']|["']$/g, '');
            parsed.push({ type: 'tag', value: tagValue });
        }
    });
    return parsed;
};


/**
 * Initializes the drag and drop functionality for the filter expression container.
 */
window.initFilterExpressionContainerDragDrop = function() {
    const filterExpressionContainer = document.getElementById("filter-expression-container");
    if (!filterExpressionContainer) return;

    filterExpressionContainer.addEventListener('dragover', (event) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
    });

    filterExpressionContainer.addEventListener('drop', (event) => {
        event.preventDefault();

        let tagsToProcess = [];
        let expressionsToProcess = [];
        let operatorToProcess = null;
        let parenthesisToProcess = null;
        let comparatorToProcess = null;
        let comparatorFieldToProcess = null;

        const dropIndex = window.getDropIndex(event.clientX);

        if (event.dataTransfer.types.includes('application/x-saved-filter-expressions')) {
            const jsonExpressions = event.dataTransfer.getData('application/x-saved-filter-expressions');
            expressionsToProcess = JSON.parse(jsonExpressions).map(exprStr => JSON.parse(exprStr));
        } else if (event.dataTransfer.types.includes('application/x-saved-filter-expression')) {
            const filterExpressionJson = event.dataTransfer.getData('application/x-saved-filter-expression');
            if (filterExpressionJson) {
                expressionsToProcess.push(JSON.parse(filterExpressionJson));
            }
        } else if (event.dataTransfer.types.includes('application/x-tag-names')) {
            tagsToProcess = JSON.parse(event.dataTransfer.getData('application/x-tag-names'));
        } else if (event.dataTransfer.types.includes('application/x-filter-control')) {
            const data = event.dataTransfer.getData('application/x-filter-control');

            const comparatorFieldNames = Array.from(document.querySelectorAll(".comparator-field-btn")).map(btn => btn.dataset.field);
            if (comparatorFieldNames.includes(data)) {
                comparatorFieldToProcess = data;
            } else if (['AND', 'OR', 'NOT'].includes(data)) {
                operatorToProcess = data;
            } else if (['(', ')'].includes(data)) {
                parenthesisToProcess = data;
            } else if (['<', '>', '=', '<=', '>='].includes(data)) {
                comparatorToProcess = data;
            } else {
                tagsToProcess.push(data);
            }
        } else if (event.dataTransfer.types.includes('text/plain')) {
            const data = event.dataTransfer.getData('text/plain');
            tagsToProcess.push(data);
        }


        if (tagsToProcess.length > 0) {
            let currentIndex = dropIndex;
            if (currentIndex > 0 && window.isCompleteOperand(window.filterExpression[currentIndex - 1])) {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'AND' });
                currentIndex++;
            }
            tagsToProcess.forEach((tagName, index) => {
                if (index > 0) {
                    window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'AND' });
                    currentIndex++;
                }
                const m = (tagName || '').match(/^([a-zA-Z][a-zA-Z0-9_]*)\s*([<>=])\s*(.+)$/);
                if (m) {
                    const allowedFields = ['rating','createdAt','updatedAt','fileModifiedAt','originalFileName'];
                    const fieldCanonical = allowedFields.find(f => f.toLowerCase() === m[1].toLowerCase());
                    if (fieldCanonical) {
                        const op = m[2];
                        const rhs = (m[3] || '').replace(/^["']|["']$/g, '');
                        window.filterExpression.splice(currentIndex, 0, { type: 'field', field: fieldCanonical, op: op, rawValue: rhs, value: `${fieldCanonical}${op}${rhs}` });
                        currentIndex++;
                        return;
                    }
                }
                window.filterExpression.splice(currentIndex, 0, { type: 'tag', value: tagName });
                currentIndex++;
            });
            if (currentIndex < window.filterExpression.length && window.isCompleteOperand(window.filterExpression[currentIndex])) {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'AND' });
            }
        }

        if (expressionsToProcess.length > 0) {
            let currentIndex = dropIndex;
            if (currentIndex > 0 && window.isCompleteOperand(window.filterExpression[currentIndex - 1])) {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'OR' });
                currentIndex++;
            }
            expressionsToProcess.forEach((droppedExpression, index) => {
                if (index > 0) {
                    window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'OR' });
                    currentIndex++;
                }
                window.filterExpression.splice(currentIndex, 0, { type: 'parenthesis', value: '(' });
                currentIndex++;
                window.filterExpression.splice(currentIndex, 0, ...droppedExpression);
                currentIndex += droppedExpression.length;
                window.filterExpression.splice(currentIndex, 0, { type: 'parenthesis', value: ')' });
                currentIndex++;
            });
            if (currentIndex < window.filterExpression.length && window.isCompleteOperand(window.filterExpression[currentIndex])) {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'OR' });
            }
        }

        if (operatorToProcess) {
            window.filterExpression.splice(dropIndex, 0, { type: 'operator', value: operatorToProcess });
        }

        if (parenthesisToProcess) {
            window.filterExpression.splice(dropIndex, 0, { type: 'parenthesis', value: parenthesisToProcess });
        }

        if (comparatorFieldToProcess) {
            let currentIndex = dropIndex;
            if (currentIndex > 0 && window.isCompleteOperand(window.filterExpression[currentIndex - 1])) {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'AND' });
                currentIndex++;
            }
            window.filterExpression.splice(currentIndex, 0, { type: 'comparator-field', value: comparatorFieldToProcess });
        }

        if (comparatorToProcess) {
            let currentIndex = dropIndex;
            const lastItem = window.filterExpression.length > 0 ? window.filterExpression[window.filterExpression.length - 1] : null;
            if (currentIndex > 0 && window.isCompleteOperand(lastItem) && lastItem.type !== 'comparator-field') {
                window.filterExpression.splice(currentIndex, 0, { type: 'operator', value: 'AND' });
                currentIndex++;
            }
            window.filterExpression.splice(currentIndex, 0, { type: 'comparator', value: comparatorToProcess });
        }
        
        window.processFilterExpressionChange();
    });
};

/**
 * Initializes the drag start listeners for the filter control buttons.
 */
function initFilterControlDragstartListeners() {
    const operatorBtns = document.querySelectorAll(".operator-btn");
    const parenthesisBtns = document.querySelectorAll(".parenthesis-btn");
    const comparatorBtns = document.querySelectorAll(".comparator-btn");
    const comparatorFieldBtns = document.querySelectorAll(".comparator-field-btn");

    operatorBtns.forEach(btn => {
        btn.addEventListener("click", () => window.addOperatorToFilter(btn.dataset.op));
        btn.addEventListener('dragstart', (event) => {
            event.dataTransfer.setData('application/x-filter-control', btn.dataset.op);
            event.dataTransfer.effectAllowed = 'copy';
        });
    });

    parenthesisBtns.forEach(btn => {
        btn.addEventListener("click", () => window.addParenthesisToFilter(btn.dataset.p));
        btn.addEventListener('dragstart', (event) => {
            event.dataTransfer.setData('application/x-filter-control', btn.dataset.p);
            event.dataTransfer.effectAllowed = 'copy';
        });
    });

    comparatorBtns.forEach(btn => {
        btn.addEventListener("click", () => window.addComparatorToFilter(btn.dataset.comp));
        btn.addEventListener('dragstart', (event) => {
            event.dataTransfer.setData('application/x-filter-control', btn.dataset.comp);
            event.dataTransfer.effectAllowed = 'copy';
        });
    });

    comparatorFieldBtns.forEach(btn => {
        btn.addEventListener("click", () => window.addComparatorFieldToFilter(btn.dataset.field));
        btn.addEventListener('dragstart', (event) => {
            event.dataTransfer.setData('application/x-filter-control', btn.dataset.field);
            event.dataTransfer.effectAllowed = 'copy';
        });
    });
}


/**
 * Copies text to the clipboard.
 * @param {string} text - The text to copy.
 */
function copyToClipboard(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).catch(err => {
            console.error('Failed to copy text: ', err);
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
 * Handles a click on the share filter button.
 * @param {number} filterId - The ID of the filter.
 * @param {string} filterName - The name of the filter.
 * @param {boolean} [skipPrompt=false] - Whether to skip the prompt and copy directly to the clipboard.
 */
window.handleShareFilterClick = async function(filterId, filterName, skipPrompt = false) {
    try {
        const response = await fetch(`/api/filters/${filterId}/share`, {
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

        if (skipPrompt) {
            copyToClipboard(shareableUrl);
            window.displayGlobalAlert('success', `Shareable link for "${filterName}" copied to clipboard!`);
        } else {
            window.prompt(`Shareable link for "${filterName}":`, shareableUrl);
            window.displayGlobalAlert('success', `Shareable link generated for "${filterName}".`);
        }

        await window.fetchAndRenderSavedFilters();

    } catch (error) {
        console.error('Error generating shareable link:', error);
        window.displayGlobalAlert('error', error.message || 'An error occurred during the sharing process.');
    }
};

/**
 * Saves and shares a filter.
 * @param {string} filterName - The name of the filter.
 * @param {Array<Object>} expression - The filter expression.
 */
window.saveAndShareFilter = async function(filterName, expression) {
    if (expression.length === 0) {
        window.displayGlobalAlert('warning', 'Filter expression is empty. Cannot export.');
        return;
    }
    try {
        const saveResponse = await fetch('/api/filters/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                ...window.getCsrfHeaders()
            },
            body: JSON.stringify({
                name: filterName.trim(),
                expression: JSON.stringify(window.filterExpression)
            })
        });

        if (!saveResponse.ok) {
            const errorData = await saveResponse.json();
            throw new Error(errorData.message || 'Failed to save filter for sharing.');
        }

        const savedFilter = await response.json();

        if (!savedFilter || !savedFilter.id) {
            throw new Error('Could not get the newly saved filter from the server to share it.');
        }

        await window.handleShareFilterClick(savedFilter.id, savedFilter.name, true);

    } catch (error) {
        console.error('Error saving and sharing filter:', error);
        window.displayGlobalAlert('error', error.message || 'An error occurred during the sharing process.');
    }
};

/**
 * Removes accents and special characters from a string.
 * @param {string} str - The string to process.
 * @returns {string} The processed string.
 */
function removeAccentsAndSpecialChars(str) {
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
              .replace(/ñ/g, "n").replace(/Ñ/g, "N")
              .replace(/[^a-zA-Z0-9_]+/g, "_");
}

/**
 * Generates a default filter name from a filter expression.
 * @returns {string} The generated filter name.
 */
function generateDefaultFilterNameFromExpression() {
    if (!window.filterExpression || window.filterExpression.length === 0) {
        const now = new Date();
        const year = now.getFullYear();
        const month = (now.getMonth() + 1).toString().padStart(2, '0');
        const day = now.getDate().toString().padStart(2, '0');
        const hours = now.getHours().toString().padStart(2, '0');
        const minutes = now.getMinutes().toString().padStart(2, '0');
        return `Filter-${year}${month}${day}-${hours}${minutes}`;
    }

    const nameParts = window.filterExpression.map(item => {
        const type = item.type;
        let value = item.value;
        if (type === "tag") {
            value = removeAccentsAndSpecialChars(value);
            return value.toLowerCase();
        } else if (type === "operator") {
            return `_${value.toUpperCase()}_`;
        } else if (type === "parenthesis") {
            return value;
        } else if (type === "field") {
            value = removeAccentsAndSpecialChars(value);
            return `_${value.replace(':', '_').toLowerCase()}_`;
        } else if (type === "comparator-field") {
            value = removeAccentsAndSpecialChars(value);
            return `_${value.toUpperCase()}_`;
        } else if (type === "comparator") {
            return `_${value}_`;
        } else if (type === "value") {
            value = removeAccentsAndSpecialChars(value);
            return `${value}`;
        }
        return "";
    });

    return nameParts.join("")
        .trim()
        .replace(/__+/g, "_")
        .replace(/^_|_$/g, '');
}

/**
 * Loads a saved filter into the filter bar.
 * @param {string} name - The name of the filter.
 * @param {string} expressionJson - The JSON string of the filter expression.
 */
window.loadSavedFilter = function(name, expressionJson) {
    try {
        window.filterExpression = JSON.parse(expressionJson);
        window.processFilterExpressionChange();
        window.displayGlobalAlert('info', `Loaded filter: "${name}"`);
        const filterNameInput = document.getElementById('filter-name-input');
        if (filterNameInput) {
            filterNameInput.value = name;
        }
    } catch (error) {
        console.error('Error loading saved filter:', error);
        window.displayGlobalAlert('error', `Failed to load filter "${name}". Invalid expression format.`);
    }
};

/**
 * Fetches and renders the saved filters.
 */
window.fetchAndRenderSavedFilters = async function() {
    const filterList = document.querySelector("#top-bar-filters #filter-list");
    if (!filterList) return;

    if (isAnonymous) {
        filterList.innerHTML = '<li><span class="badge-list-item"><span class="badge">Login to save and view filters.</span></span></li>';
        return;
    }

    try {
        const response = await fetch('/api/filters');
        if (!response.ok) throw new Error('Failed to fetch saved filters.');
        const savedFilters = await response.json();
        
        window.allSavedFilters = savedFilters.map(f => ({ name: f.name, expression: f.expression }));

        const currentlySelectedFilterIds = new Set(Array.from(window.selectedSavedFilterElements).map(el => el.dataset.filterId));
        const lastSelectedFilterId = window.lastSelectedFilterId;

        filterList.innerHTML = '';
        window.selectedSavedFilterElements.clear();
        window.lastSelectedFilterId = null;

        if (savedFilters.length === 0) {
            filterList.innerHTML = '<li class="badge-list-item"><span class="badge">no filters</span></li>';
            return;
        }

        savedFilters.forEach(filter => {
            const listItem = document.createElement('li');
            listItem.className = 'badge-list-item';

            const badge = document.createElement('span');
            badge.className = 'badge';
            badge.dataset.filterId = filter.id;
            badge.dataset.filterName = filter.name;
            badge.dataset.filterExpression = filter.expression;
            badge.setAttribute('draggable', 'true');

            const nameSpan = document.createElement('span');
            nameSpan.className = 'badge-name';
            nameSpan.textContent = filter.name;

            if (filter.isShared) {
                const shareButton = document.createElement('button');
                shareButton.className = 'badge-button';
                shareButton.textContent = 'share';
                shareButton.title = 'Copy shareable link';
                shareButton.dataset.shareableLink = filter.shareableLink;

                shareButton.addEventListener('click', (event) => {
                    event.stopPropagation();
                    copyToClipboard(filter.shareableLink);
                    window.displayGlobalAlert('success', `Shareable link for "${filter.name}" copied to clipboard!`);
                });
                badge.appendChild(shareButton);
            }

            badge.appendChild(nameSpan);

            badge.addEventListener('click', window.handleFilterClick);
            
            badge.addEventListener('dblclick', () => {
                window.loadSavedFilter(filter.name, filter.expression);
            });

            badge.addEventListener('dragstart', handleSavedFilterDragStart);

            listItem.appendChild(badge);
            filterList.appendChild(listItem);

            if (currentlySelectedFilterIds.has(filter.id.toString())) {
                badge.classList.add('selected');
                window.selectedSavedFilterElements.add(badge);
                if (lastSelectedFilterId === filter.id.toString()) {
                    window.lastSelectedFilterId = badge.dataset.filterId;
                }
            }
        });
    } catch (error) {
        console.error('Error fetching saved filters:', error);
        window.displayGlobalAlert('error', error.message || 'An error occurred while loading saved filters.');
    }
    document.dispatchEvent(new CustomEvent('filterSelectionChanged', { detail: { selectedCount: window.selectedSavedFilterElements.size } }));
    updateExportFilterMenuState();
};


document.addEventListener('DOMContentLoaded', function() {
    const filterExpressionContainer = document.getElementById("filter-expression-container");
    const newFilterInput = document.getElementById('new-filter-input');
    const filterList = document.querySelector("#top-bar-filters #filter-list");
    const filterNameInput = document.getElementById('filter-name-input');
    const clearFilterBtn = document.getElementById("clear-filter-btn");
    const saveFilterBtn = document.getElementById("save-filter-btn");
    const shareFilterBarBtn = document.getElementById("share-filter-bar-btn");
    const addSelectedTagsBtn = document.getElementById("add-selected-tags-btn");


    window.initFilterExpressionContainerDragDrop();

    initFilterControlDragstartListeners();

    window.fetchAllTags = async function() {
        try {
            const tagsFetchBody = (isAnonymous && window.sharedImageIds && window.sharedImageIds.length > 0) ? JSON.stringify({ imageIds: [], baseImageIds: window.sharedImageIds }) : JSON.stringify({ imageIds: [] });

            const response = await fetch('/images/tags', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: tagsFetchBody
            });
            if (!response.ok) throw new Error('Failed to fetch all tags.');
            const data = await response.json();
            allAvailableTags = data.map(tag => tag.name);
        } catch (error) {
            console.error('Error fetching all tags:', error);
        }
    };

    if (newFilterInput) {
        window.fetchAllTags();

        const autocompleteInputWrapper = newFilterInput.closest('.autocomplete-input-wrapper');
        const autocompleteDropdown = autocompleteInputWrapper.querySelector('.autocomplete-dropdown');

        let currentAutocompleteSelection = -1;

        function updateAutocompleteSelection(newIndex) {
            const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');
            if (items.length === 0) return;

            if (currentAutocompleteSelection > -1) {
                items[currentAutocompleteSelection].classList.remove('selected');
            }

            currentAutocompleteSelection = newIndex;
            items[currentAutocompleteSelection].classList.add('selected');

            items[currentAutocompleteSelection].scrollIntoView({ block: 'nearest' });
        }

        function selectAutocompleteItem(item) {
            window.addTagToFilter(item.textContent);
            newFilterInput.value = '';
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;
        }

        newFilterInput.addEventListener('input', () => {
            const searchTerm = newFilterInput.value.toLowerCase();
            autocompleteDropdown.innerHTML = '';
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;

            if (searchTerm.length === 0) return;

            const matchingTags = allAvailableTags.filter(tag => tag.toLowerCase().includes(searchTerm));

            matchingTags.forEach(tag => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.textContent = tag;
                item.addEventListener('mousedown', (event) => {
                    event.preventDefault();
                    selectAutocompleteItem(item);
                });
                autocompleteDropdown.appendChild(item);
            });

            if (matchingTags.length > 0) {
                autocompleteDropdown.style.display = 'block';
            }
        });

        newFilterInput.addEventListener('focus', () => {
            const event = new Event('input');
            newFilterInput.dispatchEvent(event);
        });

        newFilterInput.addEventListener('blur', () => {
            setTimeout(() => {
                autocompleteDropdown.style.display = 'none';
                currentAutocompleteSelection = -1;
            }, 100);
        });

        newFilterInput.addEventListener('keydown', (event) => {
            const items = autocompleteDropdown.querySelectorAll('.autocomplete-item');

            if (event.key === 'ArrowDown') {
                event.preventDefault();
                if (items.length > 0) {
                    const newIndex = (currentAutocompleteSelection + 1) % items.length;
                    updateAutocompleteSelection(newIndex);
                    autocompleteDropdown.style.display = 'block';
                }
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                if (items.length > 0) {
                    const newIndex = (currentAutocompleteSelection - 1 + items.length) % items.length;
                    updateAutocompleteSelection(newIndex);
                    autocompleteDropdown.style.display = 'block';
                }
            } else if (event.key === 'Enter') {
                event.preventDefault();
                if (currentAutocompleteSelection > -1 && items.length > 0) {
                    selectAutocompleteItem(items[currentAutocompleteSelection]);
                } else {
                    const typed = newFilterInput.value.trim();
                    if (typed.length > 0) {
                        const lastItem = window.filterExpression.length > 0 ? window.filterExpression[window.filterExpression.length - 1] : null;
                        if (lastItem && lastItem.type === 'comparator') {
                            window.filterExpression.push({ type: 'value', value: typed });
                        } else {
                            const tokens = window.parseExpressionString(typed);
                            if (tokens.length > 0) {
                                const first = tokens[0];
                                if (window.filterExpression.length > 0 && window.isCompleteOperand(first)) {
                                    const prev = window.filterExpression[window.filterExpression.length - 1];
                                    if (window.isCompleteOperand(prev)) {
                                        window.filterExpression.push({ type: 'operator', value: 'AND' });
                                    }
                                }
                                window.filterExpression.push(...tokens);
                            } else {
                                window.displayGlobalAlert('warning', 'Invalid filter token.');
                            }
                        }
                        window.processFilterExpressionChange();
                        newFilterInput.value = '';
                        autocompleteDropdown.style.display = 'none';
                    } else {
                        window.displayGlobalAlert('warning', 'Please enter a value.');
                    }
                }
            }
        });
    }

    if (filterNameInput) {
        const autocompleteInputWrapper = filterNameInput.closest('.autocomplete-input-wrapper');
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

        function selectAutocompleteItem(item) {
            const filterName = item.textContent;
            const filterData = window.allSavedFilters.find(f => f.name === filterName);
            if (filterData) {
                window.loadSavedFilter(filterData.name, filterData.expression);
            }
            filterNameInput.value = filterName;
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;
        }

        function showAllOptions() {
            autocompleteDropdown.innerHTML = '';
            currentAutocompleteSelection = -1;

            if (window.allSavedFilters.length === 0) {
                const item = document.createElement('div');
                item.className = 'autocomplete-item not-implemented';
                item.textContent = 'No saved filters';
                autocompleteDropdown.appendChild(item);
            } else {
                window.allSavedFilters.forEach(filter => {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-item';
                    item.textContent = filter.name;
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
                    filterNameInput.focus();
                }
            });
        }

        filterNameInput.addEventListener('input', () => {
            const searchTerm = filterNameInput.value.toLowerCase();
            autocompleteDropdown.innerHTML = '';
            autocompleteDropdown.style.display = 'none';
            currentAutocompleteSelection = -1;

            if (searchTerm.length === 0) return;

            const matchingFilters = window.allSavedFilters.filter(f => f.name.toLowerCase().includes(searchTerm));

            matchingFilters.forEach(filter => {
                const item = document.createElement('div');
                item.className = 'autocomplete-item';
                item.textContent = filter.name;
                item.addEventListener('mousedown', (event) => {
                    event.preventDefault();
                    selectAutocompleteItem(item);
                });
                autocompleteDropdown.appendChild(item);
            });

            if (matchingFilters.length > 0) {
                autocompleteDropdown.style.display = 'block';
            }
        });

        filterNameInput.addEventListener('focus', () => {
            if (filterNameInput.value.trim().length > 0) {
                const event = new Event('input');
                filterNameInput.dispatchEvent(event);
            }
        });

        filterNameInput.addEventListener('blur', () => {
            setTimeout(() => {
                autocompleteDropdown.style.display = 'none';
                currentAutocompleteSelection = -1;
            }, 100);
        });

        filterNameInput.addEventListener('keydown', (event) => {
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

    window.filterImages = async function(expressionToEvaluate = window.filterExpression) {
        
        const imageGrid = document.getElementById('image-grid-main');
        if (!imageGrid) {
            console.error("filterImages: #image-grid-main not found.");
            return;
        }

        const consolidatedExpression = consolidateExpression(expressionToEvaluate);

        imageGrid.innerHTML = '';
        
        window.currentPage = 0;
        window.allImagesLoaded = false;
        window.isLoading = false;

        if (consolidatedExpression.length === 0) {
            window.isFilterActive = false;
            window.filteredMatchingIds = [];
            window.filteredNonMatchingIds = [];
            
            window.loadImagesUntilScrollable();
            return;
        }

        try {
            const filterPayload = {
                expression: consolidatedExpression,
                baseImageIds: (isAnonymous && window.sharedImageIds && window.sharedImageIds.length > 0) ? window.sharedImageIds.map(id => Number(id)) : null,
                sort: window.sortField,
                direction: window.sortDirection
            };

            const response = await fetch('/images/filter', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(filterPayload)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Filter evaluation failed: ${response.status} ${response.statusText} - ${errorText}`);
            }

            const { matchingImageIds, nonMatchingImageIds } = await response.json();

            window.isFilterActive = true;
                window.filteredMatchingIds = matchingImageIds;
            window.filteredNonMatchingIds = nonMatchingImageIds;

            window.loadImagesUntilScrollable();

        } catch (error) {
            console.error('Error during filter evaluation:', error, 'Caller:', new Error().stack);
            window.displayGlobalAlert('error', 'Could not apply filter.');
        }
    };

    if (filterExpressionContainer) {
        if (clearFilterBtn) {
            clearFilterBtn.addEventListener("click", () => {
                window.clearFilter();
                if (filterNameInput) {
                    filterNameInput.value = '';
                }
            });
        }
        if (addSelectedTagsBtn) {
            addSelectedTagsBtn.addEventListener("click", () => {
                if (window.selectedTagElements && window.selectedTagElements.size > 0) {
                    Array.from(window.selectedTagElements).forEach(tagElement => {
                        const tagName = tagElement.dataset.tagName;
                        window.addTagToFilter(tagName);
                    });
                    window.clearTagSelections();
                } else {
                    window.displayGlobalAlert('info', 'No tags selected to add to the filter.');
                }
            });
        }
    }

    if (saveFilterBtn) {
        saveFilterBtn.addEventListener('click', async () => {
            if (window.filterExpression.length === 0) {
                window.displayGlobalAlert('warning', 'Filter expression is empty. Cannot save.');
                return;
            }
            let filterName = filterNameInput ? filterNameInput.value.trim() : '';
            if (!filterName) {
                filterName = generateDefaultFilterNameFromExpression();
            }
            if (!filterName) {
                window.displayGlobalAlert('warning', 'Filter name cannot be empty.');
                return;
            }

            try {
                const response = await fetch('/api/filters/save', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify({
                        name: filterName,
                        expression: JSON.stringify(window.filterExpression)
                    })
                });
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Failed to save filter.');
                }
                const savedFilter = await response.json();
                window.displayGlobalAlert('success', `Filter "${savedFilter.name}" saved successfully.`);
                await window.fetchAndRenderSavedFilters();
            } catch (error) {
                console.error('Error saving filter:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while saving the filter.');
            }
        });
    }

    if (shareFilterBarBtn) {
        shareFilterBarBtn.addEventListener('click', async () => {
            if (window.filterExpression.length === 0) {
                window.displayGlobalAlert('warning', 'Filter expression is empty. Cannot share.');
                return;
            }
            let filterName = filterNameInput ? filterNameInput.value.trim() : '';
            if (!filterName) {
                filterName = generateDefaultFilterNameFromExpression();
            }
            if (!filterName) {
                window.displayGlobalAlert('warning', 'Filter name cannot be empty.');
                return;
            }

            try {
                const response = await fetch('/api/filters/share-or-create', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/plain',
                        ...window.getCsrfHeaders()
                    },
                    body: JSON.stringify({
                        name: filterName,
                        expression: JSON.stringify(window.filterExpression)
                    })
                });

                if (!response.ok) {
                    const errorData = await response.text();
                    throw new Error(errorData || 'Failed to generate shareable link.');
                }

                const shareableLink = await response.text();
                copyToClipboard(shareableLink);
                window.displayGlobalAlert('success', 'Shareable link copied to clipboard!');
                await window.fetchAndRenderSavedFilters();
            } catch (error) {
                console.error('Error sharing filter:', error);
                window.displayGlobalAlert('error', error.message || 'An error occurred while sharing the filter.');
            }
        });
    }

    const storedFilter = localStorage.getItem(window.FILTER_STORAGE_KEY);
    if (storedFilter) {
        try {
            window.filterExpression = JSON.parse(storedFilter);
            window.lastSuccessfullyAppliedFilterExpression = [...window.filterExpression];
            window.filterImages(window.filterExpression);
        } catch (e) {
            console.error("Error parsing stored filter expression:", e);
            localStorage.removeItem(window.FILTER_STORAGE_KEY);
            window.filterImages([]);
        }
    } else {
        window.filterImages([]);
    }

    window.renderFilterExpression();

    window.fetchAndRenderSavedFilters();

    document.addEventListener('filterSelectionChanged', updateExportFilterMenuState);
});
