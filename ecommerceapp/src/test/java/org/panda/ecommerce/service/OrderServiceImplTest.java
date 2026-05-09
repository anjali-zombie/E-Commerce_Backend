package org.panda.ecommerce.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.panda.ecommerce.dto.request.PlaceOrderRequest;
import org.panda.ecommerce.dto.response.OrderResponse;
import org.panda.ecommerce.entity.*;
import org.panda.ecommerce.exception.BadRequestException;
import org.panda.ecommerce.exception.InsufficientStockException;
import org.panda.ecommerce.repository.*;
import org.panda.ecommerce.service.impl.OrderServiceImpl;
import org.panda.ecommerce.util.SecurityUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Test User").email("test@example.com").build();
        testProduct = Product.builder()
                .id(10L).name("Widget").price(new BigDecimal("29.99"))
                .stockQuantity(10).active(true).build();
        cartItem = CartItem.builder().id(1L).product(testProduct)
                .quantity(2).unitPrice(new BigDecimal("29.99")).build();
        testCart = Cart.builder().id(100L).user(testUser).items(new ArrayList<>(List.of(cartItem))).build();
        cartItem.setCart(testCart);
    }

    @Test
    void placeOrder_validCart_shouldCreateOrder() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethodId("pm_test_123");

        Order savedOrder = Order.builder().id(1L).user(testUser)
                .totalAmount(new BigDecimal("59.98")).items(new ArrayList<>())
                .shippingAddress("123 Main St").build();

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(paymentService.createPaymentIntent(any(), any(), any(), any())).thenReturn("pi_secret");
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(savedOrder));

            OrderResponse response = orderService.placeOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            verify(cartService).clearCart();
            verify(productRepository, times(1)).save(any(Product.class));
        }
    }

    @Test
    void placeOrder_emptyCart_shouldThrowBadRequest() {
        testCart.getItems().clear();
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethodId("pm_test_123");

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Test
    void placeOrder_insufficientStock_shouldThrow() {
        testProduct.setStockQuantity(1);
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethodId("pm_test_123");

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Test
    void placeOrder_stockDecrements() {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setShippingAddress("123 Main St");
        request.setPaymentMethodId("pm_test_123");

        Order savedOrder = Order.builder().id(1L).user(testUser)
                .totalAmount(new BigDecimal("59.98")).items(new ArrayList<>())
                .shippingAddress("123 Main St").build();

        try (MockedStatic<SecurityUtils> utils = mockStatic(SecurityUtils.class)) {
            utils.when(() -> SecurityUtils.getCurrentUserId(userRepository)).thenReturn(1L);
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(paymentService.createPaymentIntent(any(), any(), any(), any())).thenReturn("pi_secret");
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(savedOrder));

            orderService.placeOrder(request);

            // stockQuantity was 10, quantity ordered was 2, so should now be 8
            assertThat(testProduct.getStockQuantity()).isEqualTo(8);
        }
    }
}
