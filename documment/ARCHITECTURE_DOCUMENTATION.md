# Architecture Documentation

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026

---

## 🏗️ System Architecture

### Overview

Dự án sử dụng **Modular Monolith** architecture với Spring Boot MVC:
- **Backend:** Spring Boot 4.0.1 (Java 21)
- **Frontend:** Thymeleaf (Server-Side Rendering)
- **Database:** PostgreSQL
- **File Storage:** MinIO
- **Authentication:** JWT (JSON Web Tokens)

### Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│              Browser (Client)                   │
│  (HTML/CSS/JS rendered by Thymeleaf)           │
└──────────────────┬──────────────────────────────┘
                   │ HTTP Request
                   ▼
┌─────────────────────────────────────────────────┐
│         Spring Boot Application                  │
│                                                  │
│  ┌──────────────┐  ┌─────────────────────────┐ │
│  │ Controllers  │──│   Service Layer         │ │
│  │  (MVC)       │  │   (Business Logic)      │ │
│  └──────┬───────┘  └───────────┬─────────────┘ │
│         │                      │                │
│         │                      ▼                │
│         │              ┌──────────────┐         │
│         │              │ Repositories │         │
│         │              │  (Data Access)│         │
│         │              └──────┬───────┘         │
│         │                     │                 │
│         └─────────────────────┘                 │
│                   │                             │
└───────────────────┼─────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
    PostgreSQL    MinIO    External APIs
    (Database)   (Storage)  (Payment, AI)
```

---

## 📦 Package Structure

### Module Organization

```
com.hsf.e_comerce/
├── auth/                    # Authentication Module
│   ├── controller/          # AuthController
│   ├── dto/                 # Request/Response DTOs
│   ├── entity/              # User, Role, RefreshToken
│   ├── repository/          # Data access
│   └── service/             # Business logic
│       └── impl/            # Service implementations
│
├── seller/                  # Seller Module
│   ├── controller/          # SellerController, AdminSellerController
│   ├── dto/                 # SellerRequest DTOs
│   ├── entity/              # SellerRequest
│   ├── repository/          # SellerRequestRepository
│   ├── service/             # SellerRequestService
│   └── valueobject/         # SellerRequestStatus enum
│
├── shop/                    # Shop Module
│   ├── controller/          # ShopController
│   ├── dto/                 # Shop DTOs
│   ├── entity/              # Shop
│   ├── repository/          # ShopRepository
│   ├── service/             # ShopService
│   └── valueobject/         # ShopStatus enum
│
├── file/                    # File Upload Module
│   ├── controller/          # FileController
│   ├── dto/                 # FileUploadResponse
│   └── service/             # FileService
│
├── common/                  # Common Module
│   ├── controller/          # HomeController
│   ├── dto/                 # Shared DTOs
│   └── exception/           # Custom exceptions
│
└── config/                  # Configuration
    ├── SecurityConfig       # Spring Security
    ├── JwtAuthenticationFilter
    ├── MinIOConfig
    └── DataInitializer      # Default data
```

---

## 🔄 Layer Architecture

### 1. Controller Layer

**Responsibility:** Handle HTTP requests, return views or JSON

**Pattern:** MVC Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Delegate to service
        return ResponseEntity.ok(authService.login(request));
    }
}
```

### 2. Service Layer

**Responsibility:** Business logic, validation, orchestration

**Pattern:** Service Interface + Implementation

```java
public interface AuthService {
    AuthResponse login(LoginRequest request);
}

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    
    @Override
    public AuthResponse login(LoginRequest request) {
        // Business logic
    }
}
```

### 3. Repository Layer

**Responsibility:** Data access, database operations

**Pattern:** Spring Data JPA Repository

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

---

## 🎨 Design Patterns

### 1. Service Layer Pattern

**Purpose:** Separate business logic from controllers

**Implementation:**
- Interface: `AuthService`
- Implementation: `AuthServiceImpl` (trong package `impl`)

### 2. Repository Pattern

**Purpose:** Abstract data access

**Implementation:**
- Spring Data JPA repositories
- Custom query methods

### 3. DTO Pattern

**Purpose:** Transfer data between layers

**Structure:**
```
dto/
├── request/    # Input DTOs
└── response/   # Output DTOs
```

### 4. Value Object Pattern

**Purpose:** Type-safe enums for status fields

**Example:**
```java
public enum SellerRequestStatus {
    PENDING("PENDING", "Đang chờ duyệt"),
    APPROVED("APPROVED", "Đã được duyệt"),
    REJECTED("REJECTED", "Đã bị từ chối");
}
```

### 5. Exception Handling Pattern

**Purpose:** Centralized error handling

**Implementation:**
- `@RestControllerAdvice`
- `GlobalExceptionHandler`

---

## 🔐 Security Architecture

### Authentication Flow

```
1. User Login
   ↓
2. Validate Credentials
   ↓
3. Generate JWT Token + Refresh Token
   ↓
4. Return Tokens to Client
   ↓
5. Client stores tokens (localStorage)
   ↓
6. Client sends JWT in Authorization header
   ↓
7. JwtAuthenticationFilter validates token
   ↓
8. Set Authentication in SecurityContext
```

### Authorization

**Role-Based Access Control (RBAC):**

- `ROLE_BUYER` - Default role
- `ROLE_SELLER` - Seller privileges
- `ROLE_ADMIN` - Admin privileges

**Implementation:**
- Spring Security `@PreAuthorize`
- Method-level security

---

## 📁 File Storage Architecture

### MinIO Integration

```
Client Upload
    ↓
FileController
    ↓
FileService
    ↓
MinIO Client
    ↓
MinIO Server (Docker)
    ↓
Bucket: ecommerce-bucket
    ├── avatars/
    ├── shop-logos/
    └── shop-covers/
```

### File Upload Flow

1. Client uploads file (multipart/form-data)
2. FileService validates (size, type)
3. Generate unique filename
4. Upload to MinIO
5. Return public URL

---

## 🔄 Data Flow

### Example: User Registration

```
1. POST /api/auth/register
   ↓
2. AuthController.register()
   ↓
3. AuthService.register()
   ├── Validate input
   ├── Check email exists
   ├── Hash password
   ├── Create User entity
   ├── Assign ROLE_BUYER
   └── Generate JWT tokens
   ↓
4. UserRepository.save()
   ↓
5. Database (PostgreSQL)
   ↓
6. Return AuthResponse
```

---

## 🧩 Module Dependencies

```
common
  ↑
auth ──┐
       │
seller ─┼──→ shop
       │
file ──┘
```

**Dependencies:**
- All modules depend on `common`
- `seller` depends on `auth` (User entity)
- `shop` depends on `auth` (User entity)
- `file` is independent

---

## 📊 Database Architecture

### Entity Relationships

```
User (1) ──→ (N) UserRole ──→ (1) Role
  │
  │ (1)
  │
  └──→ (1) Shop
        │
        │ (1)
        │
        └──→ (N) SellerRequest
```

### UUID Primary Keys

Tất cả entities sử dụng `UUID` thay vì auto-increment:
- **Lợi ích:** Unique globally, better for distributed systems
- **Trade-off:** Slightly larger storage, no sequential ordering

---

## 🚀 Frontend Architecture

### Thymeleaf Fragments

**Purpose:** Reusable components

**Structure:**
```
fragments/
├── header.html      # Header components
├── nav-*.html       # Navigation components
└── footer.html      # Footer component
```

### Layout Templates

**Purpose:** Base layouts for different page types

**Structure:**
```
layouts/
├── base.html         # General pages
├── admin-layout.html # Admin pages
└── seller-layout.html # Seller pages
```

### CSS Architecture

**Modular CSS:**
- `base.css` - Foundation
- `layout.css` - Layout
- `components.css` - Components
- Module-specific CSS (auth, admin, seller)

### JavaScript Architecture

**Modular JS:**
- `common.js` - Shared utilities
- Module-specific JS (auth, admin, seller)

---

## 🔧 Configuration

### Application Configuration

**File:** `application.properties`

**Key Configurations:**
- Database connection
- JWT settings
- MinIO settings
- File upload limits

### Security Configuration

**File:** `SecurityConfig.java`

**Features:**
- JWT authentication filter
- CORS configuration
- Public endpoints
- Role-based access control

---

## 📈 Scalability Considerations

### Current Architecture

- **Monolithic:** Single deployable unit
- **Modular:** Code organized into modules
- **Stateless:** JWT-based authentication

### Future Scalability

1. **Horizontal Scaling:** Deploy multiple instances behind load balancer
2. **Database:** Read replicas for read-heavy operations
3. **Caching:** Redis for session/cart data
4. **CDN:** For static assets (CSS, JS, images)

---

## 🔍 Code Quality

### Standards

- **Package naming:** Lowercase, no underscores
- **Class naming:** PascalCase
- **Method naming:** camelCase
- **Constants:** UPPER_SNAKE_CASE

### Best Practices

- Service interfaces for all business logic
- DTOs for data transfer
- Value objects for enums
- Exception handling centralized
- No business logic in controllers

---

## 📚 Technology Stack

### Backend
- **Java 21**
- **Spring Boot 4.0.1**
- **Spring Security**
- **Spring Data JPA**
- **PostgreSQL**
- **MinIO**

### Frontend
- **Thymeleaf**
- **HTML5/CSS3**
- **JavaScript (ES6+)**
- **No framework dependencies**

### Tools
- **Maven** - Build tool
- **Docker** - Containerization
- **Git** - Version control

---

**Cập nhật:** 11-01-2026
