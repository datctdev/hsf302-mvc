// Common Utilities - Shared across all pages

// Note: JWT Token Management has been removed as we're using session-based authentication
// All authentication is now handled server-side through Spring Security

// Form Validation
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

// Load user info - No longer needed, handled server-side
// This function is kept for backward compatibility
async function loadUserInfo() {
    // User info is now loaded from server-side Model in Thymeleaf templates
    return null;
}

// Logout Handler (shared) - Now handled by form submission in templates
// This function is kept for backward compatibility but does nothing
function handleLogout() {
    // Logout is now handled by form submission in Thymeleaf templates
    // This function is kept for backward compatibility
    console.log('Logout should be handled by form submission');
}

// Initialize user info in navigation (for home pages)
// No longer needed - handled by Thymeleaf in templates
function initUserNav() {
    // User info is now loaded from server-side Model in Thymeleaf templates
    // This function is kept for backward compatibility
}
