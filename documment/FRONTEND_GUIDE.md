# Frontend Development Guide

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026

---

## 📋 Tổng Quan

Frontend của dự án sử dụng **Thymeleaf** (server-side rendering) với kiến trúc modular:
- **Thymeleaf Fragments** - Tái sử dụng components
- **Layout Templates** - Base layouts cho các loại pages
- **Modular CSS** - Tách CSS theo module
- **Modular JavaScript** - Tách JS theo module

---

## 🏗️ Kiến Trúc Frontend

### Cấu Trúc Thư Mục

```
src/main/resources/
├── templates/
│   ├── fragments/          # Reusable components
│   │   ├── header.html
│   │   ├── nav-home.html
│   │   ├── nav-admin.html
│   │   ├── nav-seller.html
│   │   └── footer.html
│   ├── layouts/            # Layout templates
│   │   ├── base.html
│   │   ├── admin-layout.html
│   │   └── seller-layout.html
│   ├── admin/              # Admin pages
│   ├── auth/               # Auth pages
│   ├── seller/             # Seller pages
│   └── home.html
└── static/
    ├── css/
    │   ├── base.css        # Reset, typography, variables
    │   ├── layout.css      # Header, nav, footer
    │   ├── components.css  # Cards, sections, modals
    │   ├── auth.css        # Auth-specific
    │   ├── admin.css       # Admin-specific
    │   └── seller.css      # Seller-specific
    └── js/
        ├── common.js       # Shared utilities
        ├── auth.js         # Auth logic
        ├── admin.js        # Admin logic
        └── seller.js       # Seller logic
```

---

## 🧩 Thymeleaf Fragments

### Sử Dụng Fragments

```html
<!-- Include header -->
<th:block th:replace="~{fragments/header :: header-simple}"></th:block>

<!-- Include navigation -->
<th:block th:replace="~{fragments/nav-home :: nav-home}"></th:block>

<!-- Include footer -->
<th:block th:replace="~{fragments/footer :: footer}"></th:block>
```

### Các Fragments Có Sẵn

1. **header-simple** - Header cho home/general pages
2. **header-admin** - Header cho admin pages
3. **nav-home** - Navigation cho home pages
4. **nav-admin** - Navigation cho admin pages
5. **nav-seller** - Navigation cho seller pages
6. **footer** - Footer chung

---

## 📐 Layout Templates

### Base Layout

Sử dụng cho home/general pages:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <link rel="stylesheet" th:href="@{/css/base.css}">
    <link rel="stylesheet" th:href="@{/css/layout.css}">
</head>
<body>
    <!-- Header, Nav, Footer tự động include -->
    <main class="main-content">
        <!-- Page content -->
    </main>
</body>
</html>
```

### Admin Layout

Sử dụng cho admin pages:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <link rel="stylesheet" th:href="@{/css/base.css}">
    <link rel="stylesheet" th:href="@{/css/layout.css}">
    <link rel="stylesheet" th:href="@{/css/admin.css}">
</head>
<body>
    <!-- Admin header và nav tự động -->
    <main class="main-content admin-content">
        <!-- Page content -->
    </main>
</body>
</html>
```

---

## 🎨 CSS Architecture

### CSS Files

1. **base.css** - Foundation
   - CSS Variables
   - Reset
   - Typography
   - Buttons
   - Forms
   - Status badges

2. **layout.css** - Layout
   - Header styles
   - Navigation styles
   - Footer styles
   - Main content

3. **components.css** - Components
   - Cards
   - Sections
   - Modals
   - Forms
   - Preview containers

4. **auth.css** - Auth-specific
   - Auth forms
   - Auth containers

5. **admin.css** - Admin-specific
   - Admin dashboard styles

6. **seller.css** - Seller-specific
   - Seller pages styles

### Sử Dụng CSS Variables

```css
/* Trong base.css */
:root {
    --primary-color: #007bff;
    --bg-color: #f4f4f4;
    --text-color: #333;
}

/* Sử dụng */
.my-element {
    color: var(--primary-color);
    background: var(--bg-color);
}
```

---

## 📜 JavaScript Architecture

### JavaScript Files

1. **common.js** - Shared utilities
   - `AuthUtils` - JWT token management
   - `FormValidator` - Form validation
   - `loadUserInfo()` - Load user info
   - `handleLogout()` - Logout handler
   - `initUserNav()` - Initialize user navigation

2. **auth.js** - Auth logic
   - `handleRegister()` - Register form
   - `handleLogin()` - Login form
   - `handleChangePassword()` - Change password
   - `handleUpdateProfile()` - Update profile
   - `handleAvatarPreview()` - Avatar preview

3. **admin.js** - Admin logic
   - `initAdminDashboard()` - Initialize admin dashboard
   - `loadStatistics()` - Load dashboard statistics

4. **seller.js** - Seller logic
   - Seller-specific functions

### Sử Dụng JavaScript

```html
<!-- Load common.js first -->
<script th:src="@{/js/common.js}"></script>

<!-- Load module-specific JS -->
<script th:src="@{/js/auth.js}"></script>

<!-- Page-specific scripts -->
<script>
    document.addEventListener('DOMContentLoaded', function() {
        // Your code here
        initUserNav();
    });
</script>
```

---

## 📄 Tạo Page Mới

### Ví Dụ: Tạo Admin Page Mới

1. **Tạo HTML file:**

```html
<!-- src/main/resources/templates/admin/new-page.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>New Page - Admin</title>
    
    <!-- CSS -->
    <link rel="stylesheet" th:href="@{/css/base.css}">
    <link rel="stylesheet" th:href="@{/css/layout.css}">
    <link rel="stylesheet" th:href="@{/css/components.css}">
    <link rel="stylesheet" th:href="@{/css/admin.css}">
</head>
<body>
    <!-- Admin Header -->
    <th:block th:replace="~{fragments/header :: header-admin}"></th:block>
    
    <!-- Admin Navigation -->
    <th:block th:replace="~{fragments/nav-admin :: nav-admin}"></th:block>

    <!-- Main Content -->
    <main class="main-content admin-content">
        <div class="container">
            <div class="section">
                <h2>New Page</h2>
                <!-- Your content here -->
            </div>
        </div>
    </main>

    <!-- JavaScript -->
    <script th:src="@{/js/common.js}"></script>
    <script th:src="@{/js/admin.js}"></script>
    <script>
        // Page-specific scripts
    </script>
</body>
</html>
```

2. **Thêm route trong Controller:**

```java
@GetMapping("/admin/new-page")
public String newPage() {
    return "admin/new-page";
}
```

---

## 🎯 Best Practices

### 1. Sử Dụng Fragments

✅ **DO:**
```html
<th:block th:replace="~{fragments/header :: header-simple}"></th:block>
```

❌ **DON'T:**
```html
<header>
    <h1>Chào mừng...</h1>
</header>
```

### 2. CSS Organization

✅ **DO:** Sử dụng CSS files riêng
```html
<link rel="stylesheet" th:href="@{/css/base.css}">
<link rel="stylesheet" th:href="@{/css/layout.css}">
```

❌ **DON'T:** Inline CSS
```html
<style>
    body { ... }
</style>
```

### 3. JavaScript Organization

✅ **DO:** Sử dụng module files
```html
<script th:src="@{/js/common.js}"></script>
<script th:src="@{/js/auth.js}"></script>
```

❌ **DON'T:** Inline large scripts
```html
<script>
    // 500 lines of code...
</script>
```

### 4. Naming Conventions

- **CSS Classes:** kebab-case (`nav-home`, `user-info`)
- **JavaScript Functions:** camelCase (`handleLogin`, `loadUserInfo`)
- **IDs:** camelCase (`userName`, `loginForm`)

### 5. Responsive Design

Sử dụng CSS variables và media queries:

```css
@media (max-width: 768px) {
    .container {
        padding: 0 0.5rem;
    }
}
```

---

## 🔧 Common Tasks

### Thêm Navigation Item

**File:** `fragments/nav-admin.html`

```html
<nav th:fragment="nav-admin" class="nav-admin">
    <div class="nav-content">
        <a href="/admin/dashboard">Dashboard</a>
        <a href="/admin/new-page">New Page</a>  <!-- Add here -->
    </div>
</nav>
```

### Thêm CSS Class Mới

**File:** `css/components.css`

```css
.my-new-component {
    /* Styles */
}
```

### Thêm JavaScript Function

**File:** `js/common.js` (nếu shared) hoặc module-specific file

```javascript
function myNewFunction() {
    // Implementation
}
```

---

## 📚 Resources

- **Thymeleaf Docs:** https://www.thymeleaf.org/documentation.html
- **CSS Variables:** https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties
- **JavaScript Modules:** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules

---

**Cập nhật:** 11-01-2026
