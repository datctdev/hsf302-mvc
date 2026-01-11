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

// Register Form Handler
async function handleRegister(event) {
    event.preventDefault();
    FormValidator.clearErrors();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const fullName = document.getElementById('fullName').value.trim();
    const phoneNumber = document.getElementById('phoneNumber').value.trim();

    // Validation
    let hasError = false;

    if (!email) {
        FormValidator.showError('emailError', 'Email không được để trống');
        hasError = true;
    } else if (!FormValidator.validateEmail(email)) {
        FormValidator.showError('emailError', 'Email không hợp lệ');
        hasError = true;
    }

    if (!password) {
        FormValidator.showError('passwordError', 'Mật khẩu không được để trống');
        hasError = true;
    } else if (!FormValidator.validatePassword(password)) {
        FormValidator.showError('passwordError', 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số');
        hasError = true;
    }

    if (password !== confirmPassword) {
        FormValidator.showError('confirmPasswordError', 'Mật khẩu xác nhận không khớp');
        hasError = true;
    }

    if (!fullName) {
        FormValidator.showError('fullNameError', 'Họ và tên không được để trống');
        hasError = true;
    }

    if (hasError) {
        return;
    }

    // Disable submit button
    const submitBtn = document.getElementById('registerBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang xử lý...';

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email,
                password,
                fullName,
                phoneNumber: phoneNumber || null
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Lưu token
            AuthUtils.saveToken(data.token);
            
            // Lưu refresh token nếu có
            if (data.refreshToken) {
                AuthUtils.saveRefreshToken(data.refreshToken);
            }
            
            // Hiển thị thông báo thành công
            alert('Đăng ký thành công! Bạn sẽ được chuyển đến trang chủ.');
            
            // Redirect về trang chủ
            window.location.href = '/';
        } else {
            // Hiển thị lỗi
            if (data.errors) {
                // Validation errors từ server
                Object.keys(data.errors).forEach(field => {
                    FormValidator.showError(field + 'Error', data.errors[field]);
                });
            } else {
                alert(data.message || 'Đăng ký thất bại. Vui lòng thử lại.');
            }
        }
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Đăng ký';
    }
}

// Login Form Handler
async function handleLogin(event) {
    event.preventDefault();
    FormValidator.clearErrors();

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    // Validation
    let hasError = false;

    if (!email) {
        FormValidator.showError('emailError', 'Email không được để trống');
        hasError = true;
    } else if (!FormValidator.validateEmail(email)) {
        FormValidator.showError('emailError', 'Email không hợp lệ');
        hasError = true;
    }

    if (!password) {
        FormValidator.showError('passwordError', 'Mật khẩu không được để trống');
        hasError = true;
    }

    if (hasError) {
        return;
    }

    // Disable submit button
    const submitBtn = document.getElementById('loginBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang đăng nhập...';

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                email,
                password,
                rememberMe: document.getElementById('rememberMe')?.checked || false
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Lưu token
            AuthUtils.saveToken(data.token);
            
            // Lưu refresh token nếu có
            if (data.refreshToken) {
                AuthUtils.saveRefreshToken(data.refreshToken);
            }
            
            // Hiển thị thông báo thành công
            alert('Đăng nhập thành công!');
            
            // Redirect về trang chủ
            window.location.href = '/';
        } else {
            // Hiển thị lỗi
            const errorElement = document.getElementById('loginError');
            if (errorElement) {
                errorElement.textContent = data.message || 'Email hoặc mật khẩu không đúng';
                errorElement.style.display = 'block';
            } else {
                alert(data.message || 'Email hoặc mật khẩu không đúng');
            }
        }
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Đăng nhập';
    }
}

// Logout Handler
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

// Change Password Handler
async function handleChangePassword(event) {
    event.preventDefault();
    FormValidator.clearErrors();

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    // Validation
    let hasError = false;

    if (!currentPassword) {
        FormValidator.showError('currentPasswordError', 'Mật khẩu hiện tại không được để trống');
        hasError = true;
    }

    if (!newPassword) {
        FormValidator.showError('newPasswordError', 'Mật khẩu mới không được để trống');
        hasError = true;
    } else if (!FormValidator.validatePassword(newPassword)) {
        FormValidator.showError('newPasswordError', 'Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số');
        hasError = true;
    }

    if (newPassword !== confirmPassword) {
        FormValidator.showError('confirmPasswordError', 'Mật khẩu xác nhận không khớp');
        hasError = true;
    }

    if (hasError) {
        return;
    }

    // Disable submit button
    const submitBtn = document.getElementById('changePasswordBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang xử lý...';

    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword,
                newPassword,
                confirmPassword
            })
        });

        // Kiểm tra response status trước khi parse JSON
        if (!response.ok) {
            let errorMessage = 'Đổi mật khẩu thất bại. Vui lòng thử lại.';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                // Nếu không parse được JSON, dùng message mặc định
            }
            alert(errorMessage);
            submitBtn.disabled = false;
            submitBtn.textContent = 'Đổi Mật Khẩu';
            return;
        }

        // Parse JSON response
        let data;
        try {
            const responseText = await response.text();
            if (responseText) {
                data = JSON.parse(responseText);
            } else {
                data = { message: 'Đổi mật khẩu thành công!' };
            }
        } catch (parseError) {
            data = { message: 'Đổi mật khẩu thành công!' };
        }
        
        // Hiển thị thông báo thành công và redirect
        alert(data.message || 'Đổi mật khẩu thành công!');
        
        // Redirect về trang chủ - sử dụng replace để không thể quay lại
        window.location.replace('/');
        
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Đổi Mật Khẩu';
    }
}

// Update Profile Handler
async function handleUpdateProfile(event) {
    event.preventDefault();
    FormValidator.clearErrors();

    const fullName = document.getElementById('fullName').value.trim();
    const phoneNumber = document.getElementById('phoneNumber').value.trim();
    const avatarFile = document.getElementById('avatarFile').files[0];

    // Disable submit button
    const submitBtn = document.getElementById('updateProfileBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Đang cập nhật...';

    try {
        let finalAvatarUrl = null;

        // Upload avatar file if provided
        if (avatarFile) {
            // Validate file size (25MB)
            if (avatarFile.size > 25 * 1024 * 1024) {
                FormValidator.showError('avatarFileError', 'Kích thước file không được vượt quá 25MB');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Cập Nhật';
                return;
            }

            // Validate file type
            if (!avatarFile.type.startsWith('image/')) {
                FormValidator.showError('avatarFileError', 'Chỉ chấp nhận file ảnh');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Cập Nhật';
                return;
            }

            const formData = new FormData();
            formData.append('file', avatarFile);
            formData.append('folder', 'avatars');

            const uploadResponse = await AuthUtils.fetchWithAuth('/api/files/upload', {
                method: 'POST',
                body: formData
            });

            if (uploadResponse.ok) {
                const uploadData = await uploadResponse.json();
                finalAvatarUrl = uploadData.url;
            } else {
                const errorData = await uploadResponse.json().catch(() => ({}));
                FormValidator.showError('avatarFileError', errorData.message || 'Upload ảnh thất bại. Vui lòng thử lại.');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Cập Nhật';
                return;
            }
        }

        // Update profile
        const response = await AuthUtils.fetchWithAuth('/api/auth/profile', {
            method: 'PUT',
            body: JSON.stringify({
                fullName: fullName || null,
                phoneNumber: phoneNumber || null,
                avatarUrl: finalAvatarUrl
            })
        });

        const data = await response.json();

        if (response.ok) {
            alert('Cập nhật thông tin thành công!');
            // Reload page to show updated info
            window.location.reload();
        } else {
            if (data.message) {
                alert(data.message);
            } else {
                alert('Cập nhật thông tin thất bại. Vui lòng thử lại.');
            }
        }
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Cập Nhật';
    }
}

// Avatar Preview Handler
function handleAvatarPreview(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const previewDiv = document.getElementById('avatarPreview');
            const previewImg = document.getElementById('avatarPreviewImg');
            previewImg.src = e.target.result;
            previewDiv.style.display = 'block';
        };
        reader.readAsDataURL(file);
    } else {
        document.getElementById('avatarPreview').style.display = 'none';
    }
}

// Deactivate Account Handler
async function handleDeactivateAccount() {
    if (!confirm('Bạn có chắc chắn muốn vô hiệu hóa tài khoản? Bạn sẽ không thể đăng nhập cho đến khi kích hoạt lại.')) {
        return;
    }

    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/deactivate', {
            method: 'POST'
        });

        if (response.ok) {
            const data = await response.json();
            alert(data.message || 'Tài khoản đã được vô hiệu hóa. Bạn sẽ được đăng xuất.');
            AuthUtils.removeToken();
            window.location.href = '/login';
        } else {
            const data = await response.json();
            alert(data.message || 'Vô hiệu hóa tài khoản thất bại. Vui lòng thử lại.');
        }
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
    }
}

// Activate Account Handler
async function handleActivateAccount() {
    try {
        const response = await AuthUtils.fetchWithAuth('/api/auth/activate', {
            method: 'POST'
        });

        if (response.ok) {
            const data = await response.json();
            alert(data.message || 'Tài khoản đã được kích hoạt thành công!');
            window.location.reload();
        } else {
            const data = await response.json();
            alert(data.message || 'Kích hoạt tài khoản thất bại. Vui lòng thử lại.');
        }
    } catch (error) {
        alert('Đã xảy ra lỗi. Vui lòng thử lại sau.');
    }
}
