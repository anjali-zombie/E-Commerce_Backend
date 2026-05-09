package org.panda.ecommerce.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.panda.ecommerce.dto.request.AddToCartRequest;
import org.panda.ecommerce.dto.response.CartResponse;
import org.panda.ecommerce.entity.*;
import org.panda.ecommerce.exception.InsufficientStockException;
import org.panda.ecommerce.exception.UnauthorizedException;
import org.panda.ecommerce.repository.CartItemRepository;
import org.panda.ecommerce.repository.CartRepository;
import org.panda.ecommerce.repository.ProductRepository;
import org.panda.ecommerce.repository.UserRepository;
import org.panda.ecommerce.service.impl.CartServiceImpl;
import org.panda.ecommerce.util.SecurityUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Test").email("test@example.com").build();
        testProduct = Product.builder()
                .id(10L).name("Widget").price(new BigDecimal("19.99"))
                .stockQuantity(20).active(true).build();
        testCart = Cart.builder().id(100L).user(testUser).items(new ArrayList<>()).build();
    }

    @Test
    void addItem_sufficientStock_shouldAddToCart() {
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);

            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(10L);
            request.setQuantity(2);

            when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartResponse response = cartService.addItem(request);

            assertThat(response).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Test
    void addItem_insufficientStock_shouldThrow() {
        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);

            testProduct.setStockQuantity(1);
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(10L);
            request.setQuantity(5);

            when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Widget");
        }
    }

    @Test
    void removeItem_differentUser_shouldThrowUnauthorized() {
        User otherUser = User.builder().id(2L).name("Other").build();
        Cart otherCart = Cart.builder().id(200L).user(otherUser).items(new ArrayList<>()).build();
        CartItem item = CartItem.builder().id(50L).cart(otherCart).product(testProduct).quantity(1)
                .unitPrice(testProduct.getPrice()).build();

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);
            when(cartItemRepository.findById(50L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> cartService.removeItem(50L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Test
    void addItem_existingItem_shouldIncreaseQuantity() {
        CartItem existingItem = CartItem.builder().id(1L).cart(testCart).product(testProduct)
                .quantity(2).unitPrice(testProduct.getPrice()).build();

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);

            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(10L);
            request.setQuantity(3);

            when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartItemRepository.findByCartIdAndProductId(100L, 10L)).thenReturn(Optional.of(existingItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            cartService.addItem(request);

            assertThat(existingItem.getQuantity()).isEqualTo(5);
        }
    }
}
