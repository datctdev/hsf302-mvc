# Development Setup Guide

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026

---

## 📋 Prerequisites

### Required:
- **Java 21+** (JDK)
- **Maven 3.8+**
- **Docker & Docker Compose** (cho PostgreSQL và MinIO)
- **Git**

### Optional:
- **IDE:** IntelliJ IDEA, Eclipse, VS Code
- **Postman/Insomnia** (để test API)

---

## 🚀 Quick Start

### 1. Clone Repository

```bash
git clone <repository-url>
cd e-comerce
```

### 2. Setup Environment Variables

Copy `env.example` và tạo file `.env`:

```bash
cp env.example .env
```

Chỉnh sửa `.env` với các giá trị phù hợp:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ecommerce
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key-min-256-bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# MinIO
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=ecommerce-bucket
```

### 3. Start Docker Services

```bash
docker compose up -d
```

Services sẽ được start:
- **PostgreSQL** (port 5432)
- **MinIO** (port 9000)
- **MinIO Init** (tự động tạo bucket)

Kiểm tra services:

```bash
docker compose ps
```

### 4. Build và Run Application

#### Option 1: Maven

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

#### Option 2: IDE

1. Import project vào IDE
2. Run `EComerceApplication.java`

### 5. Verify Setup

- **Application:** http://localhost:8080
- **MinIO Console:** http://localhost:9001 (minioadmin/minioadmin)
- **Database:** localhost:5432

---

## 🗄️ Database Setup

### Automatic (Recommended)

Database sẽ tự động được tạo khi chạy Docker Compose. Application sẽ tự động tạo schema khi start.

### Manual

Nếu cần setup manual:

```bash
# Connect to PostgreSQL
docker exec -it app-postgres psql -U postgres

# Create database
CREATE DATABASE ecommerce;

# Exit
\q
```

---

## 📦 Project Structure

```
e-comerce/
├── src/main/java/com/hsf/e_comerce/
│   ├── auth/              # Authentication module
│   ├── seller/            # Seller module
│   ├── shop/              # Shop module
│   ├── file/              # File upload module
│   ├── common/            # Common utilities
│   └── config/            # Configuration
├── src/main/resources/
│   ├── templates/         # Thymeleaf templates
│   │   ├── fragments/     # Reusable fragments
│   │   ├── layouts/       # Layout templates
│   │   ├── admin/         # Admin pages
│   │   ├── auth/          # Auth pages
│   │   └── seller/        # Seller pages
│   ├── static/            # Static resources
│   │   ├── css/           # Stylesheets
│   │   └── js/             # JavaScript files
│   └── application.properties
└── docker-compose.yml
```

---

## 🔧 Configuration

### Application Properties

File: `src/main/resources/application.properties`

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:ecommerce}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800000}

# MinIO
minio.url=${MINIO_URL:http://localhost:9000}
minio.access-key=${MINIO_ACCESS_KEY:minioadmin}
minio.secret-key=${MINIO_SECRET_KEY:minioadmin}
minio.bucket-name=${MINIO_BUCKET_NAME:ecommerce-bucket}

# File Upload
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=25MB
```

---

## 🧪 Testing

### Default Accounts

Sau khi start application, 3 tài khoản mặc định sẽ được tạo:

| Role | Email | Password |
|------|-------|----------|
| BUYER | buyer@gmail.com | buyer123@ |
| SELLER | seller@gmail.com | seller123@ |
| ADMIN | admin@gmail.com | admin123@ |

Xem chi tiết: `DEFAULT_ACCOUNTS.md`

### Test API

Sử dụng Postman hoặc curl:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer@gmail.com","password":"buyer123@"}'

# Get user info (với token)
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer {token}"
```

---

## 🐛 Troubleshooting

### Port Already in Use

```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Docker Issues

```bash
# Restart services
docker compose down
docker compose up -d

# Check logs
docker compose logs app-postgres
docker compose logs app-minio
```

### Database Connection Error

1. Kiểm tra PostgreSQL đang chạy: `docker compose ps`
2. Kiểm tra credentials trong `.env`
3. Kiểm tra port 5432 không bị block

### MinIO Issues

1. Kiểm tra MinIO đang chạy: `docker compose ps`
2. Kiểm tra bucket đã được tạo: http://localhost:9001
3. Kiểm tra credentials trong `.env`

---

## 📚 Next Steps

1. Đọc `API_DOCUMENTATION.md` để hiểu API endpoints
2. Đọc `FRONTEND_GUIDE.md` để hiểu frontend architecture
3. Đọc `ARCHITECTURE_DOCUMENTATION.md` để hiểu code structure

---

## 🔗 Useful Links

- **Application:** http://localhost:8080
- **MinIO Console:** http://localhost:9001
- **API Docs:** Xem `API_DOCUMENTATION.md`

---

**Cập nhật:** 11-01-2026
