# Phân Tích Chức Năng: Xem Sản Phẩm Công Khai

**Ngày:** 14-01-2026  
**Mục tiêu:** Cho phép người mua và người không đăng nhập xem được sản phẩm

---

## 📋 Tổng Quan

### Yêu Cầu
- ✅ **Người không đăng nhập** có thể xem danh sách sản phẩm
- ✅ **Người không đăng nhập** có thể xem chi tiết sản phẩm
- ✅ **Người mua (đã đăng nhập)** có thể xem danh sách và chi tiết sản phẩm
- ✅ Hỗ trợ tìm kiếm, lọc, phân trang

### Trạng Thái Hiện Tại
- ❌ **Chưa có controller public** cho xem sản phẩm
- ✅ Đã có `ProductService` với method `getProductById()`
- ✅ Đã có entities và repositories
- ❌ Chưa có endpoint `/api/products` (public)
- ❌ Chưa có frontend pages cho buyer

---

## 🏗️ Kiến Trúc Đề Xuất

### 1. Backend API Endpoints

#### 1.1. Public Product Controller
**File:** `src/main/java/com/hsf/e_comerce/product/controller/ProductController.java`

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    // GET /api/products - Danh sách sản phẩm (public, có phân trang)
    // GET /api/products/{id} - Chi tiết sản phẩm (public)
    // GET /api/products/search - Tìm kiếm sản phẩm (public)
    // GET /api/products/categories - Danh sách categories (public)
}
```

**Endpoints chi tiết:**

| Method | Endpoint | Mô tả | Quyền truy cập |
|--------|----------|-------|----------------|
| GET | `/api/products` | Danh sách sản phẩm (phân trang, lọc) | Public |
| GET | `/api/products/{id}` | Chi tiết sản phẩm | Public |
| GET | `/api/products/search` | Tìm kiếm sản phẩm | Public |
| GET | `/api/products/categories` | Danh sách categories | Public |
| GET | `/api/products/shops/{shopId}` | Sản phẩm theo shop | Public |

#### 1.2. Query Parameters

**GET /api/products:**
```
?page=0                    // Số trang (default: 0)
&size=20                   // Số sản phẩm/trang (default: 20)
&status=PUBLISHED          // Lọc theo trạng thái (chỉ hiển thị PUBLISHED)
&categoryId=xxx            // Lọc theo category
&shopId=xxx                // Lọc theo shop
&minPrice=100000           // Giá tối thiểu
&maxPrice=500000           // Giá tối đa
&sort=price,asc            // Sắp xếp (price, createdAt, name)
&search=keyword            // Tìm kiếm theo tên/mô tả
```

**GET /api/products/search:**
```
?q=keyword                 // Từ khóa tìm kiếm
&categoryId=xxx            // Lọc theo category
&minPrice=xxx              // Giá tối thiểu
&maxPrice=xxx              // Giá tối đa
&page=0                    // Phân trang
&size=20                   // Số kết quả/trang
```

### 2. Service Layer

#### 2.1. Thêm Methods vào ProductService

```java
public interface ProductService {
    // ... existing methods ...
    
    // Public methods
    Page<ProductResponse> getPublishedProducts(
        int page, 
        int size, 
        String search, 
        UUID categoryId, 
        UUID shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        String sortDir
    );
    
    ProductResponse getPublishedProductById(UUID productId);
    
    List<ProductResponse> searchProducts(String keyword, int page, int size);
    
    List<ProductCategoryResponse> getAllCategories();
}
```

#### 2.2. Business Rules

**Quy tắc hiển thị:**
1. ✅ Chỉ hiển thị sản phẩm có `status = PUBLISHED`
2. ✅ Không hiển thị sản phẩm của shop bị vô hiệu hóa
3. ✅ Không hiển thị sản phẩm của seller bị vô hiệu hóa
4. ✅ Sắp xếp mặc định: `createdAt DESC` (mới nhất trước)

**Quy tắc tìm kiếm:**
- Tìm trong: `name`, `description`, `sku`
- Không phân biệt hoa thường
- Hỗ trợ tìm kiếm tiếng Việt (có dấu/không dấu)

### 3. Repository Layer

#### 3.1. Thêm Methods vào ProductRepository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // ... existing methods ...
    
    // Public query methods
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.isActive = true AND p.shop.user.isActive = true")
    Page<Product> findPublishedProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.isActive = true AND p.shop.user.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchPublishedProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.isActive = true AND p.shop.user.isActive = true " +
           "AND (:categoryId IS NULL OR EXISTS " +
           "(SELECT 1 FROM ProductCategoryMapping m WHERE m.product = p AND m.category.id = :categoryId)) " +
           "AND (:shopId IS NULL OR p.shop.id = :shopId) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)")
    Page<Product> findPublishedProductsWithFilters(
        @Param("categoryId") UUID categoryId,
        @Param("shopId") UUID shopId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    Optional<Product> findByIdAndStatus(UUID id, ProductStatus status);
}
```

### 4. Security Configuration

#### 4.1. Cập nhật SecurityConfig

```java
.authorizeHttpRequests(auth -> auth
    // ... existing rules ...
    
    // Public product endpoints
    .requestMatchers("/api/products/**").permitAll()
    
    // ... other rules ...
)
```

### 5. Frontend Pages

#### 5.1. Danh Sách Sản Phẩm
**File:** `src/main/resources/templates/products.html`

**Tính năng:**
- Hiển thị grid/list sản phẩm
- Phân trang
- Sidebar filters:
  - Category
  - Price range (slider)
  - Shop
- Search bar
- Sort options (Giá, Mới nhất, Bán chạy)

#### 5.2. Chi Tiết Sản Phẩm
**File:** `src/main/resources/templates/products/detail.html`

**Tính năng:**
- Hiển thị ảnh sản phẩm (gallery)
- Thông tin cơ bản (tên, giá, mô tả)
- Variants selector (nếu có)
- Nút "Thêm vào giỏ hàng" (chỉ hiển thị khi đã đăng nhập)
- Thông tin shop
- Sản phẩm liên quan

#### 5.3. Home Page Integration
**File:** `src/main/resources/templates/home.html`

**Tính năng:**
- Hiển thị sản phẩm nổi bật
- Hiển thị sản phẩm mới nhất
- Categories carousel

---

## 📊 Data Flow

### 1. Xem Danh Sách Sản Phẩm

```
User (Anonymous/Logged in)
    ↓
GET /api/products?page=0&size=20&categoryId=xxx
    ↓
ProductController.getProducts()
    ↓
ProductService.getPublishedProducts()
    ↓
ProductRepository.findPublishedProductsWithFilters()
    ↓
Filter: status=PUBLISHED, shop.active=true, user.active=true
    ↓
Page<ProductResponse>
    ↓
Frontend: Render product cards
```

### 2. Xem Chi Tiết Sản Phẩm

```
User (Anonymous/Logged in)
    ↓
GET /api/products/{id}
    ↓
ProductController.getProductById()
    ↓
ProductService.getPublishedProductById()
    ↓
ProductRepository.findByIdAndStatus(id, PUBLISHED)
    ↓
Check: shop.active && user.active
    ↓
ProductResponse (với variants, images, category)
    ↓
Frontend: Render product detail page
```

### 3. Tìm Kiếm Sản Phẩm

```
User (Anonymous/Logged in)
    ↓
GET /api/products/search?q=keyword&page=0
    ↓
ProductController.searchProducts()
    ↓
ProductService.searchProducts()
    ↓
ProductRepository.searchPublishedProducts()
    ↓
Full-text search trong name, description
    ↓
Page<ProductResponse>
    ↓
Frontend: Render search results
```

---

## 🔒 Security & Privacy

### 1. Quyền Truy Cập

| Endpoint | Anonymous | Buyer | Seller | Admin |
|----------|-----------|-------|--------|-------|
| GET /api/products | ✅ | ✅ | ✅ | ✅ |
| GET /api/products/{id} | ✅ | ✅ | ✅ | ✅ |
| GET /api/products/search | ✅ | ✅ | ✅ | ✅ |
| GET /api/products/categories | ✅ | ✅ | ✅ | ✅ |

### 2. Data Filtering

**Chỉ hiển thị:**
- ✅ Sản phẩm có `status = PUBLISHED`
- ✅ Sản phẩm của shop đang active
- ✅ Sản phẩm của seller đang active

**Không hiển thị:**
- ❌ Sản phẩm DRAFT
- ❌ Sản phẩm ARCHIVED
- ❌ Sản phẩm của shop bị vô hiệu hóa
- ❌ Sản phẩm của seller bị vô hiệu hóa

### 3. Rate Limiting (Tùy chọn)

- Giới hạn số request/giây cho anonymous users
- Giới hạn số request/giây cho search endpoint

---

## 🎨 UI/UX Design

### 1. Product List Page

**Layout:**
```
┌─────────────────────────────────────────┐
│  Header (Logo, Search, Cart, Login)      │
├─────────────────────────────────────────┤
│  Breadcrumb: Trang chủ > Sản phẩm       │
├──────────┬──────────────────────────────┤
│          │  Sort: [Mới nhất ▼]          │
│ Filters  │  ┌─────┬─────┬─────┐         │
│          │  │ P1  │ P2  │ P3  │         │
│ Category │  ├─────┼─────┼─────┤         │
│ - All    │  │ P4  │ P5  │ P6  │         │
│ - Cat 1  │  ├─────┼─────┼─────┤         │
│ - Cat 2  │  │ P7  │ P8  │ P9  │         │
│          │  └─────┴─────┴─────┘         │
│ Price    │  [< 1] [2] [3] [4] [>]       │
│ 0 - 1M   │                               │
│          │                               │
│ Shop     │                               │
│ - All    │                               │
│ - Shop 1 │                               │
└──────────┴──────────────────────────────┘
```

### 2. Product Detail Page

**Layout:**
```
┌─────────────────────────────────────────┐
│  Header                                 │
├─────────────────────────────────────────┤
│  Breadcrumb: Trang chủ > Cat > Product  │
├──────────┬──────────────────────────────┤
│          │  Product Name                │
│ Image    │  Price: 200,000 VNĐ          │
│ Gallery  │                              │
│ [Main]   │  Variants:                   │
│ [Thumb]  │  - Color: [Red] [Blue]       │
│ [Thumb]  │  - Size: [M] [L] [XL]        │
│          │                              │
│          │  Stock: 10                   │
│          │  [Thêm vào giỏ hàng]         │
│          │                              │
│          │  Description:                │
│          │  ...                         │
│          │                              │
│          │  Shop Info:                  │
│          │  [Shop Name] [View Shop]     │
└──────────┴──────────────────────────────┘
```

---

## 📝 Implementation Checklist

### Backend
- [ ] Tạo `ProductController` (public endpoints)
- [ ] Thêm methods vào `ProductService`
- [ ] Thêm query methods vào `ProductRepository`
- [ ] Cập nhật `SecurityConfig` để permit `/api/products/**`
- [ ] Tạo DTO cho search/filter requests
- [ ] Implement pagination
- [ ] Implement search với full-text
- [ ] Implement filtering (category, price, shop)
- [ ] Implement sorting
- [ ] Add validation và error handling

### Frontend
- [ ] Tạo `products.html` (danh sách)
- [ ] Tạo `products/detail.html` (chi tiết)
- [ ] Tạo `products/search.html` (kết quả tìm kiếm)
- [ ] Implement product card component
- [ ] Implement filter sidebar
- [ ] Implement pagination UI
- [ ] Implement search bar
- [ ] Implement image gallery
- [ ] Implement variant selector
- [ ] Add responsive design
- [ ] Add loading states
- [ ] Add error handling

### Testing
- [ ] Test anonymous user có thể xem sản phẩm
- [ ] Test buyer có thể xem sản phẩm
- [ ] Test chỉ hiển thị PUBLISHED products
- [ ] Test search functionality
- [ ] Test filtering
- [ ] Test pagination
- [ ] Test performance với large dataset

---

## 🚀 Performance Considerations

### 1. Database Indexing

**Cần tạo indexes:**
```sql
CREATE INDEX idx_product_status ON products(status);
CREATE INDEX idx_product_shop_active ON products(shop_id) WHERE shop_id IN (SELECT id FROM shops WHERE is_active = true);
CREATE INDEX idx_product_name_search ON products USING gin(to_tsvector('vietnamese', name));
CREATE INDEX idx_product_price ON products(base_price);
```

### 2. Caching

**Có thể cache:**
- Danh sách categories (1 hour)
- Sản phẩm nổi bật (30 minutes)
- Sản phẩm mới nhất (15 minutes)

### 3. Pagination

- Default: 20 sản phẩm/trang
- Max: 100 sản phẩm/trang
- Sử dụng cursor-based pagination cho large datasets

---

## 📚 API Examples

### 1. Get Products List

```http
GET /api/products?page=0&size=20&status=PUBLISHED&categoryId=xxx&sort=price,asc
```

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Áo thun",
      "sku": "SHIRT-001",
      "basePrice": 200000,
      "status": "PUBLISHED",
      "images": [...],
      "variants": [...],
      "shopName": "Shop ABC"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

### 2. Get Product Detail

```http
GET /api/products/a2ffd0a3-7fd0-4378-bbfb-de24d8cd5a7c
```

**Response:**
```json
{
  "id": "uuid",
  "name": "Áo thun",
  "description": "Mô tả sản phẩm...",
  "sku": "SHIRT-001",
  "basePrice": 200000,
  "status": "PUBLISHED",
  "images": [
    {
      "id": "uuid",
      "imageUrl": "https://...",
      "isThumbnail": true,
      "displayOrder": 0
    }
  ],
  "variants": [
    {
      "id": "uuid",
      "name": "Color",
      "value": "Red",
      "priceModifier": 0,
      "stockQuantity": 10,
      "sku": "SHIRT-001-RED"
    }
  ],
  "category": {
    "id": "uuid",
    "name": "Quần áo"
  },
  "shopId": "uuid",
  "shopName": "Shop ABC"
}
```

### 3. Search Products

```http
GET /api/products/search?q=áo thun&page=0&size=20
```

**Response:** (tương tự như Get Products List)

---

## 🎯 Next Steps

1. **Phase 1:** Implement basic public viewing (list + detail)
2. **Phase 2:** Add search functionality
3. **Phase 3:** Add filtering và sorting
4. **Phase 4:** Optimize performance (indexing, caching)
5. **Phase 5:** Add advanced features (related products, recommendations)

---

## 📌 Notes

- Tất cả endpoints public đều không cần authentication
- Chỉ hiển thị sản phẩm PUBLISHED
- Cần validate input parameters (page, size, price range)
- Cần handle edge cases (empty results, invalid IDs)
- Cần implement proper error messages (Vietnamese)
