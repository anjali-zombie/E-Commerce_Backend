package org.panda.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import org.panda.ecommerce.dto.request.AddToCartRequest;
import org.panda.ecommerce.dto.request.UpdateCartItemRequest;
import org.panda.ecommerce.dto.response.CartItemResponse;
import org.panda.ecommerce.dto.response.CartResponse;
import org.panda.ecommerce.entity.Cart;
import org.panda.ecommerce.entity.CartItem;
import org.panda.ecommerce.entity.Product;
import org.panda.ecommerce.entity.User;
import org.panda.ecommerce.exception.InsufficientStockException;
import org.panda.ecommerce.exception.ResourceNotFoundException;
import org.panda.ecommerce.exception.UnauthorizedException;
import org.panda.ecommerce.repository.CartItemRepository;
import org.panda.ecommerce.repository.CartRepository;
import org.panda.ecommerce.repository.ProductRepository;
import org.panda.ecommerce.repository.UserRepository;
import org.panda.ecommerce.service.CartService;
import org.panda.ecommerce.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(AddToCartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        Product product = productRepository.findById(request.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), request.getQuantity());
        }

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity(), newQty);
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.getItems().add(newItem);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long cartItemId, UpdateCartItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to modify this cart item");
        }

        Product product = item.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity(), request.getQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long cartItemId) {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to remove this cart item");
        }

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart() {
        Long userId = SecurityUtils.getCurrentUserId(userRepository);
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .imageUrl(item.getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalAmount(total)
                .totalItems(itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .build();
    }
}
