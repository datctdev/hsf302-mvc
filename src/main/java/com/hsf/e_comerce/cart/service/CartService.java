package com.hsf.e_comerce.cart.service;

import com.hsf.e_comerce.cart.dto.request.AddToCartRequest;
import com.hsf.e_comerce.cart.dto.request.UpdateCartItemRequest;
import com.hsf.e_comerce.cart.dto.response.CartResponse;
import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface CartService {

    Optional<Cart> getCartWithItemsAndProducts(User user);

    CartResponse getCartByUser(User user);
    
    CartResponse addToCart(User user, AddToCartRequest request);
    
    CartResponse updateCartItem(User user, UpdateCartItemRequest request);
    
    CartResponse removeCartItem(User user, UUID cartItemId);
    
    CartResponse clearCart(User user);
    
    Integer getCartItemCount(User user);

}
