// Seller-specific JavaScript

// Initialize seller pages
document.addEventListener('DOMContentLoaded', async function() {
    const token = AuthUtils.getToken();
    if (!token) {
        window.location.href = '/login?expired=true';
        return;
    }

    // Check if user is seller and show seller shop link
    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/me');
        if (response.ok) {
            const user = await response.json();
            const roles = user.roles || [];
            
            // Show seller shop link if user is seller
            if (roles.includes('ROLE_SELLER')) {
                const sellerShopLink = document.getElementById('sellerShopLink');
                if (sellerShopLink) {
                    sellerShopLink.style.display = 'inline-block';
                }
            }
        }
    } catch (error) {
        // Ignore errors
    }
});
