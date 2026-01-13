// Common Utilities - Shared across all pages

// JWT Token Management
const AuthUtils = {
    // Lưu token vào localStorage
    saveToken: function(token) {
        localStorage.setItem('jwt_token', token);
    },

    // Lưu refresh token
    saveRefreshToken: function(refreshToken) {
        localStorage.setItem('refresh_token', refreshToken);
    },

    // Lấy token từ localStorage
    getToken: function() {
        return localStorage.getItem('jwt_token');
    },

    // Lấy refresh token
    getRefreshToken: function() {
        return localStorage.getItem('refresh_token');
    },

    // Xóa token
    removeToken: function() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('refresh_token');
    },

    // Kiểm tra đã đăng nhập chưa
    isAuthenticated: function() {
        return this.getToken() !== null;
    },

    // Lấy Authorization header
    getAuthHeader: function() {
        const token = this.getToken();
        return token ? `Bearer ${token}` : '';
    },

    // Gửi request với JWT token
    fetchWithAuth: async function(url, options = {}) {
        const token = this.getToken();
        if (!token) {
            throw new Error('Chưa đăng nhập');
        }

        // Don't set Content-Type for FormData, let browser set it with boundary
        const isFormData = options.body instanceof FormData;
        const headers = {
            'Authorization': `Bearer ${token}`,
            ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
            ...options.headers
        };

        let response = await fetch(url, {
            ...options,
            headers
        });

        // Nếu token hết hạn, thử refresh token
        if (response.status === 401) {
            const refreshToken = this.getRefreshToken();
            if (refreshToken) {
                try {
                    const refreshResponse = await fetch('/api/auth/refresh', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ refreshToken })
                    });

                    if (refreshResponse.ok) {
                        const data = await refreshResponse.json();
                        this.saveToken(data.token);
                        if (data.refreshToken) {
                            this.saveRefreshToken(data.refreshToken);
                        }

                        // Retry original request với token mới
                        headers['Authorization'] = `Bearer ${data.token}`;
                        response = await fetch(url, {
                            ...options,
                            headers
                        });
                    } else {
                        // Refresh token cũng không hợp lệ
                        this.removeToken();
                        window.location.href = '/login?expired=true';
                        throw new Error('Phiên đăng nhập đã hết hạn');
                    }
                } catch (error) {
                    this.removeToken();
                    window.location.href = '/login?expired=true';
                    throw new Error('Phiên đăng nhập đã hết hạn');
                }
            } else {
                // Không có refresh token
                this.removeToken();
                window.location.href = '/login?expired=true';
                throw new Error('Phiên đăng nhập đã hết hạn');
            }
        }

        return response;
    }
};

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

// Load user info
async function loadUserInfo() {
    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/me');
        if (response.ok) {
            const user = await response.json();
            return user;
        }
    } catch (error) {
        // Error loading user info
    }
    return null;
}

// Logout Handler (shared)
async function handleLogout() {
    try {
        const token = AuthUtils.getToken();
        if (token) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
        }
    } catch (error) {
        // Ignore logout errors
    } finally {
        // Xóa token và redirect
        AuthUtils.removeToken();
        window.location.href = '/login';
    }
}

// Initialize user info in navigation (for home pages)
async function initUserNav() {
    const token = AuthUtils.getToken();
    if (token) {
        try {
            const user = await loadUserInfo();
            if (user) {
                // Hide login/register links
                const authLinks = document.getElementById('authLinks');
                if (authLinks) {
                    authLinks.style.display = 'none';
                }
                
                // Show user info
                const userInfoDiv = document.getElementById('userInfo');
                if (userInfoDiv) {
                    userInfoDiv.style.display = 'flex';
                    const userNameEl = document.getElementById('userName');
                    if (userNameEl) {
                        userNameEl.textContent = user.fullName || user.email;
                    }
                }
            }
        } catch (error) {
            // If error, show login links
            const authLinks = document.getElementById('authLinks');
            if (authLinks) {
                authLinks.style.display = 'block';
            }
            const userInfoDiv = document.getElementById('userInfo');
            if (userInfoDiv) {
                userInfoDiv.style.display = 'none';
            }
        }
    } else {
        // Show login/register links
        const authLinks = document.getElementById('authLinks');
        if (authLinks) {
            authLinks.style.display = 'block';
        }
        const userInfoDiv = document.getElementById('userInfo');
        if (userInfoDiv) {
            userInfoDiv.style.display = 'none';
        }
    }
}
