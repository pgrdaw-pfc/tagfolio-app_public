/**
 * @file This file handles the behavior of the user list page, including inline editing and deletion of users.
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.delete-form').forEach(form => {
        form.addEventListener('submit', function(event) {
            event.preventDefault();

            const formToSubmit = this;

            window.displayConfirmationAlert({
                message: 'Are you sure you want to delete this user?',
                onConfirm: function() {
                    formToSubmit.submit();
                }
            });
        });
    });

    document.querySelectorAll('.edit-user-button').forEach(button => {
        button.addEventListener('click', function() {
            const userId = this.dataset.userId;
            const userRow = document.getElementById('user-row-' + userId);

            if (!userRow) {
                return;
            }

            userRow.classList.add('is-editing');

            const emailDisplay = userRow.querySelector('.user-email-display');
            const emailEdit = userRow.querySelector('.user-email-edit');
            if (emailDisplay && emailEdit) {
                emailEdit.value = emailDisplay.textContent.trim();
            }


            const rolesDisplay = userRow.querySelector('.user-roles-display');
            const rolesEdit = userRow.querySelector('.user-roles-edit');
            if (rolesDisplay && rolesEdit) {
                const currentRoleSpan = rolesDisplay.querySelector('span');
                const currentRoleName = currentRoleSpan ? currentRoleSpan.textContent.trim() : '';

                Array.from(rolesEdit.options).forEach(option => {
                    option.selected = option.textContent.trim() === currentRoleName;
                });
            }
        });
    });

    document.querySelectorAll('.cancel-edit-button').forEach(button => {
        button.addEventListener('click', function() {
            const userId = this.dataset.userId;
            const userRow = document.getElementById('user-row-' + userId);
            userRow?.classList.remove('is-editing');
        });
    });

    document.querySelectorAll('.inline-update-form').forEach(form => {
        form.addEventListener('submit', function(event) {
            const userRow = event.target.closest('tr');
            const emailInput = userRow.querySelector('.user-email-edit');
            const roleSelect = userRow.querySelector('.user-roles-edit');

            if (emailInput && roleSelect) {
                form.querySelector('.edit-email-input').value = emailInput.value;
                form.querySelector('.edit-role-input').value = roleSelect.value;
            } else {
                event.preventDefault();
            }
        });
    });
});
