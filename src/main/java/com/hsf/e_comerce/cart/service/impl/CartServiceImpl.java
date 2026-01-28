package com.hsf.e_comerce.cart.service.impl;

import com.hsf.e_comerce.cart.dto.request.AddToCartRequest;
import com.hsf.e_comerce.cart.dto.request.UpdateCartItemRequest;
import com.hsf.e_comerce.cart.dto.response.CartItemResponse;
import com.hsf.e_comerce.cart.dto.response.CartResponse;
import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartItemRepository;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.cart.service.CartService;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.product.repository.ProductRepository;
import com.hsf.e_comerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> getCartWithItemsAndProducts(User user) {
        return cartRepository.findByUserIdWithItemsAndProducts(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUser(User user) {
        Cart cart = getOrCreateCart(user);
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(User user, AddToCartRequest request) {
        // Validate product exists and is published
        Product product = productRepository.findPublishedById(request.getProductId())
                .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại hoặc chưa được xuất bản."));

        // Validate and get variant
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            // User selected a specific variant
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new CustomException("Biến thể sản phẩm không tồn tại."));
            
            // Verify variant belongs to product
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new CustomException("Biến thể không thuộc về sản phẩm này.");
            }
            
            // Check stock
            if (variant.getStockQuantity() < request.getQuantity()) {
                throw new CustomException("Số lượng sản phẩm không đủ. Còn lại: " + variant.getStockQuantity());
            }
        } else {
            // No variant selected - find default variant or single variant
            List<ProductVariant> productVariants = productVariantRepository.findByProduct(product);
            
            if (productVariants == null || productVariants.isEmpty()) {
                // Product has no variants (should not happen with auto-creation, but handle it)
                throw new CustomException("Sản phẩm chưa có biến thể. Vui lòng liên hệ shop.");
            } else if (productVariants.size() == 1) {
                // Only one variant (likely default variant) - use it automatically
                variant = productVariants.get(0);
            } else {
                // Multiple variants - try to find default variant
                variant = productVariants.stream()
                        .filter(v -> "Mặc định".equals(v.getName()) && "Tiêu chuẩn".equals(v.getValue()))
                        .findFirst()
                        .orElse(null);
                
                // If no default variant found and multiple variants exist, user must select one
                if (variant == null) {
                    throw new CustomException("Sản phẩm có nhiều biến thể. Vui lòng chọn biến thể sản phẩm.");
                }
            }
            
            // Check stock for auto-selected variant
            if (variant.getStockQuantity() < request.getQuantity()) {
                throw new CustomException("Số lượng sản phẩm không đủ. Còn lại: " + variant.getStockQuantity());
            }
        }

        // Get or create cart
        Cart cart = getOrCreateCart(user);

        // Calculate unit price (variant is guaranteed to be not null after validation above)
        BigDecimal unitPrice = product.getBasePrice();
        unitPrice = unitPrice.add(variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO);

        // Check if item already exists in cart (variant is guaranteed to be not null)
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProductAndVariant(cart, product, variant);

        CartItem cartItem;
        if (existingItem.isPresent()) {
            // Update quantity
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            
            // Check stock again (variant is guaranteed to be not null)
            if (variant.getStockQuantity() < newQuantity) {
                throw new CustomException("Số lượng sản phẩm không đủ. Còn lại: " + variant.getStockQuantity());
            }
            
            cartItem.setQuantity(newQuantity);
        } else {
            // Create new cart item
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setUnitPrice(unitPrice);
            cart.getItems().add(cartItem);
        }

        cartItemRepository.save(cartItem);
        cartRepository.save(cart);

        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(User user, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(user);
        
        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new CustomException("Mục giỏ hàng không tồn tại."));
        
        // Verify cart item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new CustomException("Bạn không có quyền cập nhật mục này.");
        }
        
        // Check stock if variant exists
        if (cartItem.getVariant() != null) {
            if (cartItem.getVariant().getStockQuantity() < request.getQuantity()) {
                throw new CustomException("Số lượng sản phẩm không đủ. Còn lại: " + cartItem.getVariant().getStockQuantity());
            }
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(User user, UUID cartItemId) {
        Cart cart = getOrCreateCart(user);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException("Mục giỏ hàng không tồn tại."));
        
        // Verify cart item belongs to user's cart
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new CustomException("Bạn không có quyền xóa mục này.");
        }
        
        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        cartRepository.save(cart);
        
        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartItemRepository.deleteByCartId(cart.getId());
        cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCartItemCount(User user) {
        // Optimized: Use direct count query instead of loading full cart with items
        Integer count = cartItemRepository.countTotalItemsByUserId(user.getId());
        return count != null ? count : 0;
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse mapItemToResponse(CartItem item) {
        // Get product image (thumbnail or first image)
        String imageUrl = "/images/placeholder.png";
        List<ProductImage> images = productImageRepository.findByProductOrderByDisplayOrderAsc(item.getProduct());
        if (images != null && !images.isEmpty()) {
            Optional<ProductImage> thumbnail = images.stream()
                    .filter(ProductImage::getIsThumbnail)
                    .findFirst();
            if (thumbnail.isPresent()) {
                imageUrl = thumbnail.get().getImageUrl();
            } else {
                imageUrl = images.get(0).getImageUrl();
            }
        }

        CartItemResponse.CartItemResponseBuilder builder = CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(imageUrl)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt());

        if (item.getVariant() != null) {
            builder.variantId(item.getVariant().getId())
                    .variantName(item.getVariant().getName())
                    .variantValue(item.getVariant().getValue());
        }

        return builder.build();
    }
}
