/**
 * @file This file handles the display and behavior of global alerts and confirmation dialogs.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

/**
 * Holds the active confirmation alert's details.
 * @type {Object|null}
 */
window.activeConfirmation = null;

/**
 * Displays a global alert message.
 *
 * @param {string} type - The type of alert (e.g., 'success', 'error').
 * @param {string} message - The message to display.
 */
window.displayGlobalAlert = function(type, message) {
    const container = document.getElementById('global-alert-container');
    if (!container) return;

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.opacity = '1';
    alertDiv.style.transition = 'opacity 0.5s ease';
    alertDiv.style.marginBottom = '10px';

    container.appendChild(alertDiv);

    setTimeout(() => {
        alertDiv.style.opacity = '0';
        alertDiv.addEventListener('transitionend', () => alertDiv.remove());
    }, 5000);
};

/**
 * Displays a confirmation alert with customizable options.
 *
 * @param {Object} options - The options for the confirmation alert.
 * @param {string} options.message - The message to display.
 * @param {string} [options.contentHtml] - Optional HTML content to display.
 * @param {string} [options.confirmText='Accept'] - The text for the confirm button.
 * @param {string} [options.cancelText='Cancel'] - The text for the cancel button.
 * @param {function} [options.onConfirm] - The callback function to execute on confirmation.
 * @param {function} [options.onCancel] - The callback function to execute on cancellation.
 */
window.displayConfirmationAlert = function(options) {
    const container = document.getElementById('global-alert-container');
    if (!container) return;

    if (window.activeConfirmation && window.activeConfirmation.element) {
        window.activeConfirmation.element.remove();
        window.activeConfirmation = null;
    }

    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-confirmation';

    const messageDiv = document.createElement('div');
    messageDiv.textContent = options.message || 'Are you sure?';
    alertDiv.appendChild(messageDiv);

    if (options.contentHtml) {
        const contentDiv = document.createElement('div');
        contentDiv.innerHTML = options.contentHtml;
        contentDiv.classList.add('alert-content');
        alertDiv.appendChild(contentDiv);
    }

    const buttonsDiv = document.createElement('div');
    buttonsDiv.className = 'alert-buttons';

    const confirmBtn = document.createElement('button');
    confirmBtn.className = 'alert-btn accept';
    confirmBtn.textContent = options.confirmText || 'Accept';
    confirmBtn.onclick = (event) => {
        event.stopPropagation();
        if (options.onConfirm) options.onConfirm();
        alertDiv.remove();
        window.activeConfirmation = null;
    };

    const cancelBtn = document.createElement('button');
    cancelBtn.className = 'alert-btn cancel';
    cancelBtn.textContent = options.cancelText || 'Cancel';
    cancelBtn.onclick = (event) => {
        event.stopPropagation();
        if (options.onCancel) options.onCancel();
        alertDiv.remove();
        window.activeConfirmation = null;
    };

    buttonsDiv.appendChild(confirmBtn);
    buttonsDiv.appendChild(cancelBtn);
    alertDiv.appendChild(buttonsDiv);

    container.appendChild(alertDiv);

    window.activeConfirmation = {
        element: alertDiv,
        onConfirm: options.onConfirm,
        onCancel: options.onCancel
    };
};

document.addEventListener('keydown', (event) => {
    if (window.activeConfirmation) {
        if (event.key === 'Enter') {
            event.preventDefault();
            if (window.activeConfirmation.onConfirm) {
                window.activeConfirmation.onConfirm();
            }
            window.activeConfirmation.element.remove();
            window.activeConfirmation = null;
        } else if (event.key === 'Escape') {
            event.preventDefault();
            if (window.activeConfirmation.onCancel) {
                window.activeConfirmation.onCancel();
            }
            window.activeConfirmation.element.remove();
            window.activeConfirmation = null;
        }
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.addEventListener('transitionend', () => alert.remove());
        }, 5000);
    });
});
