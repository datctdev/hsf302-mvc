# Deployment Guide

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026

---

## 📋 Prerequisites

- **Server:** Linux (Ubuntu 20.04+ recommended)
- **Docker & Docker Compose** installed
- **Domain name** (optional, for production)
- **SSL Certificate** (for HTTPS)

---

## 🚀 Deployment Steps

### 1. Prepare Server

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. Clone Repository

```bash
git clone <repository-url>
cd e-comerce
```

### 3. Configure Environment

Tạo file `.env` cho production:

```env
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ecommerce_prod
DB_USERNAME=ecommerce_user
DB_PASSWORD=<strong-password>

# JWT
JWT_SECRET=<generate-strong-secret-256-bits>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# MinIO
MINIO_URL=http://minio:9000
MINIO_ACCESS_KEY=<strong-access-key>
MINIO_SECRET_KEY=<strong-secret-key>
MINIO_BUCKET_NAME=ecommerce-prod-bucket

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

**⚠️ Lưu ý:**
- Generate strong passwords và secrets
- Không commit `.env` vào git
- Sử dụng secrets management (AWS Secrets Manager, HashiCorp Vault) cho production

### 4. Build Application

```bash
# Build JAR
mvn clean package -DskipTests

# JAR file sẽ ở: target/e-comerce-0.0.1-SNAPSHOT.jar
```

### 5. Update Docker Compose

Tạo `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ecommerce-postgres-prod
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network
    restart: unless-stopped

  minio:
    image: minio/minio:latest
    container_name: ecommerce-minio-prod
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
    restart: unless-stopped

  app:
    build: .
    container_name: ecommerce-app-prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      MINIO_URL: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
      MINIO_BUCKET_NAME: ${MINIO_BUCKET_NAME}
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - minio
    networks:
      - ecommerce-network
    restart: unless-stopped

volumes:
  postgres_data:
  minio_data:

networks:
  ecommerce-network:
    driver: bridge
```

Tạo `Dockerfile`:

```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/e-comerce-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6. Deploy

```bash
# Start services
docker compose -f docker-compose.prod.yml up -d

# Check logs
docker compose -f docker-compose.prod.yml logs -f app

# Check status
docker compose -f docker-compose.prod.yml ps
```

---

## 🔒 SSL/HTTPS Setup

### Option 1: Nginx Reverse Proxy với Let's Encrypt

```nginx
# /etc/nginx/sites-available/ecommerce
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        return 301 https://$server_name$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Option 2: Cloudflare (Recommended)

1. Add domain to Cloudflare
2. Enable SSL/TLS (Full mode)
3. Configure DNS records

---

## 📊 Monitoring

### Health Check

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database
docker exec -it ecommerce-postgres-prod pg_isready

# MinIO
curl http://localhost:9000/minio/health/live
```

### Logs

```bash
# Application logs
docker compose -f docker-compose.prod.yml logs -f app

# Database logs
docker compose -f docker-compose.prod.yml logs -f postgres

# MinIO logs
docker compose -f docker-compose.prod.yml logs -f minio
```

### Backup

#### Database Backup

```bash
# Backup
docker exec ecommerce-postgres-prod pg_dump -U ${DB_USERNAME} ${DB_NAME} > backup.sql

# Restore
docker exec -i ecommerce-postgres-prod psql -U ${DB_USERNAME} ${DB_NAME} < backup.sql
```

#### MinIO Backup

```bash
# Backup MinIO data
docker run --rm -v ecommerce_minio_data:/data -v $(pwd):/backup alpine tar czf /backup/minio-backup.tar.gz /data
```

---

## 🔄 Updates

### Update Application

```bash
# Pull latest code
git pull

# Rebuild
mvn clean package -DskipTests

# Restart
docker compose -f docker-compose.prod.yml restart app
```

### Database Migration

Application tự động migrate khi start (Hibernate `ddl-auto=update`).

⚠️ **Production:** Nên sử dụng Flyway hoặc Liquibase cho migration.

---

## 🛡️ Security Checklist

- [ ] Strong passwords cho database và MinIO
- [ ] JWT secret đủ mạnh (256+ bits)
- [ ] SSL/HTTPS enabled
- [ ] Firewall configured (chỉ mở ports cần thiết)
- [ ] Regular backups
- [ ] Remove default accounts (nếu có)
- [ ] Rate limiting (sẽ implement)
- [ ] CORS configured properly
- [ ] Security headers (CSP, X-Frame-Options, etc.)

---

## 📝 Environment Variables

### Production

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate  # Không dùng update trong production
```

---

## 🆘 Troubleshooting

### Application không start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs app

# Check environment variables
docker exec ecommerce-app-prod env
```

### Database connection issues

```bash
# Test connection
docker exec -it ecommerce-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME}
```

### MinIO issues

```bash
# Check MinIO status
docker compose -f docker-compose.prod.yml ps minio

# Check bucket
docker exec ecommerce-minio-prod mc ls minio/
```

---

## 📚 Additional Resources

- Docker Documentation: https://docs.docker.com
- Spring Boot Production: https://spring.io/guides/gs/spring-boot-for-azure/
- Nginx Configuration: https://nginx.org/en/docs/

---

**Cập nhật:** 11-01-2026
