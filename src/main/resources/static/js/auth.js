// Auth-specific JavaScript - Updated for MVC (no API calls)

// Form Validation Utilities (kept for client-side validation)
const FormValidator = {
    validateEmail: function(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },

    validatePassword: function(password) {
        // Min 8 chars, có chữ hoa, chữ thường, số
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        return passwordRegex.test(password);
    },

    showError: function(elementId, message) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = message;
            element.style.display = 'block';
            element.style.color = '#dc3545';
        }
    },

    hideError: function(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.style.display = 'none';
        }
    },

    clearErrors: function() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(el => {
            el.style.display = 'none';
            el.textContent = '';
        });
    }
};

// Real-time validation for login form
function initLoginValidation() {
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const loginForm = document.getElementById('loginForm');

    if (!loginForm || !emailInput || !passwordInput) {
        return;
    }

    // Email validation
    emailInput.addEventListener('blur', function() {
        const email = this.value.trim();
        if (email && !FormValidator.validateEmail(email)) {
            FormValidator.showError('emailError', 'Email không hợp lệ');
        } else {
            FormValidator.hideError('emailError');
        }
    });

    emailInput.addEventListener('input', function() {
        if (this.value.trim()) {
            FormValidator.hideError('emailError');
        }
    });

    // Password validation
    passwordInput.addEventListener('input', function() {
        if (this.value) {
            FormValidator.hideError('passwordError');
        }
    });
}

// Initialize validation when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initLoginValidation);
} else {
    initLoginValidation();
}

// Avatar Preview Handler (for profile page)
function handleAvatarPreview(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const previewDiv = document.getElementById('avatarPreview');
            const previewImg = document.getElementById('avatarPreviewImg');
            if (previewDiv && previewImg) {
                previewImg.src = e.target.result;
                previewDiv.style.display = 'block';
            }
        };
        reader.readAsDataURL(file);
    } else {
        const previewDiv = document.getElementById('avatarPreview');
        if (previewDiv) {
            previewDiv.style.display = 'none';
        }
    }
}
