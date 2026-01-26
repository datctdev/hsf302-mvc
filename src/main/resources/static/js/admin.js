// Admin-specific JavaScript - Updated for MVC (no API calls)

// All admin functionality is now handled server-side through form submissions
// This file is kept for any client-side UI enhancements that may be needed

// Set active nav link based on current path
function setActiveNavLink() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-admin .nav-link').forEach(link => {
        if (link.getAttribute('data-path') === currentPath) {
            link.classList.add('active');
        }
    });
}

// Initialize admin pages
document.addEventListener('DOMContentLoaded', function() {
    // Set active nav link
    setActiveNavLink();
});
