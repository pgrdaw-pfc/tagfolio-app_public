/**
 * @file This file handles the image upload functionality, including drag-and-drop and conflict resolution.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * Prevents default event behavior.
 * @param {Event} e - The event.
 */
function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

/**
 * Gets the HTML for metadata differences.
 * @param {Object} originalMetadata - The original metadata.
 * @param {Object} newMetadata - The new metadata.
 * @returns {string} The HTML string.
 */
function getMetadataDifferencesHtml(originalMetadata, newMetadata) {
    const keysToCheck = window.displayMetadataKeys || [];
    const ignoredKeys = ['FileName'];
    let differencesHtml = '';

    keysToCheck.forEach(key => {
        if (ignoredKeys.includes(key)) {
            return;
        }

        const originalValue = originalMetadata[key];
        const newValue = newMetadata[key];

        if (JSON.stringify(originalValue) !== JSON.stringify(newValue)) {
            differencesHtml += `
                <div class="metadata-diff-item">
                    <strong>${key}:</strong>
                    <ul>
                        <li>Original: ${originalValue ? JSON.stringify(originalValue) : 'N/A'}</li>
                        <li>New: ${newValue ? JSON.stringify(newValue) : 'N/A'}</li>
                    </ul>
                </div>
            `;
        }
    });

    return differencesHtml || '<p>No significant metadata differences detected for display.</p>';
}

/**
 * Uploads a list of files.
 * @param {FileList} files - The files to upload.
 */
async function uploadFiles(files) {
    if (!files || files.length === 0) return;

    const uploadOverlay = document.getElementById('upload-overlay');
    if (uploadOverlay) {
        uploadOverlay.classList.add('visible');
    }

    const formData = new FormData();
    for (let file of files) {
        formData.append('images[]', file);
    }

    let reloadPage = false;

    try {
        const response = await fetch('/images', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                ...window.getCsrfHeaders()
            },
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Upload failed.');
        }

        const data = await response.json();

        if (data.uploaded && data.uploaded.length > 0) {
            window.displayGlobalAlert('success', `Successfully uploaded ${data.uploaded.length} image(s)!`);
            reloadPage = true;
        }
        if (data.skipped && data.skipped.length > 0) {
            const skippedFiles = data.skipped.map(img => `${img.originalFileName} (created at ${new Date(img.createdAt).toLocaleString()})`).join(', ');
            window.displayGlobalAlert('info', `Skipped ${data.skipped.length} image(s) because they already exist: ${skippedFiles}`);
        }
        if (data.conflicted && data.conflicted.length > 0) {
            let conflictContentHtml = '';
            data.conflicted.forEach(conflict => {
                conflictContentHtml += `
                    <div class="conflict-entry">
                        <h4>${conflict.fileName}</h4>
                        <div class="metadata-differences">
                            ${getMetadataDifferencesHtml(conflict.originalMetadata, conflict.newMetadata)}
                        </div>
                        <hr>
                    </div>
                `;
            });

            const userAction = await new Promise(resolve => {
                window.displayConfirmationAlert({
                    message: `Conflicts Detected for ${data.conflicted.length} image(s).`,
                    contentHtml: conflictContentHtml,
                    confirmText: 'Overwrite All',
                    cancelText: 'Cancel All',
                    onConfirm: async () => {
                        const overwritePromises = data.conflicted.map(async (conflict) => {
                            const fileToOverwrite = Array.from(files).find(f => f.name === conflict.fileName);
                            if (fileToOverwrite) {
                                const overwriteFormData = new FormData();
                                overwriteFormData.append('image', fileToOverwrite);
                                try {
                                    const overwriteResponse = await fetch('/images/overwrite', {
                                        method: 'POST',
                                        headers: {
                                            'Accept': 'application/json',
                                            'X-Requested-With': 'XMLHttpRequest',
                                            ...window.getCsrfHeaders()
                                        },
                                        body: overwriteFormData
                                    });
                                    if (!overwriteResponse.ok) {
                                        const errorData = await overwriteResponse.json();
                                        throw new Error(errorData.message || `Failed to overwrite ${conflict.fileName}.`);
                                    }
                                    window.displayGlobalAlert('success', `Successfully overwritten ${conflict.fileName}.`);
                                } catch (error) {
                                    console.error(`Error overwriting ${conflict.fileName}:`, error);
                                    window.displayGlobalAlert('error', `Failed to overwrite ${conflict.fileName}: ${error.message || 'Unknown error'}`);
                                }
                            }
                        });
                        await Promise.all(overwritePromises);
                        resolve('overwrite');
                    },
                    onCancel: () => resolve('cancel')
                });
            });

            if (userAction === 'overwrite') {
                reloadPage = true;
            }
        }

        if (reloadPage) {
            window.location.reload();
        } else if ((!data.uploaded || data.uploaded.length === 0) && (!data.conflicted || data.conflicted.length === 0) && data.skipped.length === files.length) {
            window.displayGlobalAlert('info', 'All selected images were already present and identical.');
        }

    } catch (error) {
        console.error('Upload error:', error);
        let errorMessage = 'An unexpected error occurred during image upload.';

        if (error instanceof TypeError && (error.message.includes('NetworkError') || error.message.includes('Failed to fetch'))) {
            errorMessage = 'Upload failed: The total size of your images might exceed the server\'s upload limit, or the network connection was interrupted. Please try uploading fewer or smaller files.';
        } else if (error.message) {
            errorMessage = error.message;
        }
        window.displayGlobalAlert('error', errorMessage);
    } finally {
        if (uploadOverlay) {
            uploadOverlay.classList.remove('visible');
        }
        const fileInput = document.getElementById('fileInput');
        if (fileInput) fileInput.value = '';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const mainContentArea = document.getElementById('main-content-area');
    const uploadImagesBtn = document.getElementById('upload-images-btn');
    const fileInput = document.createElement('input');

    fileInput.type = 'file';
    fileInput.multiple = true;
    fileInput.accept = 'image/*';
    fileInput.style.display = 'none';
    document.body.appendChild(fileInput);

    if (uploadImagesBtn) {
        uploadImagesBtn.addEventListener('click', (event) => {
            event.preventDefault();
            fileInput.click();
        });
    }

    fileInput.addEventListener('change', async () => {
        await uploadFiles(fileInput.files);
    });

    if (mainContentArea) {
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            mainContentArea.addEventListener(eventName, preventDefaults, false);
        });
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            document.body.addEventListener(eventName, preventDefaults, false);
        });

        mainContentArea.addEventListener('dragenter', (e) => {
            mainContentArea.classList.add('drag-over');
        });

        mainContentArea.addEventListener('dragleave', (e) => {
            if (!mainContentArea.contains(e.relatedTarget)) {
                mainContentArea.classList.remove('drag-over');
            }
        });

        mainContentArea.addEventListener('drop', async (e) => {
            mainContentArea.classList.remove('drag-over');
            await uploadFiles(e.dataTransfer.files);
        });
    }
});
