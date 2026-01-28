package com.hsf.e_comerce.product.service.impl;

import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.product.controller.ProductMvcController;
import com.hsf.e_comerce.product.dto.request.CreateProductRequest;
import com.hsf.e_comerce.product.dto.request.ProductImageRequest;
import com.hsf.e_comerce.product.dto.request.ProductVariantRequest;
import com.hsf.e_comerce.product.dto.request.UpdateProductRequest;
import com.hsf.e_comerce.product.dto.response.CategoryResponse;
import com.hsf.e_comerce.product.dto.response.ProductResponse;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.product.repository.ProductRepository;
import com.hsf.e_comerce.product.repository.ProductVariantRepository;
import com.hsf.e_comerce.product.service.ProductService;
import com.hsf.e_comerce.product.valueobject.ProductStatus;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(UUID shopId, CreateProductRequest request) {
        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new CustomException("Shop không tồn tại"));

        // Validate SKU uniqueness
        if (productRepository.existsBySkuAndDeletedFalse(request.getSku())) {
            throw new CustomException("SKU đã tồn tại: " + request.getSku());
        }

        // Create product
        Product product = new Product();
        product.setShop(shop);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setBasePrice(request.getBasePrice());
        product.setStatus(ProductStatus.valueOf(request.getStatus().toUpperCase()));

        // Set category (N-1: 1 product có 0 hoặc 1 category)
        if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomException("Danh mục không tồn tại"));
            product.setCategory(category);
        }

        product = productRepository.save(product);

        // Create variants
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (ProductVariantRequest variantRequest : request.getVariants()) {
                // Validate variant SKU uniqueness
                if (variantRepository.existsBySku(variantRequest.getSku())) {
                    throw new CustomException("Variant SKU đã tồn tại: " + variantRequest.getSku());
                }

                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setName(variantRequest.getName());
                variant.setValue(variantRequest.getValue());
                variant.setPriceModifier(variantRequest.getPriceModifier());
                variant.setStockQuantity(variantRequest.getStockQuantity());
                variant.setSku(variantRequest.getSku());
                variantRepository.save(variant);
            }
        } else {
            // Auto-create default variant if no variants provided
            // This ensures every product has at least one variant for stock management
            String defaultVariantSku = product.getSku() + "-DEFAULT";
            
            // Check if default SKU already exists (unlikely but possible)
            int suffix = 1;
            while (variantRepository.existsBySku(defaultVariantSku)) {
                defaultVariantSku = product.getSku() + "-DEFAULT-" + suffix;
                suffix++;
            }
            
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(product);
            defaultVariant.setName("Mặc định");
            defaultVariant.setValue("Tiêu chuẩn");
            defaultVariant.setPriceModifier(BigDecimal.ZERO);
            defaultVariant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
            defaultVariant.setSku(defaultVariantSku);
            variantRepository.save(defaultVariant);
        }

        // Create images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            boolean hasThumbnail = false;
            for (ProductImageRequest imageRequest : request.getImages()) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageUrl(imageRequest.getImageUrl());
                image.setIsThumbnail(imageRequest.getIsThumbnail());
                image.setDisplayOrder(imageRequest.getDisplayOrder());
                
                if (imageRequest.getIsThumbnail()) {
                    if (hasThumbnail) {
                        throw new CustomException("Chỉ được có một ảnh thumbnail");
                    }
                    hasThumbnail = true;
                }
                
                imageRepository.save(image);
            }
        }

        return ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        );
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID shopId, UUID productId, UpdateProductRequest request) {
        // Validate product belongs to shop
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại"));

        if (!product.getShop().getId().equals(shopId)) {
            throw new CustomException("Bạn không có quyền chỉnh sửa sản phẩm này");
        }

        // Update basic fields
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySkuAndDeletedFalse(request.getSku())) {
                throw new CustomException("SKU đã tồn tại: " + request.getSku());
            }
            product.setSku(request.getSku());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getStatus() != null) {
            product.setStatus(ProductStatus.valueOf(request.getStatus().toUpperCase()));
        }

        product = productRepository.save(product);

        // Update variants - smart update: update existing, create new, delete removed
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> existingVariants = variantRepository.findByProduct(product);
            List<UUID> requestVariantIds = request.getVariants().stream()
                    .map(ProductVariantRequest::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            
            // Delete variants that are not in the request
            existingVariants.stream()
                    .filter(v -> !requestVariantIds.contains(v.getId()))
                    .forEach(variantRepository::delete);
            
            // Update or create variants
            for (ProductVariantRequest variantRequest : request.getVariants()) {
                ProductVariant variant;
                
                if (variantRequest.getId() != null) {
                    // Update existing variant
                    variant = variantRepository.findById(variantRequest.getId())
                            .orElseThrow(() -> new CustomException("Variant không tồn tại với ID: " + variantRequest.getId()));
                    
                    // Validate SKU uniqueness only if SKU changed
                    if (!variant.getSku().equals(variantRequest.getSku())) {
                        if (variantRepository.existsBySku(variantRequest.getSku())) {
                            throw new CustomException("Variant SKU đã tồn tại: " + variantRequest.getSku());
                        }
                    }
                } else {
                    // Create new variant
                    if (variantRepository.existsBySku(variantRequest.getSku())) {
                        throw new CustomException("Variant SKU đã tồn tại: " + variantRequest.getSku());
                    }
                    variant = new ProductVariant();
                }

                variant.setProduct(product);
                variant.setName(variantRequest.getName());
                variant.setValue(variantRequest.getValue());
                variant.setPriceModifier(variantRequest.getPriceModifier());
                variant.setStockQuantity(variantRequest.getStockQuantity());
                variant.setSku(variantRequest.getSku());
                variantRepository.save(variant);
            }
        } else if (request.getVariants() != null && request.getVariants().isEmpty()) {
            // If variants is explicitly empty list, delete all and create default variant
            variantRepository.findByProduct(product).forEach(variantRepository::delete);
            
            // Create default variant
            String defaultVariantSku = product.getSku() + "-DEFAULT";
            int suffix = 1;
            while (variantRepository.existsBySku(defaultVariantSku)) {
                defaultVariantSku = product.getSku() + "-DEFAULT-" + suffix;
                suffix++;
            }
            
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(product);
            defaultVariant.setName("Mặc định");
            defaultVariant.setValue("Tiêu chuẩn");
            defaultVariant.setPriceModifier(BigDecimal.ZERO);
            defaultVariant.setStockQuantity(0); // Default to 0 when updating
            defaultVariant.setSku(defaultVariantSku);
            variantRepository.save(defaultVariant);
        }
        // If variants is null, don't change existing variants (partial update)

        // Update images - smart update: update existing, create new, delete removed
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<ProductImage> existingImages = imageRepository.findByProductOrderByDisplayOrderAsc(product);
            List<UUID> requestImageIds = request.getImages().stream()
                    .map(ProductImageRequest::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            
            // Delete images that are not in the request
            existingImages.stream()
                    .filter(img -> !requestImageIds.contains(img.getId()))
                    .forEach(imageRepository::delete);
            
            boolean hasThumbnail = false;
            for (ProductImageRequest imageRequest : request.getImages()) {
                ProductImage image;
                
                if (imageRequest.getId() != null) {
                    // Update existing image
                    image = imageRepository.findById(imageRequest.getId())
                            .orElseThrow(() -> new CustomException("Image không tồn tại với ID: " + imageRequest.getId()));
                } else {
                    // Create new image
                    image = new ProductImage();
                }

                image.setProduct(product);
                image.setImageUrl(imageRequest.getImageUrl());
                image.setIsThumbnail(imageRequest.getIsThumbnail());
                image.setDisplayOrder(imageRequest.getDisplayOrder());
                
                if (imageRequest.getIsThumbnail()) {
                    if (hasThumbnail) {
                        throw new CustomException("Chỉ được có một ảnh thumbnail");
                    }
                    hasThumbnail = true;
                }
                
                imageRepository.save(image);
            }
        }
        // If images is null or empty, keep existing images (don't delete)

        // Update category (N-1: 1 product có 0 hoặc 1 category)
        if (Boolean.TRUE.equals(request.getClearCategory())) {
            product.setCategory(null);
            product = productRepository.save(product);
        } else if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomException("Danh mục không tồn tại"));
            product.setCategory(category);
            product = productRepository.save(product);
        }

        return ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        );
    }

    @Override
    @Transactional
    public void deleteProduct(UUID shopId, UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại"));

        if (!product.getShop().getId().equals(shopId)) {
            throw new CustomException("Bạn không có quyền xóa sản phẩm này");
        }

        // Soft delete: set deleted flag to true
        product.setDeleted(true);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID productId) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại"));

        return ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsByShop(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new CustomException("Shop không tồn tại"));

        return productRepository.findByShopAndDeletedFalse(shop).stream()
                .map(product -> ProductResponse.convertToResponse(
                        product,
                        variantRepository,
                        imageRepository
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByShopAndStatus(UUID shopId, String status) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new CustomException("Shop không tồn tại"));

        ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());

        return productRepository.findByShopAndStatusAndDeletedFalse(shop, productStatus).stream()
                .map(product -> ProductResponse.convertToResponse(
                        product,
                        variantRepository,
                        imageRepository
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublishedProducts(
            int page,
            int size,
            String search,
            UUID categoryId,
            UUID shopId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDir) {
        
        // Validate pagination
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        
        // Create sort
        Sort sort = createSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Use filter query if any filter is provided
        if (search != null && !search.trim().isEmpty() ||
            categoryId != null ||
            shopId != null ||
            minPrice != null ||
            maxPrice != null) {
            
            Page<Product> products = productRepository.findPublishedProductsWithFilters(
                    categoryId,
                    shopId,
                    minPrice,
                    maxPrice,
                    search != null ? search.trim() : null,
                    pageable
            );
            
            return products.map(product -> ProductResponse.convertToResponse(
                    product,
                    variantRepository,
                    imageRepository
            ));
        }
        
        // Otherwise use simple query
        Page<Product> products = productRepository.findPublishedProducts(pageable);
        
        return products.map(product -> ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublishedProductById(UUID productId) {
        Product product = productRepository.findPublishedById(productId)
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại hoặc chưa được xuất bản"));
        
        return ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublishedProducts(page, size, null, null, null, null, null, null, null);
        }
        
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.searchPublishedProducts(keyword.trim(), pageable);
        
        return products.map(product -> ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublishedProductsByShop(UUID shopId, int page, int size) {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findPublishedProductsByShop(shopId, pageable);
        
        return products.map(product -> ProductResponse.convertToResponse(
                product,
                variantRepository,
                imageRepository
        ));
    }

    @Override
    public List<CategoryResponse> findAllCategory() {
        return categoryRepository.findAll().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .parentId(category.getParent() != null ? category.getParent().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllForAdmin(UUID shopId, ProductStatus status, Pageable pageable) {
        return productRepository.findAllForAdmin(shopId, status, pageable);
    }

    @Override
    @Transactional
    public void setProductStatus(UUID productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.hsf.e_comerce.common.exception.CustomException("Không tìm thấy sản phẩm."));
        product.setStatus(status);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void setProductDeleted(UUID productId, boolean deleted) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.hsf.e_comerce.common.exception.CustomException("Không tìm thấy sản phẩm."));
        product.setDeleted(deleted);
        productRepository.save(product);
    }

    private Sort createSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        // Validate sort field
        String field = sortBy.toLowerCase();
        switch (field) {
            case "price":
                return Sort.by(direction, "basePrice");
            case "name":
                return Sort.by(direction, "name");
            case "created":
            case "createdat":
                return Sort.by(direction, "createdAt");
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}
