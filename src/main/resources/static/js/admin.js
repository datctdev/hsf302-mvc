// Admin-specific JavaScript

// Initialize admin dashboard
async function initAdminDashboard() {
    const token = AuthUtils.getToken();
    if (!token) {
        window.location.href = '/login?expired=true';
        return;
    }

    // Check if user is admin
    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/me');
        if (response.ok) {
            const user = await response.json();
            const roles = user.roles || [];
            
            // Check if user has ADMIN role
            if (!roles.includes('ROLE_ADMIN')) {
                alert('Bạn không có quyền truy cập trang này.');
                window.location.href = '/';
                return;
            }
            
            // Set admin name
            const adminNameEl = document.getElementById('adminName');
            if (adminNameEl) {
                adminNameEl.textContent = user.fullName || user.email;
            }
        } else {
            window.location.href = '/login?expired=true';
            return;
        }
    } catch (error) {
        window.location.href = '/login?expired=true';
        return;
    }

    // Load statistics
    await loadStatistics();
}

// Load dashboard statistics
async function loadStatistics() {
    try {
        // Load pending seller requests
        const requestsResponse = await AuthUtils.fetchWithAuth('/api/admin/seller-requests?status=PENDING');
        if (requestsResponse.ok) {
            const requests = await requestsResponse.json();
            const pendingEl = document.getElementById('pendingRequests');
            if (pendingEl) {
                pendingEl.textContent = requests.length || 0;
            }
        }
    } catch (error) {
        // Ignore errors for now
    }

    // TODO: Load other statistics when APIs are ready
    const totalUsersEl = document.getElementById('totalUsers');
    if (totalUsersEl) {
        totalUsersEl.textContent = '-';
    }
    const totalShopsEl = document.getElementById('totalShops');
    if (totalShopsEl) {
        totalShopsEl.textContent = '-';
    }
    const totalOrdersEl = document.getElementById('totalOrders');
    if (totalOrdersEl) {
        totalOrdersEl.textContent = '-';
    }
}

// Initialize admin pages
document.addEventListener('DOMContentLoaded', function() {
    // Check if we're on an admin page
    if (window.location.pathname.startsWith('/admin')) {
        initAdminDashboard();
    }
});
