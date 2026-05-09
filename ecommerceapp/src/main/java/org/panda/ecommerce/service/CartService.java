package org.panda.ecommerce.service;

import org.panda.ecommerce.dto.request.AddToCartRequest;
import org.panda.ecommerce.dto.request.UpdateCartItemRequest;
import org.panda.ecommerce.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartResponse addItem(AddToCartRequest request);

    CartResponse updateItem(Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long cartItemId);

    void clearCart();
}
